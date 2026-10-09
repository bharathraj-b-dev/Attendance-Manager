package com.bharath.attendancemanager.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import com.bharath.attendancemanager.data.LocalStorage;
import com.bharath.attendancemanager.model.AttendanceStatus;

public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String ACTION_ATTEND = "com.bharath.attendancemanager.ACTION_ATTEND";
    public static final String ACTION_BUNK   = "com.bharath.attendancemanager.ACTION_BUNK";
    public static final String ACTION_OD     = "com.bharath.attendancemanager.ACTION_OD";

    public static final String EXTRA_SUBJECT_NAME   = "extra_subject_name";
    public static final String EXTRA_COMPONENT_NAME = "extra_component_name";
    public static final String EXTRA_DATE           = "extra_date";
    public static final String EXTRA_TIME           = "extra_time";
    public static final String EXTRA_NOTIF_ID       = "extra_notif_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        String subjectName   = intent.getStringExtra(EXTRA_SUBJECT_NAME);
        String componentName = intent.getStringExtra(EXTRA_COMPONENT_NAME);
        String date           = intent.getStringExtra(EXTRA_DATE);
        String time           = intent.getStringExtra(EXTRA_TIME);
        int notifId           = intent.getIntExtra(EXTRA_NOTIF_ID, -1);

        AttendanceStatus newStatus = null;
        if (ACTION_ATTEND.equals(action)) {
            newStatus = AttendanceStatus.PRESENT;
        } else if (ACTION_BUNK.equals(action)) {
            newStatus = AttendanceStatus.ABSENT;
        } else if (ACTION_OD.equals(action)) {
            newStatus = AttendanceStatus.ON_DUTY;
        }

        if (newStatus != null && subjectName != null) {
            LocalStorage.getInstance(context).markSessionLocally(subjectName, componentName != null ? componentName : "", date, time, newStatus);
            if (notifId != -1) {
                NotificationManagerCompat.from(context).cancel(notifId);
            }
        }
    }
}