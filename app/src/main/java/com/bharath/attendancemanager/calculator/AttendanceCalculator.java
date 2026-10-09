package com.bharath.attendancemanager.calculator;

import com.bharath.attendancemanager.R;
import com.bharath.attendancemanager.model.SubjectAttendance;

import java.util.Locale;

public class AttendanceCalculator {

    public static final double DEFAULT_TARGET_PERCENTAGE = 75.0;

    public enum StatusCategory {
        SAFE("Safe", R.color.status_safe_text, R.color.status_safe_bg, R.drawable.bg_badge_safe),
        WARNING("Warning", R.color.status_warning_text, R.color.status_warning_bg, R.drawable.bg_badge_warning),
        CRITICAL("Critical", R.color.status_critical_text, R.color.status_critical_bg, R.drawable.bg_badge_critical);

        private final String label;
        private final int textColorRes;
        private final int bgColorRes;
        private final int badgeDrawableRes;

        StatusCategory(String label, int textColorRes, int bgColorRes, int badgeDrawableRes) {
            this.label = label;
            this.textColorRes = textColorRes;
            this.bgColorRes = bgColorRes;
            this.badgeDrawableRes = badgeDrawableRes;
        }

        public String getLabel() {
            return label;
        }

        public int getTextColorRes() {
            return textColorRes;
        }

        public int getBgColorRes() {
            return bgColorRes;
        }

        public int getBadgeDrawableRes() {
            return badgeDrawableRes;
        }
    }

    /**
     * Calculates the attendance percentage: (present / (present + absent)) * 100.
     * Note: Unmarked sessions are never included in the percentage.
     */
    public static double calculatePercentage(int present, int absent) {
        int total = present + absent;
        if (total <= 0) return 0.0;
        return ((double) present / total) * 100.0;
    }

    /**
     * Determines status category based on target.
     * Safe: >= target
     * Warning: between [target - 5.0, target)
     * Critical: < target - 5.0
     */
    public static StatusCategory getStatusCategory(double percentage, double targetPercentage) {
        if (percentage >= targetPercentage) {
            return StatusCategory.SAFE;
        } else if (percentage >= (targetPercentage - 5.0)) {
            return StatusCategory.WARNING;
        } else {
            return StatusCategory.CRITICAL;
        }
    }

    /**
     * Calculates how many future classes the student can miss while remaining at or above target%.
     * Returns 0 if current attendance is already below target.
     */
    public static int calculateBunkableClasses(int present, int absent, double targetPercentage) {
        int total = present + absent;
        if (total <= 0 || targetPercentage <= 0.0 || targetPercentage >= 100.0) {
            return 0;
        }

        double currentPct = ((double) present / total) * 100.0;
        if (currentPct < targetPercentage) {
            return 0;
        }

        // present / (total + b) >= target / 100
        // 100 * present >= target * (total + b)
        // b <= (100 * present - target * total) / target
        double numerator = (100.0 * present) - (targetPercentage * total);
        double maxBunks = Math.floor(numerator / targetPercentage);
        return Math.max(0, (int) maxBunks);
    }

    /**
     * Calculates how many consecutive future classes the student must attend to reach target%.
     * Returns 0 if current attendance is already at or above target.
     */
    public static int calculateClassesNeededToReachTarget(int present, int absent, double targetPercentage) {
        int total = present + absent;
        if (targetPercentage <= 0.0 || targetPercentage >= 100.0) {
            return 0;
        }

        double currentPct = (total > 0) ? (((double) present / total) * 100.0) : 0.0;
        if (currentPct >= targetPercentage) {
            return 0;
        }

        // (present + a) / (total + a) >= target / 100
        // (100 - target) * a >= target * total - 100 * present
        // a >= (target * total - 100 * present) / (100 - target)
        double numerator = (targetPercentage * total) - (100.0 * present);
        double denominator = 100.0 - targetPercentage;
        double needed = Math.ceil(numerator / denominator);
        return Math.max(0, (int) needed);
    }

    /**
     * Returns a human-friendly advice string for a given subject.
     */
    public static String getBunkAdvice(int present, int absent, double targetPercentage) {
        int total = present + absent;
        if (total <= 0) {
            return "No classes recorded yet.";
        }

        double currentPct = ((double) present / total) * 100.0;
        if (currentPct >= targetPercentage) {
            int bunks = calculateBunkableClasses(present, absent, targetPercentage);
            if (bunks == 0) {
                return String.format(Locale.US, "You are on the edge of %.0f%%. Do not miss the next class.", targetPercentage);
            } else if (bunks == 1) {
                return String.format(Locale.US, "You can safely miss 1 class and stay above %.0f%%.", targetPercentage);
            } else {
                return String.format(Locale.US, "You can safely miss %d classes and stay above %.0f%%.", bunks, targetPercentage);
            }
        } else {
            int needed = calculateClassesNeededToReachTarget(present, absent, targetPercentage);
            if (needed == 1) {
                return String.format(Locale.US, "Need 1 more class to reach %.0f%%.", targetPercentage);
            } else {
                return String.format(Locale.US, "Need %d more consecutive classes to reach %.0f%%.", needed, targetPercentage);
            }
        }
    }

    /**
     * Simulates what-if scenario:
     * If user attends N classes: present increases by N, total increases by N.
     * If user misses N classes: present unchanged, total increases by N.
     */
    public static double simulateScenario(int currentPresent, int currentAbsent, int count, boolean isAttend) {
        int newPresent = isAttend ? (currentPresent + count) : currentPresent;
        int newAbsent = isAttend ? currentAbsent : (currentAbsent + count);
        return calculatePercentage(newPresent, newAbsent);
    }

    public static String getShortBunkSummary(SubjectAttendance subject, double targetPercentage) {
        int p = subject.getPresentCount();
        int a = subject.getAbsentCount();
        int total = p + a;
        if (total <= 0) return "No data";

        double pct = subject.getPercentage();
        if (pct >= targetPercentage) {
            int bunks = calculateBunkableClasses(p, a, targetPercentage);
            return bunks > 0 ? ("Can miss " + bunks) : "On edge";
        } else {
            int needed = calculateClassesNeededToReachTarget(p, a, targetPercentage);
            return "Need " + needed;
        }
    }

    /**
     * Calculates dynamic guidance for What-If simulation state.
     */
    public static String getWhatIfGuidance(int simPresent, int simAbsent, double targetPercentage) {
        int total = simPresent + simAbsent;
        if (total <= 0) {
            return "No classes in simulation.";
        }
        double simPct = ((double) simPresent / total) * 100.0;
        if (simPct >= targetPercentage) {
            int maxBunks = calculateBunkableClasses(simPresent, simAbsent, targetPercentage);
            if (maxBunks == 0) {
                return String.format(Locale.US, "🎉 You reach your %.0f%% target! You are right on the edge. Do not miss the next class.", targetPercentage);
            } else if (maxBunks == 1) {
                return String.format(Locale.US, "🎉 You reach your %.0f%% target! You can safely miss 1 additional class.", targetPercentage);
            } else {
                return String.format(Locale.US, "🎉 You reach your %.0f%% target! You can safely miss %d additional classes.", targetPercentage, maxBunks);
            }
        } else {
            int needed = calculateClassesNeededToReachTarget(simPresent, simAbsent, targetPercentage);
            if (needed == 1) {
                return String.format(Locale.US, "You need 1 more consecutive class to reach %.0f%%.", targetPercentage);
            } else {
                return String.format(Locale.US, "You need %d more consecutive classes to reach %.0f%%.", needed, targetPercentage);
            }
        }
    }
}
