package com.example.priangradingsystem.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The database layout (version 5) and the one routine that brings ANY older layout up to it.
 * Pure Java on purpose: it only talks to the database through {@link SqlExec}.
 *
 * <pre>
 * teachers      login accounts
 * school_years  "2026-2027", exactly one is_current
 * students      the person: name, birthdate, guardian, photo (no level / year: those change every year)
 * enrollments   student + school year -> level for that year
 * grade_records every score, tied to a student AND a school year
 * </pre>
 */
public final class DbSchema {
    public static final int VERSION = 5;

    /** Minimal database access so the migration can run on Android and in plain-Java tests. */
    public interface SqlExec {
        void exec(String sql, Object... args);
        List<String[]> query(String sql, String... args);
    }

    private DbSchema() {}

    static final String CREATE_TEACHERS =
            "CREATE TABLE teachers (" +
            "teacher_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "full_name TEXT NOT NULL, " +
            "username TEXT NOT NULL UNIQUE, " +
            "password_hash TEXT NOT NULL)";

    static final String CREATE_SCHOOL_YEARS =
            "CREATE TABLE school_years (" +
            "school_year_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "label TEXT NOT NULL UNIQUE, " +
            "is_current INTEGER NOT NULL DEFAULT 0 CHECK (is_current IN (0, 1)))";

    private static String createStudents(String name) {
        return "CREATE TABLE " + name + " (" +
                "student_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "first_name TEXT NOT NULL, " +
                "middle_name TEXT, " +
                "last_name TEXT NOT NULL, " +
                "name_extension TEXT, " +
                "birthdate TEXT, " +
                "sex TEXT, " +
                "guardian_first_name TEXT, " +
                "guardian_middle_name TEXT, " +
                "guardian_last_name TEXT, " +
                "guardian_extension TEXT, " +
                "guardian_contact TEXT, " +
                "address TEXT, " +
                "photo_uri TEXT)";
    }

    static final String CREATE_ENROLLMENTS =
            "CREATE TABLE enrollments (" +
            "enrollment_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "student_id INTEGER NOT NULL REFERENCES students(student_id) ON DELETE CASCADE, " +
            "school_year_id INTEGER NOT NULL REFERENCES school_years(school_year_id), " +
            "level TEXT NOT NULL DEFAULT 'Nursery', " +
            "UNIQUE (student_id, school_year_id))";

    private static String createGradeRecords(String name) {
        return "CREATE TABLE " + name + " (" +
                "record_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL REFERENCES students(student_id) ON DELETE CASCADE, " +
                "school_year_id INTEGER NOT NULL REFERENCES school_years(school_year_id), " +
                "subject TEXT NOT NULL, " +
                "quarter TEXT NOT NULL, " +
                "activity TEXT, " +
                "activity_name TEXT, " +
                "score TEXT, " +
                "remarks TEXT, " +
                "date TEXT, " +
                "time TEXT)";
    }

    static final String[] INDEXES = {
            "CREATE INDEX IF NOT EXISTS idx_students_name ON students(last_name, first_name)",
            "CREATE INDEX IF NOT EXISTS idx_enrollments_year ON enrollments(school_year_id)",
            "CREATE INDEX IF NOT EXISTS idx_grades_lookup ON grade_records(student_id, school_year_id, quarter)"
    };

    /** Tables from older versions that nothing in the app uses any more. */
    private static final String[] LEGACY_TABLES = {
            "assessments", "assessment_types", "classes", "subjects", "grading_periods", "attendance_records"
    };

