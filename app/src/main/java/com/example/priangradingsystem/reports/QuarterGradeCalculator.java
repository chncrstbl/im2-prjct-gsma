package com.example.priangradingsystem.reports;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Computes one student's quarterly grades from that student's records for ONE quarter.
 * Pure Java so it can be unit tested.
 */
public final class QuarterGradeCalculator {

    public static final List<String> SUBJECTS = java.util.Arrays.asList(
            "Writing", "Math", "Reading", "Life Skills", "PE & Arts");

    /** One saved grade_records row (only the fields the calculation needs). */
    public static final class GradeRow {
        public final long recordId;
        public final String subject, activity, activityName, score, date, time;
        public GradeRow(long recordId, String subject, String activity, String activityName, String score, String date) {
            this(recordId, subject, activity, activityName, score, date, "");
        }
        /** {@code time} is the HH:mm:ss the row was submitted (grade_records.time); may be blank on older rows. */
        public GradeRow(long recordId, String subject, String activity, String activityName, String score, String date, String time) {
            this.recordId = recordId;
            this.subject = subject == null ? "" : subject;
            this.activity = activity == null ? "" : activity;
            this.activityName = activityName == null ? "" : activityName;
            this.score = score == null ? "" : score;
            this.date = date == null ? "" : date;
            this.time = time == null ? "" : time;
        }
    }

    public enum Status { NO_DATA, INCOMPLETE, COMPLETE }

    /** Per-subject result. Values that could not be computed are -1. */
    public static final class SubjectResult {
        public final String subject;
        public final List<Integer> examRawScores = new ArrayList<>();
        public double examGrade = -1;            // mapped equivalent grade (average if several exams)
        public int starsEarned, starsPossible, ratingCount;
        /** Books ratings entered for this subject itself (shared Homework is not counted). */
        public int ownBooksCount;
        public double booksHwRate = -1;
        public double examContribution = -1, attendanceContribution = -1, booksHwContribution = -1;
        public double programPoints = 0;
        public double initialBeforeBonus = -1;
        public double adjustedInitial = -1;
        public int transmuted = -1;              // -1 = unmapped (below 75.90) or not computed
        public Status status = Status.NO_DATA;
        SubjectResult(String subject) { this.subject = subject; }
        public boolean hasExam() { return examGrade >= 0; }
        public boolean hasBooksHw() { return ratingCount > 0; }
    }

    public static final class Result {
        public int attendancePresent, attendanceAbsent, attendanceTotal;
        public double attendanceRate = -1;
        public final Map<String, SubjectResult> subjects = new LinkedHashMap<>();
        public int completeSubjects;
        public double overallInitial = -1;       // average of the five ADJUSTED initial grades
        public int overallTransmuted = -1;
        public boolean overallComplete;
        public final List<String> warnings = new ArrayList<>();

        /** Single-line-per-subject trace for Logcat. Contains no student-identifying data. */
        public String trace() {
            StringBuilder sb = new StringBuilder();
            sb.append("attendance present=").append(attendancePresent).append(" absent=").append(attendanceAbsent)
                    .append(" total=").append(attendanceTotal).append(" rate=").append(fmt(attendanceRate));
            for (SubjectResult s : subjects.values()) {
                sb.append("\n  ").append(s.subject)
                        .append(": exams=").append(s.examRawScores).append(" examGrade=").append(fmt(s.examGrade))
                        .append(" stars=").append(s.starsEarned).append('/').append(s.starsPossible)
                        .append(" (ratings=").append(s.ratingCount).append(") bhRate=").append(fmt(s.booksHwRate))
                        .append(" | exam30=").append(fmt(s.examContribution))
                        .append(" att30=").append(fmt(s.attendanceContribution))
                        .append(" bh40=").append(fmt(s.booksHwContribution))
                        .append(" bonus=").append(fmt(s.programPoints))
                        .append(" initial=").append(fmt(s.initialBeforeBonus))
                        .append(" adjusted=").append(fmt(s.adjustedInitial))
                        .append(" transmuted=").append(s.transmuted).append(" status=").append(s.status);
            }
            sb.append("\n  overall: complete=").append(completeSubjects).append("/5 avgInitial=").append(fmt(overallInitial))
                    .append(" transmuted=").append(overallTransmuted);
            for (String w : warnings) sb.append("\n  WARN: ").append(w);
            return sb.toString();
        }
        private static String fmt(double v) { return String.format(Locale.US, "%.4f", v); }
    }

