package com.bharath.attendancemanager.model;

import com.bharath.attendancemanager.R;

public enum AttendanceStatus {
    PRESENT("Present", R.color.status_safe_text, R.drawable.bg_badge_safe),
    ABSENT("Absent", R.color.status_critical_text, R.drawable.bg_badge_critical),
    ON_DUTY("On Duty", R.color.status_on_duty_text, R.drawable.bg_badge_on_duty),
    UNMARKED("Unmarked", R.color.status_unmarked_text, R.drawable.bg_badge_unmarked);

    private final String displayName;
    private final int textColorRes;
    private final int badgeBackgroundRes;

    AttendanceStatus(String displayName, int textColorRes, int badgeBackgroundRes) {
        this.displayName = displayName;
        this.textColorRes = textColorRes;
        this.badgeBackgroundRes = badgeBackgroundRes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTextColorRes() {
        return textColorRes;
    }

    public int getBadgeBackgroundRes() {
        return badgeBackgroundRes;
    }

    public static AttendanceStatus fromRawString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return UNMARKED;
        }
        String clean = raw.trim().toUpperCase();
        if (clean.contains("PRESENT") || clean.contains("ATTEND") || clean.equals("P")) {
            return PRESENT;
        } else if (clean.contains("ABSENT") || clean.contains("BUNK") || clean.equals("A")) {
            return ABSENT;
        } else if (clean.contains("ON DUTY") || clean.contains("ON-DUTY") || clean.contains("OD")) {
            return ON_DUTY;
        }
        return UNMARKED;
    }
}
