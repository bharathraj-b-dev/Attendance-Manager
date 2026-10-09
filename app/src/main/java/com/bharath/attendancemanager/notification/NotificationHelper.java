package com.bharath.attendancemanager.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bharath.attendancemanager.R;
import com.bharath.attendancemanager.calculator.AttendanceCalculator;
import com.bharath.attendancemanager.data.LocalStorage;
import com.bharath.attendancemanager.model.SubjectAttendance;
import com.bharath.attendancemanager.ui.MainActivity;

import java.util.List;
import java.util.Locale;

public class NotificationHelper {

    public static final String CHANNEL_ALERTS_ID   = "attendance_alerts_channel";
    public static final String CHANNEL_UPDATES_ID  = "attendance_updates_channel";
    public static final String CHANNEL_REMINDERS_ID = "class_reminders_channel";

    private static final int NOTIF_ID_IMPORT_SUCCESS = 1001;
    private static final int NOTIF_ID_ALERT_BASE     = 2000;

    public static void initChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel alertChannel = new NotificationChannel(
                    CHANNEL_ALERTS_ID,
                    "Attendance Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("Alerts when your attendance drops below target");
            manager.createNotificationChannel(alertChannel);

            NotificationChannel updateChannel = new NotificationChannel(
                    CHANNEL_UPDATES_ID,
                    "Attendance Sync & Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            updateChannel.setDescription("Notifications about successful attendance imports");
            manager.createNotificationChannel(updateChannel);

            NotificationChannel reminderChannel = new NotificationChannel(
                    CHANNEL_REMINDERS_ID,
                    "Class Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            reminderChannel.setDescription("Heads-up reminders 30 minutes before each class");
            manager.createNotificationChannel(reminderChannel);
        }
    }

    public static void showImportSuccess(Context context, int subjectsCount) {
        if (!LocalStorage.getInstance(context).areNotificationsEnabled()) return;

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_UPDATES_ID)
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentTitle("Attendance Synced Successfully")
                .setContentText(String.format(Locale.US, "Imported %d subjects from DigiCampus.", subjectsCount))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notifySafely(context, NOTIF_ID_IMPORT_SUCCESS, builder);
    }

    public static void checkAllSubjectsAndNotify(Context context, List<SubjectAttendance> subjects, double target) {
        if (!LocalStorage.getInstance(context).areNotificationsEnabled() || subjects == null) return;

        int alertIndex = 0;
        for (SubjectAttendance subject : subjects) {
            double pct = subject.getPercentage();
            if (pct < target) {
                int needed = AttendanceCalculator.calculateClassesNeededToReachTarget(
                        subject.getPresentCount(),
                        subject.getAbsentCount(),
                        target
                );

                String title = "⚠️ Attendance Warning: " + subject.getSubjectName();
                String body = String.format(Locale.US, "Current: %.2f%% (Target: %.0f%%). Attend %d consecutive classes to recover.",
                        pct, target, needed);

                Intent intent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context,
                        alertIndex,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
                );

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
                        .setSmallIcon(R.drawable.ic_alert_circle)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

                notifySafely(context, NOTIF_ID_ALERT_BASE + alertIndex, builder);
                alertIndex++;
                if (alertIndex >= 3) break; // Limit simultaneous alert notifications
            }
        }
    }

    private static void notifySafely(Context context, int id, NotificationCompat.Builder builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static boolean isSystemNotificationEnabled(Context context) {
        boolean areEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();
        return areEnabled && hasNotificationPermission(context);
    }

    public static void openNotificationSettings(Context context) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
