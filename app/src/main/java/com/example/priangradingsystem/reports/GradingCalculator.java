package com.example.priangradingsystem.reports;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Grade calculation rules shared by the reports screen. Pure Java (no Android classes). */
public final class GradingCalculator {
    public static final double EXAM_WEIGHT = 0.30;
    public static final double ATTENDANCE_WEIGHT = 0.30;
    public static final double BOOKS_HW_WEIGHT = 0.40;
    public static final int MAX_STARS_PER_RATING = 3;
    /** Lowest initial grade covered by the official transmutation table. */
    public static final double LOWEST_MAPPED_INITIAL = 75.90;

    private static final Pattern FIRST_NUMBER = Pattern.compile("^\\s*(-?\\d+(?:\\.\\d+)?)");

    private GradingCalculator() {}

    // ------------------------------------------------------------------ Exam (30%)

    /**
     * Official exam mapping: raw score 1-20 to equivalent grade.
     * 1=80 ... 10=89, 11=91 ... 20=100. The grade 90 is intentionally skipped.
     * Returns -1 for anything outside 1..20.
     */
    public static int examEquivalentGrade(int rawScore) {
        switch (rawScore) {
            case 1:  return 80;
            case 2:  return 81;
            case 3:  return 82;
            case 4:  return 83;
            case 5:  return 84;
            case 6:  return 85;
            case 7:  return 86;
            case 8:  return 87;
            case 9:  return 88;
            case 10: return 89;
            case 11: return 91;   // 90 is skipped on purpose
            case 12: return 92;
            case 13: return 93;
            case 14: return 94;
            case 15: return 95;
            case 16: return 96;
            case 17: return 97;
            case 18: return 98;
            case 19: return 99;
            case 20: return 100;
            default: return -1;
        }
    }

    /** Parses "16/20" or "16" and returns the raw score (1..20), or -1 if invalid. */
    public static int parseExamScore(String raw) {
        double n = leadingNumber(raw);
        if (Double.isNaN(n) || n != Math.rint(n)) return -1;
        int v = (int) n;
        return (v >= 1 && v <= 20) ? v : -1;
    }

    // ------------------------------------------------------------------ Attendance (30%)

    /** Present / (Present + Absent) x 100. {@code totalSchoolDays} must include absent days. */
    public static double calculateAttendanceRate(int present, int totalSchoolDays) {
        if (totalSchoolDays <= 0 || present < 0 || present > totalSchoolDays) return -1;
        return present * 100.0 / totalSchoolDays;
    }

