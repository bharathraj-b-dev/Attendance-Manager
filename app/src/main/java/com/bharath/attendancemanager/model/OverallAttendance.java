package com.bharath.attendancemanager.model;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

public class OverallAttendance implements Serializable {
    private final int totalPresent;
    private final int totalAbsent;
    private final int totalMarked;
    private final int totalUpcoming;
    private final double overallPercentage;
    private final int safeSubjectsCount;
    private final int warningSubjectsCount;
    private final int criticalSubjectsCount;

    public OverallAttendance(int totalPresent, int totalAbsent, int totalMarked, int totalUpcoming,
                             double overallPercentage, int safeSubjectsCount, int warningSubjectsCount, int criticalSubjectsCount) {
        this.totalPresent = totalPresent;
        this.totalAbsent = totalAbsent;
        this.totalMarked = totalMarked;
        this.totalUpcoming = totalUpcoming;
        this.overallPercentage = overallPercentage;
        this.safeSubjectsCount = safeSubjectsCount;
        this.warningSubjectsCount = warningSubjectsCount;
        this.criticalSubjectsCount = criticalSubjectsCount;
    }

    public static OverallAttendance calculate(List<SubjectAttendance> subjects, double targetPercentage) {
        if (subjects == null || subjects.isEmpty()) {
            return new OverallAttendance(0, 0, 0, 0, 0.0, 0, 0, 0);
        }

        int pres = 0;
        int abs = 0;
        int upcoming = 0;
        int safeCount = 0;
        int warnCount = 0;
        int critCount = 0;

        for (SubjectAttendance s : subjects) {
            pres += s.getPresentCount();
            abs += s.getAbsentCount();
            upcoming += s.getUpcomingCount();

            double p = s.getPercentage();
            if (p >= targetPercentage) {
                safeCount++;
            } else if (p >= (targetPercentage - 5.0)) {
                warnCount++;
            } else {
                critCount++;
            }
        }

        int total = pres + abs;
        double overallPct = (total > 0) ? ((double) pres / total) * 100.0 : 0.0;

        return new OverallAttendance(pres, abs, total, upcoming, overallPct, safeCount, warnCount, critCount);
    }

    public int getTotalPresent() {
        return totalPresent;
    }

    public int getTotalAbsent() {
        return totalAbsent;
    }

    public int getTotalMarked() {
        return totalMarked;
    }

    public int getTotalUpcoming() {
        return totalUpcoming;
    }

    public double getOverallPercentage() {
        return overallPercentage;
    }

    public String getFormattedPercentage() {
        return String.format(Locale.US, "%.2f%%", overallPercentage);
    }

    public int getSafeSubjectsCount() {
        return safeSubjectsCount;
    }

    public int getWarningSubjectsCount() {
        return warningSubjectsCount;
    }

    public int getCriticalSubjectsCount() {
        return criticalSubjectsCount;
    }
}