    private QuarterGradeCalculator() {}

    public static Result calculate(List<GradeRow> records) {
        return calculate(records, -1);
    }

    public static Result calculate(List<GradeRow> records, int totalSchoolDaysOverride) {
        Result res = new Result();
        for (String s : SUBJECTS) res.subjects.put(s, new SubjectResult(s));

        Set<Long> seenIds = new HashSet<>();                       // deduplicate by record_id
        Map<String, GradeRow> attendanceByDate = new LinkedHashMap<>();   // keyed by date|time|record_id
        Map<String, GradeRow> bonusByProgram = new LinkedHashMap<>();     // newest record per program wins
        Map<String, List<Integer>> exams = new LinkedHashMap<>();
        for (String s : SUBJECTS) exams.put(s, new ArrayList<>());

        for (GradeRow r : records) {
            if (!seenIds.add(r.recordId)) continue;
            String activity = normActivity(r.activity);

            if (activity.equals("attendance")) {
                if (GradingCalculator.parseAttendanceStatus(r.score) == null) continue;
                // One submitted attendance row = one school day. A submission is identified by
                // date + submission time + record_id. The date alone is not enough (the teacher may enter
                // several days on one date, or enter late), and date + time alone is not enough either
                // (rows saved within the same second share both), so record_id is part of the key.
                attendanceByDate.put(r.date.trim().toLowerCase(Locale.ROOT) + "|" + r.time.trim() + "|" + r.recordId, r);
            } else if (activity.equals("exam")) {
                String subj = GradingCalculator.canonicalSubject(r.subject);
                int raw = GradingCalculator.parseExamScore(r.score);
                if (subj == null || raw < 0) {
                    res.warnings.add("Ignored exam record " + r.recordId + " (unknown subject or score outside 1-20)");
                    continue;
                }
                exams.get(subj).add(raw);
            } else if (activity.equals("book") || activity.equals("homework")) {
                int stars = GradingCalculator.parseStarRating(r.score);
                if (stars < 0) { res.warnings.add("Ignored rating record " + r.recordId + " (not 0-3 stars)"); continue; }
                if (activity.equals("homework")) {
                    for (SubjectResult sr : res.subjects.values()) addStars(sr, stars);
                } else {
                    String subj = GradingCalculator.canonicalSubject(r.subject);
                    if (subj == null) { res.warnings.add("Ignored Books record " + r.recordId + " (unknown subject)"); continue; }
                    addStars(res.subjects.get(subj), stars);
                    res.subjects.get(subj).ownBooksCount++;
                }
            } else if (activity.equals("programperformance")) {
                if (GradingCalculator.parseProgramPoints(r.score) < 0) {
                    res.warnings.add("Ignored Program Performance record " + r.recordId + " (unreadable points)");
                    continue;
                }
                if (GradingCalculator.programSubjects(r.subject).isEmpty()) {
                    res.warnings.add("Ignored Program Performance record " + r.recordId + " (unknown subject)");
                    continue;
                }
                String name = r.activityName.trim().toLowerCase(Locale.ROOT);
                String key = name.isEmpty() ? "record-" + r.recordId
                        : GradingCalculator.programSubjects(r.subject) + "|" + name;
                GradeRow old = bonusByProgram.get(key);
                if (old == null || r.recordId > old.recordId) bonusByProgram.put(key, r);
            }
        }

        // Attendance: total school days = present + absent
        for (GradeRow r : attendanceByDate.values()) {
            if (Boolean.TRUE.equals(GradingCalculator.parseAttendanceStatus(r.score))) res.attendancePresent++;
            else res.attendanceAbsent++;
        }
        int calculatedTotal = res.attendancePresent + res.attendanceAbsent;
        res.attendanceTotal = totalSchoolDaysOverride > 0 ? Math.max(calculatedTotal, totalSchoolDaysOverride) : calculatedTotal;
        res.attendanceRate = GradingCalculator.calculateAttendanceRate(res.attendancePresent, res.attendanceTotal);
        if (res.attendanceTotal > 0 && res.attendanceAbsent == 0 && totalSchoolDaysOverride <= 0) {
            res.warnings.add("No absent days are recorded; attendance is " + res.attendancePresent + "/" + res.attendanceTotal
                    + ". If the school year has more days than were recorded, the attendance rate is overstated.");
        }

        // Program Performance bonus points
        Map<String, Double> bonus = new LinkedHashMap<>();
        for (String s : SUBJECTS) bonus.put(s, 0.0);
        for (GradeRow r : bonusByProgram.values()) {
            double pts = GradingCalculator.parseProgramPoints(r.score);
            for (String s : GradingCalculator.programSubjects(r.subject)) bonus.put(s, bonus.get(s) + pts);
        }

        double initialSum = 0;
        for (String s : SUBJECTS) {
            SubjectResult sr = res.subjects.get(s);
            List<Integer> list = exams.get(s);
            sr.examRawScores.addAll(list);
            if (!list.isEmpty()) {
                double sum = 0;
                for (int raw : list) sum += GradingCalculator.examEquivalentGrade(raw);
                sr.examGrade = sum / list.size();
            }
            if (sr.ratingCount > 0) sr.booksHwRate = GradingCalculator.calculateBooksHwRate(sr.starsEarned, sr.starsPossible);
            sr.programPoints = bonus.get(s);

            boolean hasExam = sr.hasExam();
            boolean hasAttendance = res.attendanceTotal > 0;
            boolean hasBooksHw = sr.hasBooksHw();
            // Attendance and Homework are shared by all five subjects, so they never make a subject "graded".
            // A subject has data only once it has its own exam or its own Books ratings.
            boolean anyData = hasExam || sr.ownBooksCount > 0;
            boolean complete = hasExam && hasAttendance && hasBooksHw;
            sr.status = complete ? Status.COMPLETE : (anyData ? Status.INCOMPLETE : Status.NO_DATA);

            if (!anyData) continue;

            sr.examContribution = hasExam ? sr.examGrade * GradingCalculator.EXAM_WEIGHT : 0.0;
            sr.attendanceContribution = hasAttendance ? res.attendanceRate * GradingCalculator.ATTENDANCE_WEIGHT : 0.0;
            sr.booksHwContribution = hasBooksHw ? sr.booksHwRate * GradingCalculator.BOOKS_HW_WEIGHT : 0.0;
            sr.initialBeforeBonus = sr.examContribution + sr.attendanceContribution + sr.booksHwContribution;
            sr.adjustedInitial = sr.initialBeforeBonus + sr.programPoints;    // bonus BEFORE transmutation
            sr.transmuted = GradingCalculator.transmuteGrade(sr.adjustedInitial);

            if (complete) {
                res.completeSubjects++;
                initialSum += sr.adjustedInitial;
            }
        }

        if (res.completeSubjects == SUBJECTS.size()) {
            res.overallComplete = true;
            res.overallInitial = initialSum / SUBJECTS.size();          // average initials, THEN transmute
            res.overallTransmuted = GradingCalculator.transmuteGrade(res.overallInitial);
        }
        return res;
    }

    private static void addStars(SubjectResult sr, int stars) {
        sr.starsEarned += stars;
        sr.starsPossible += GradingCalculator.MAX_STARS_PER_RATING;
        sr.ratingCount++;
    }

    private static String normActivity(String a) {
        String k = a == null ? "" : a.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (k.equals("exams")) return "exam";
        if (k.equals("books")) return "book";
        if (k.equals("homeworks")) return "homework";
        if (k.equals("attendances")) return "attendance";
        return k;
    }
}