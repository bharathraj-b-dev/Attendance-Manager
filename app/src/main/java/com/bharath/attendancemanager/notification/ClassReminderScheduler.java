package com.bharath.attendancemanager.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.bharath.attendancemanager.model.AttendanceSession;
import com.bharath.attendancemanager.model.SubjectAttendance;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Schedules and cancels AlarmManager-based class reminder notifications.
 *
 * Call scheduleAllReminders() after every DigiCampus import.
 * Each alarm fires 30 minutes before the class start time.
 * ClassReminderReceiver handles the alarm and shows the notification.
 */
public class ClassReminderScheduler {

    private static final String TAG = "ClassReminderScheduler";
    private static final int REMINDER_MINUTES_BEFORE = 30;
    private static final int BASE_REQUEST_CODE = 5000;
    private static final int MAX_ALARMS = 50;

    private static final String[] DATE_FORMATS = {
            "dd-MM-yyyy HH:mm",
            "dd-MM-yyyy hh:mm a",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy hh:mm a",
            "yyyy-MM-dd HH:mm",
            "dd-MMM-yyyy HH:mm",
            "dd MMM yyyy HH:mm"
    };

    public static void scheduleAllReminders(Context context, List<SubjectAttendance> subjects) {
        if (subjects == null || subjects.isEmpty()) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        cancelAllReminders(context, alarmManager);

        if (!isExactAlarmAllowed(context, alarmManager)) {
            Log.w(TAG, "Exact alarm permission not granted. Reminders will not fire.");
            return;
        }

        int alarmIndex = 0;
        long now = System.currentTimeMillis();

        for (SubjectAttendance subject : subjects) {
            if (alarmIndex >= MAX_ALARMS) break;
            List<AttendanceSession> upcoming = subject.getUpcomingSessions();
            if (upcoming == null) continue;

            for (AttendanceSession session : upcoming) {
                if (alarmIndex >= MAX_ALARMS) break;

                long triggerTime = parseTriggerTime(session.getDate(), session.getTime());
                if (triggerTime <= 0) continue;

                long reminderTime = triggerTime - (REMINDER_MINUTES_BEFORE * 60 * 1000L);
                if (reminderTime <= now) continue;

                scheduleAlarm(context, alarmManager, alarmIndex,
                        session.getSubjectName().isEmpty() ? subject.getSubjectName() : session.getSubjectName(),
                        session.getComponentName(), session.getDate(), session.getTime(), reminderTime);
                alarmIndex++;
            }
        }

        Log.i(TAG, "Scheduled " + alarmIndex + " class reminder alarm(s).");
    }

    public static void cancelAllReminders(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) cancelAllReminders(context, alarmManager);
    }

    private static void scheduleAlarm(Context context, AlarmManager alarmManager,
                                      int index, String subjectName, String componentName,
                                      String date, String classTime, long triggerAtMillis) {
        Intent intent = new Intent(context, ClassReminderReceiver.class);
        intent.setAction("com.bharath.attendancemanager.CLASS_REMINDER_" + index);
        intent.putExtra(ClassReminderReceiver.EXTRA_SUBJECT_NAME, subjectName);
        intent.putExtra(ClassReminderReceiver.EXTRA_COMPONENT_NAME, componentName);
        intent.putExtra(ClassReminderReceiver.EXTRA_CLASS_DATE, date);
        intent.putExtra(ClassReminderReceiver.EXTRA_CLASS_TIME, classTime);
        intent.putExtra(ClassReminderReceiver.EXTRA_NOTIF_ID, BASE_REQUEST_CODE + index);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT |
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        PendingIntent pi = PendingIntent.getBroadcast(context, BASE_REQUEST_CODE + index, intent, flags);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            }
            Log.d(TAG, "Alarm set for [" + subjectName + "] at " + new Date(triggerAtMillis));
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException scheduling alarm: " + e.getMessage());
        }
    }

    private static void cancelAllReminders(Context context, AlarmManager alarmManager) {
        for (int i = 0; i < MAX_ALARMS; i++) {
            Intent intent = new Intent(context, ClassReminderReceiver.class);
            intent.setAction("com.bharath.attendancemanager.CLASS_REMINDER_" + i);
            int flags = PendingIntent.FLAG_NO_CREATE |
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent pi = PendingIntent.getBroadcast(context, BASE_REQUEST_CODE + i, intent, flags);
            if (pi != null) alarmManager.cancel(pi);
        }
    }

    private static long parseTriggerTime(String date, String time) {
        if (date == null || date.isEmpty()) return 0;
        String combined = (time != null && !time.isEmpty()) ? (date.trim() + " " + time.trim()) : date.trim();

        for (String fmt : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.US);
                sdf.setLenient(false);
                Date parsed = sdf.parse(combined);
                if (parsed != null) return parsed.getTime();
            } catch (ParseException ignored) {}
        }

        String[] dateOnlyFormats = {"dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "dd-MMM-yyyy"};
        for (String fmt : dateOnlyFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.US);
                sdf.setLenient(false);
                Date parsed = sdf.parse(date.trim());
                if (parsed != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(parsed);
                    cal.set(Calendar.HOUR_OF_DAY, 8);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    return cal.getTimeInMillis();
                }
            } catch (ParseException ignored) {}
        }
        return 0;
    }

    private static boolean isExactAlarmAllowed(Context context, AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }
}