    /** CREATE statements for a brand-new empty database (no seed rows), in dependency order. */
    public static List<String> createStatements() {
        List<String> s = new ArrayList<>();
        s.add(CREATE_TEACHERS);
        s.add(CREATE_SCHOOL_YEARS);
        s.add(createStudents("students"));
        s.add(CREATE_ENROLLMENTS);
        s.add(createGradeRecords("grade_records"));
        for (String i : INDEXES) s.add(i);
        return s;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Idempotent. Creates what is missing and converts old layouts, keeping every student and score:
     * all existing students are enrolled in the current school year (with the level they had) and all
     * existing grade records are assigned to it.
     */
    public static void upgrade(SqlExec x, Calendar now) {
        Set<String> tables = tableNames(x);

        if (!tables.contains("teachers")) x.exec(CREATE_TEACHERS);
        if (!tables.contains("school_years")) x.exec(CREATE_SCHOOL_YEARS);
        long yearId = ensureCurrentYear(x, now);

        // ---- students -------------------------------------------------------------------------
        boolean hadStudents = tables.contains("students");
        Set<String> studentCols = hadStudents ? columnNames(x, "students") : new HashSet<String>();
        boolean studentsLegacy = hadStudents && !studentCols.contains("name_extension");

        // An enrollments table without school_year_id is the unused class-based leftover.
        if (tables.contains("enrollments") && !columnNames(x, "enrollments").contains("school_year_id")) {
            x.exec("DROP TABLE enrollments");
            tables.remove("enrollments");
        }
        if (!tables.contains("enrollments")) {
            // Created before students is rebuilt: foreign keys are not enforced while the schema changes.
            x.exec(CREATE_ENROLLMENTS);
        }

        if (!hadStudents) {
            x.exec(createStudents("students"));
        } else if (studentsLegacy) {
            String level = studentCols.contains("level")
                    ? "CASE WHEN TRIM(COALESCE(level, '')) = '' THEN 'Nursery' " +
                      "WHEN instr(level, '-') > 0 THEN TRIM(substr(level, 1, instr(level, '-') - 1)) " +
                      "ELSE TRIM(level) END"
                    : "'Nursery'";
            x.exec("INSERT OR IGNORE INTO enrollments (student_id, school_year_id, level) " +
                    "SELECT student_id, ?, " + level + " FROM students", yearId);

            x.exec(createStudents("students_v5"));
            x.exec("INSERT INTO students_v5 (student_id, first_name, middle_name, last_name, birthdate, sex, " +
                    "guardian_contact, address, photo_uri) " +
                    "SELECT student_id, COALESCE(first_name, ''), " + pick(studentCols, "middle_name") + ", " +
                    "COALESCE(last_name, ''), " + pick(studentCols, "birthdate") + ", " + pick(studentCols, "sex") + ", " +
                    pick(studentCols, "guardian_contact") + ", " + pick(studentCols, "address") + ", " +
                    "NULLIF(" + pick(studentCols, "photo_uri") + ", '') FROM students");
            if (studentCols.contains("guardian_name")) {
                for (String[] row : x.query("SELECT student_id, guardian_name FROM students")) {
                    String[] p = NameSplitter.split(row[1]);
                    x.exec("UPDATE students_v5 SET guardian_first_name = ?, guardian_middle_name = ?, " +
                            "guardian_last_name = ?, guardian_extension = ? WHERE student_id = ?",
                            p[0], p[1], p[2], p[3], Long.parseLong(row[0]));
                }
            }
            x.exec("DROP TABLE students");
            x.exec("ALTER TABLE students_v5 RENAME TO students");
        }

        // ---- grade_records --------------------------------------------------------------------
        boolean hadGrades = tables.contains("grade_records");
        Set<String> gradeCols = hadGrades ? columnNames(x, "grade_records") : new HashSet<String>();
        if (!hadGrades) {
            x.exec(createGradeRecords("grade_records"));
        } else if (!gradeCols.contains("school_year_id")) {
            x.exec(createGradeRecords("grade_records_v5"));
            if (gradeCols.contains("student_id")) {
                String q = pick(gradeCols, "quarter");
                String quarter = "CASE WHEN " + q + " LIKE '%1%' THEN '1st Quarter' " +
                        "WHEN " + q + " LIKE '%2%' THEN '2nd Quarter' " +
                        "WHEN " + q + " LIKE '%3%' THEN '3rd Quarter' " +
                        "WHEN " + q + " LIKE '%4%' THEN '4th Quarter' " +
                        "ELSE COALESCE(CAST(" + q + " AS TEXT), '1st Quarter') END";
                x.exec("INSERT INTO grade_records_v5 (record_id, student_id, school_year_id, subject, quarter, " +
                        "activity, activity_name, score, remarks, date, time) " +
                        "SELECT record_id, student_id, ?, COALESCE(CAST(" + pick(gradeCols, "subject") + " AS TEXT), ''), " +
                        quarter + ", " + pick(gradeCols, "activity") + ", " + pick(gradeCols, "activity_name") + ", " +
                        "CAST(" + pick(gradeCols, "score") + " AS TEXT), " + pick(gradeCols, "remarks") + ", " +
                        "CAST(" + pick(gradeCols, "date") + " AS TEXT), " + pick(gradeCols, "time") + " " +
                        "FROM grade_records WHERE student_id IN (SELECT student_id FROM students)", yearId);
            }
            x.exec("DROP TABLE grade_records");
            x.exec("ALTER TABLE grade_records_v5 RENAME TO grade_records");
        }

        // ---- unused leftovers and indexes -----------------------------------------------------
        for (String t : LEGACY_TABLES) {
            if (tables.contains(t)) x.exec("DROP TABLE " + t);
        }
        x.exec("DROP INDEX IF EXISTS idx_grades_student");
        for (String idx : INDEXES) x.exec(idx);
    }

    /** Problems found in the final layout (empty list = healthy). Used by tests and diagnostics. */
    public static List<String> check(SqlExec x) {
        List<String> problems = new ArrayList<>();
        Set<String> tables = tableNames(x);
        String[] wanted = {"teachers", "school_years", "students", "enrollments", "grade_records"};
        for (String t : wanted) if (!tables.contains(t)) problems.add("missing table " + t);
        for (String t : tables) {
            boolean known = t.startsWith("sqlite_") || t.equals("android_metadata");
            for (String w : wanted) if (w.equals(t)) known = true;
            if (!known) problems.add("unexpected table " + t);
        }
        if (!x.query("PRAGMA foreign_key_check").isEmpty()) problems.add("foreign key violations");
        List<String[]> cur = x.query("SELECT COUNT(*) FROM school_years WHERE is_current = 1");
        if (cur.isEmpty() || !"1".equals(cur.get(0)[0])) problems.add("there must be exactly one current school year");
        return problems;
    }

    // ---------------------------------------------------------------------------------------------

    private static long ensureCurrentYear(SqlExec x, Calendar now) {
        List<String[]> rows = x.query("SELECT school_year_id, label, is_current FROM school_years ORDER BY label DESC");
        if (rows.isEmpty()) {
            x.exec("INSERT INTO school_years (label, is_current) VALUES (?, 1)", SchoolYear.labelFor(now));
            rows = x.query("SELECT school_year_id, label, is_current FROM school_years");
        }
        long keep = -1;
        for (String[] r : rows) if ("1".equals(r[2])) { keep = Long.parseLong(r[0]); break; }
        if (keep < 0) keep = Long.parseLong(rows.get(0)[0]); // newest label
        x.exec("UPDATE school_years SET is_current = CASE WHEN school_year_id = ? THEN 1 ELSE 0 END", keep);
        return keep;
    }

    /** Column name if the old table has it, otherwise NULL, so one INSERT works for every old layout. */
    private static String pick(Set<String> cols, String name) {
        return cols.contains(name) ? name : "NULL";
    }

    static Set<String> tableNames(SqlExec x) {
        Set<String> out = new HashSet<>();
        for (String[] r : x.query("SELECT name FROM sqlite_master WHERE type = 'table'")) out.add(r[0].toLowerCase(Locale.ROOT));
        return out;
    }

    static Set<String> columnNames(SqlExec x, String table) {
        Set<String> out = new HashSet<>();
        for (String[] r : x.query("PRAGMA table_info(" + table + ")")) out.add(r[1].toLowerCase(Locale.ROOT));
        return out;
    }
}
