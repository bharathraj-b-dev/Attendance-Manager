package com.bharath.attendancemanager.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bharath.attendancemanager.data.LocalStorage;
import com.bharath.attendancemanager.model.SubjectAttendance;

import java.util.List;

/**
 * Fires when the device boots (BOOT_COMPLETED).
 * AlarmManager alarms are wiped on reboot, so we reschedule all class reminders
 * by reading the saved subjects from LocalStorage.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }
        List<SubjectAttendance> subjects = LocalStorage.getInstance(context).getSubjects();
        if (subjects != null && !subjects.isEmpty()) {
            ClassReminderScheduler.scheduleAllReminders(context, subjects);
        }
    }
}