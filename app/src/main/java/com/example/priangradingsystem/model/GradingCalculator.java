package com.example.priangradingsystem.model;

@SuppressWarnings("unused")
public class GradingCalculator {

    // Weight Constants from Image 1
    public static final double EXAM_WEIGHT = 0.30;
    public static final double ATTENDANCE_WEIGHT = 0.30;
    public static final double BOOKS_HW_WEIGHT = 0.40;

    /**
     * Calculates Value Rate for Exams (30%)
     */
    public static double calculateExamRate(double rawScore, double totalScore) {
        if (totalScore <= 0) return 0;
        return (rawScore / totalScore) * 100.0;
    }

    /**
     * Calculates Value Rate for Attendance (30%)
     */
    public static double calculateAttendanceRate(double daysAttended, double totalDays) {
        if (totalDays <= 0) return 0;
        return (daysAttended / totalDays) * 100.0;
    }

    /**
     * Calculates Value Rate (Round off) for Books & Homework (40%)
     */
    public static double calculateBooksHwRate(double earnedStars, double totalStars) {
        if (totalStars <= 0) return 0;
        double rate = (earnedStars / totalStars) * 100.0;
        return Math.round(rate);
    }

    /**
     * Calculates Initial Grade:
     * Initial Grade = (Exam Rate * 0.30) + (Attendance Rate * 0.30) + (Books/HW Rate * 0.40)
     */
    public static double calculateInitialGrade(double examRate, double attendanceRate, double booksHwRate) {
        return (examRate * EXAM_WEIGHT) + (attendanceRate * ATTENDANCE_WEIGHT) + (booksHwRate * BOOKS_HW_WEIGHT);
    }

    /**
     * Transmutation Table Lookup from Image 2
     */
    public static int transmuteGrade(double initialGrade) {
        if (initialGrade >= 99.50) return 100;
        if (initialGrade >= 98.32) return 99;
        if (initialGrade >= 97.14) return 98;
        if (initialGrade >= 95.96) return 97;
        if (initialGrade >= 94.78) return 96;
        if (initialGrade >= 93.60) return 95;
        if (initialGrade >= 92.42) return 94;
        if (initialGrade >= 91.24) return 93;
        if (initialGrade >= 90.06) return 92;
        if (initialGrade >= 88.88) return 91;
        if (initialGrade >= 87.70) return 90;
        if (initialGrade >= 86.52) return 89;
        if (initialGrade >= 85.34) return 88;
        if (initialGrade >= 84.16) return 87;
        if (initialGrade >= 82.98) return 86;
        if (initialGrade >= 81.80) return 85;
        if (initialGrade >= 80.62) return 84;
        if (initialGrade >= 79.44) return 83;
        if (initialGrade >= 78.26) return 82;
        if (initialGrade >= 77.08) return 81;
        if (initialGrade >= 75.90) return 80;
        return (int) Math.max(70, Math.round(initialGrade));
    }
}
