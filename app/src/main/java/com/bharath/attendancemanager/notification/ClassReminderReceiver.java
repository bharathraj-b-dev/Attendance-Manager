package com.bharath.attendancemanager.notification;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bharath.attendancemanager.R;
import com.bharath.attendancemanager.data.LocalStorage;
import com.bharath.attendancemanager.ui.MainActivity;

/**
 * Fired by AlarmManager when an upcoming class reminder is due.
 * Shows heads-up notification with action buttons [ ATTEND ] [ BUNK ] [ OD ].
 */
public class ClassReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_SUBJECT_NAME   = "subject_name";
    public static final String EXTRA_COMPONENT_NAME = "component_name";
    public static final String EXTRA_CLASS_DATE     = "class_date";
    public static final String EXTRA_CLASS_TIME     = "class_time";
    public static final String EXTRA_NOTIF_ID       = "notif_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!LocalStorage.getInstance(context).areNotificationsEnabled()) return;

        String subjectName   = intent.getStringExtra(EXTRA_SUBJECT_NAME);
        String componentName = intent.getStringExtra(EXTRA_COMPONENT_NAME);
        String date           = intent.getStringExtra(EXTRA_CLASS_DATE);
        String classTime     = intent.getStringExtra(EXTRA_CLASS_TIME);
        int    notifId       = intent.getIntExtra(EXTRA_NOTIF_ID, 3000);

        if (subjectName == null || subjectName.isEmpty()) return;
        if (componentName == null) componentName = "";
        if (date == null) date = "";
        if (classTime == null) classTime = "";

        String fullTitle = componentName.isEmpty() ? subjectName : subjectName + " (" + componentName + ")";
        String body = classTime.isEmpty() ? "Upcoming class starting soon. What happened?" : "Class starts at " + classTime + ". Mark what happened:";

        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Action Intention Builders
        PendingIntent attendPending = createActionPendingIntent(context, NotificationActionReceiver.ACTION_ATTEND, subjectName, componentName, date, classTime, notifId, 1);
        PendingIntent bunkPending = createActionPendingIntent(context, NotificationActionReceiver.ACTION_BUNK, subjectName, componentName, date, classTime, notifId, 2);
        PendingIntent odPending = createActionPendingIntent(context, NotificationActionReceiver.ACTION_OD, subjectName, componentName, date, classTime, notifId, 3);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDERS_ID)
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentTitle("Upcoming Class: " + fullTitle)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openPendingIntent)
                .addAction(0, "✓ ATTEND", attendPending)
                .addAction(0, "✕ BUNK", bunkPending)
                .addAction(0, "◉ OD", odPending);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    private PendingIntent createActionPendingIntent(Context context, String action, String subject, String component, String date, String time, int notifId, int requestCodeOffset) {
        Intent intent = new Intent(context, NotificationActionReceiver.class);
        intent.setAction(action);
        intent.putExtra(NotificationActionReceiver.EXTRA_SUBJECT_NAME, subject);
        intent.putExtra(NotificationActionReceiver.EXTRA_COMPONENT_NAME, component);
        intent.putExtra(NotificationActionReceiver.EXTRA_DATE, date);
        intent.putExtra(NotificationActionReceiver.EXTRA_TIME, time);
        intent.putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notifId);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getBroadcast(context, notifId * 10 + requestCodeOffset, intent, flags);
    }
}