    /** true = present, false = absent, null = not a recognised attendance status. */
    public static Boolean parseAttendanceStatus(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.equals("present") || value.equals("p") || value.equals("present day")) return Boolean.TRUE;
        if (value.equals("absent") || value.equals("a") || value.equals("absent day")) return Boolean.FALSE;
        return null;
    }

    // ------------------------------------------------------------------ Books + Homework (40%)

    /** Stars earned / (ratings x 3) x 100. */
    public static double calculateBooksHwRate(int earnedStars, int possibleStars) {
        if (possibleStars <= 0 || earnedStars < 0 || earnedStars > possibleStars) return -1;
        return earnedStars * 100.0 / possibleStars;
    }

    /** Parses the leading star count of "3 ⭐ (3 Stars)"; valid range 0..3, else -1. */
    public static int parseStarRating(String raw) {
        double n = leadingNumber(raw);
        if (Double.isNaN(n) || n != Math.rint(n) || n < 0 || n > MAX_STARS_PER_RATING) return -1;
        return (int) n;
    }

    // ------------------------------------------------------------------ Program Performance

    /** Parses "2 Points" / "2" into bonus points (>= 0). Returns -1 when unreadable. */
    public static double parseProgramPoints(String raw) {
        double n = leadingNumber(raw);
        if (Double.isNaN(n) || n < 0) return -1;
        return n;
    }

    // ------------------------------------------------------------------ Initial grade

    /**
     * Weighted initial grade before bonus. {@code examGrade} is the mapped equivalent grade (80-100),
     * the other two are percentages. Returns -1 if any component is invalid.
     */
    public static double calculateInitialGrade(double examGrade, double attendanceRate, double booksHwRate) {
        if (!validPercent(examGrade) || !validPercent(attendanceRate) || !validPercent(booksHwRate)) return -1;
        return examGrade * EXAM_WEIGHT + attendanceRate * ATTENDANCE_WEIGHT + booksHwRate * BOOKS_HW_WEIGHT;
    }

    private static boolean validPercent(double value) {
        return Double.isFinite(value) && value >= 0 && value <= 100;
    }

    // ------------------------------------------------------------------ Transmutation

    /** Rounds to 2 decimals (half up) so values line up with the 2-decimal ranges of the table. */
    public static double roundTo2(double v) {
        return Math.floor(v * 100.0 + 0.5 + 1e-9) / 100.0;
    }

    /**
     * Official table. Maps initial grade to transmuted grade.
     * Values above 100 (possible once Program Performance points are added) are treated as 100.
     * Initial grades below 75.90 return the rounded initial grade.
     */
    public static int transmuteGrade(double initialGrade) {
        if (!Double.isFinite(initialGrade) || initialGrade < 0) return -1;
        double g = roundTo2(initialGrade);
        if (g >= 99.50) return 100;
        if (g >= 98.32) return 99;
        if (g >= 97.14) return 98;
        if (g >= 95.96) return 97;
        if (g >= 94.78) return 96;
        if (g >= 93.60) return 95;
        if (g >= 92.42) return 94;
        if (g >= 91.24) return 93;
        if (g >= 90.06) return 92;
        if (g >= 88.88) return 91;
        if (g >= 87.70) return 90;
        if (g >= 86.52) return 89;
        if (g >= 85.34) return 88;
        if (g >= 84.16) return 87;
        if (g >= 82.98) return 86;
        if (g >= 81.80) return 85;
        if (g >= 80.62) return 84;
        if (g >= 79.44) return 83;
        if (g >= 78.26) return 82;
        if (g >= 77.08) return 81;
        if (g >= 75.90) return 80;
        return (int) Math.round(g);
    }

    // ------------------------------------------------------------------ Subject names

    /** Maps stored subject text to one of the five canonical names, or null. */
    public static String canonicalSubject(String raw) {
        String k = subjectKey(raw);
        switch (k) {
            case "writing": return "Writing";
            case "math":
            case "mathematics": return "Math";
            case "reading": return "Reading";
            case "lifeskills":
            case "lifeskill": return "Life Skills";
            case "peandarts":
            case "peandart":
            case "pearts":
            case "peart": return "PE & Arts";
            default: return null;
        }
    }

    /**
     * Subjects a Program Performance entry applies to, based on the subject saved with it:
     * blank / N/A / All Subjects -> all five; a combined entry such as "Life Skills & PE & Arts" -> each
     * subject named in it; a single subject -> that subject; anything else -> empty (ignored).
     */
    public static java.util.List<String> programSubjects(String raw) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (isAllSubjects(raw)) { out.addAll(java.util.Arrays.asList("Writing", "Math", "Reading", "Life Skills", "PE & Arts")); return out; }
        String single = canonicalSubject(raw);
        if (single != null) { out.add(single); return out; }
        String k = subjectKey(raw);
        String[][] names = {{"Writing", "writing"}, {"Math", "math"}, {"Reading", "reading"},
                {"Life Skills", "lifeskills"}, {"PE & Arts", "peandarts"}};
        for (String[] n : names) {
            if (k.contains(n[1])) { out.add(n[0]); k = k.replace(n[1], "|"); }
        }
        return out;
    }

    /** true for blank / N/A / General / All Subjects. */
    public static boolean isAllSubjects(String raw) {
        String k = subjectKey(raw);
        return k.isEmpty() || k.equals("na") || k.equals("none") || k.equals("general")
                || k.equals("all") || k.equals("allsubjects");
    }

    private static String subjectKey(String raw) {
        if (raw == null) return "";
        return raw.toLowerCase(Locale.ROOT).replace("&", "and").replaceAll("[^a-z0-9]", "");
    }

    private static double leadingNumber(String raw) {
        if (raw == null) return Double.NaN;
        Matcher m = FIRST_NUMBER.matcher(raw);
        if (!m.find()) return Double.NaN;
        try { return Double.parseDouble(m.group(1)); }
        catch (NumberFormatException e) { return Double.NaN; }
    }
}