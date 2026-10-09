package com.bharath.attendancemanager.calculator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AttendanceCalculatorTest {

    @Test
    public void testPercentageCalculation() {
        double pct1 = AttendanceCalculator.calculatePercentage(49, 11);
        assertEquals(81.6666, pct1, 0.01);

        double pct2 = AttendanceCalculator.calculatePercentage(43, 17);
        assertEquals(71.6666, pct2, 0.01);

        double pctZero = AttendanceCalculator.calculatePercentage(0, 0);
        assertEquals(0.0, pctZero, 0.001);
    }

    @Test
    public void testBunkableClassesMath() {
        // 49 present, 11 absent (total 60). Target 75%
        // Max bunks = floor((100 * 49 - 75 * 60) / 75) = floor((4900 - 4500) / 75) = floor(5.333) = 5
        int bunks = AttendanceCalculator.calculateBunkableClasses(49, 11, 75.0);
        assertEquals(5, bunks);

        // Verification: missing 5 classes gives 49 / 65 = 75.38% (>= 75%)
        double after5Misses = AttendanceCalculator.calculatePercentage(49, 11 + 5);
        assertTrue(after5Misses >= 75.0);

        // Missing 6 classes drops below 75%: 49 / 66 = 74.24% (< 75%)
        double after6Misses = AttendanceCalculator.calculatePercentage(49, 11 + 6);
        assertTrue(after6Misses < 75.0);

        // If attendance is already below target (43 present, 17 absent = 71.67%)
        int bunksBelow = AttendanceCalculator.calculateBunkableClasses(43, 17, 75.0);
        assertEquals(0, bunksBelow);
    }

    @Test
    public void testClassesNeededToReachTargetMath() {
        // 43 present, 17 absent (total 60). Target 75%
        // Needed = ceil((75 * 60 - 100 * 43) / (100 - 75)) = ceil((4500 - 4300) / 25) = ceil(200 / 25) = 8
        int needed = AttendanceCalculator.calculateClassesNeededToReachTarget(43, 17, 75.0);
        assertEquals(8, needed);

        // Verification: attending 8 consecutive classes gives (43 + 8) / (60 + 8) = 51 / 68 = 75.00%
        double after8Attended = AttendanceCalculator.calculatePercentage(43 + 8, 17);
        assertEquals(75.0, after8Attended, 0.001);

        // Attending 7 classes is not enough: (43 + 7) / (60 + 7) = 50 / 67 = 74.63%
        double after7Attended = AttendanceCalculator.calculatePercentage(43 + 7, 17);
        assertTrue(after7Attended < 75.0);

        // If attendance is already above target (49 present, 11 absent = 81.67%)
        int neededAbove = AttendanceCalculator.calculateClassesNeededToReachTarget(49, 11, 75.0);
        assertEquals(0, neededAbove);
    }

    @Test
    public void testSimulateScenarios() {
        // 43 present, 17 absent = 71.67%
        // If attend 1 next class: 44 / 61 = 72.13%
        double attend1 = AttendanceCalculator.simulateScenario(43, 17, 1, true);
        assertEquals(72.1311, attend1, 0.01);

        // If miss 1 next class: 43 / 61 = 70.49%
        double miss1 = AttendanceCalculator.simulateScenario(43, 17, 1, false);
        assertEquals(70.4918, miss1, 0.01);
    }

    @Test
    public void testStatusCategories() {
        assertEquals(AttendanceCalculator.StatusCategory.SAFE,
                AttendanceCalculator.getStatusCategory(75.0, 75.0));
        assertEquals(AttendanceCalculator.StatusCategory.SAFE,
                AttendanceCalculator.getStatusCategory(81.67, 75.0));

        assertEquals(AttendanceCalculator.StatusCategory.WARNING,
                AttendanceCalculator.getStatusCategory(71.67, 75.0));
        assertEquals(AttendanceCalculator.StatusCategory.WARNING,
                AttendanceCalculator.getStatusCategory(70.0, 75.0));

        assertEquals(AttendanceCalculator.StatusCategory.CRITICAL,
                AttendanceCalculator.getStatusCategory(69.99, 75.0));
        assertEquals(AttendanceCalculator.StatusCategory.CRITICAL,
                AttendanceCalculator.getStatusCategory(50.0, 75.0));
    }
}
