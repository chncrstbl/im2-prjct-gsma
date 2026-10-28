package com.example.priangradingsystem.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import com.example.priangradingsystem.model.Teacher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("all")
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "Database.db";
    private static final int DATABASE_VERSION = 4;
    private final Context context;
    private final File dbPath;
    private String lastError = "";

    public String getLastError() { return lastError; }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
        this.dbPath = context.getDatabasePath(DATABASE_NAME);
        copyDatabaseIfNeeded();
    }

    private void copyDatabaseIfNeeded() {
        if (!dbPath.exists() || dbPath.length() == 0) {
            File parentDir = dbPath.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    Log.w(TAG, "Failed to create directory: " + parentDir.getAbsolutePath());
                }
            }

            try (InputStream input = context.getAssets().open("Database.db");
                 OutputStream output = new FileOutputStream(dbPath)) {
                byte[] buffer = new byte[1024];
                int readLength;
                while ((readLength = input.read(buffer)) > 0) {
                    output.write(buffer, 0, readLength);
                }
                output.flush();
                Log.d(TAG, "Database copied successfully from assets.");
            } catch (IOException e) {
                Log.e(TAG, "Error copying database from assets", e);
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ensureTablesAndColumns(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        ensureTablesAndColumns(db);
    }

    private void createTables(SQLiteDatabase db) {
        try {
            String createTeachersTable = "CREATE TABLE IF NOT EXISTS teachers (" +
                    "teacher_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "full_name TEXT NOT NULL, " +
                    "username TEXT NOT NULL UNIQUE, " +
                    "password_hash TEXT NOT NULL" +
                    ");";
            db.execSQL(createTeachersTable);

            String insertDefaultTeacher = "INSERT OR IGNORE INTO teachers (full_name, username, password_hash) " +
                    "VALUES ('Marian F. Ceron', 'Prian', 'Ham20456');";
            db.execSQL(insertDefaultTeacher);

            String createStudentsTable = "CREATE TABLE IF NOT EXISTS students (" +
                    "student_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "first_name TEXT NOT NULL, " +
                    "middle_name TEXT, " +
                    "last_name TEXT NOT NULL, " +
                    "birthdate TEXT, " +
                    "sex TEXT, " +
                    "age TEXT, " +
                    "guardian_name TEXT, " +
                    "guardian_contact TEXT, " +
                    "address TEXT, " +
                    "photo_uri TEXT, " +
                    "status TEXT NOT NULL DEFAULT 'Active', " +
                    "date_added TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "level TEXT DEFAULT 'Nursery'" +
                    ");";
            db.execSQL(createStudentsTable);

            createGradeRecordsTable(db);
            createAttendanceTable(db);
        } catch (Exception e) {
            Log.e(TAG, "Error creating database tables", e);
        }
    }

    private void createGradeRecordsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS grade_records (" +
                "record_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, " +
                "subject TEXT NOT NULL, " +
                "quarter TEXT NOT NULL, " +
                "score TEXT, " +
                "remarks TEXT, " +
                "date TEXT, " +
                "activity TEXT, " +
                "activity_name TEXT, " +
                "FOREIGN KEY(student_id) REFERENCES students(student_id) ON DELETE CASCADE" +
                ");");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_grades_student ON grade_records(student_id)");
    }

    /** One attendance status per student and calendar date. */
    private void createAttendanceTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS attendance_records (" +
                "attendance_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, " +
                "attendance_date TEXT NOT NULL, " +
                "status TEXT NOT NULL CHECK(status IN ('Present', 'Absent', 'Suspended')), " +
                "UNIQUE(student_id, attendance_date), " +
                "FOREIGN KEY(student_id) REFERENCES students(student_id) ON DELETE CASCADE" +
                ");");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_attendance_student_date " +
                "ON attendance_records(student_id, attendance_date)");
    }

    /** Counts Present attendance entries for one student across all quarters. */
    public int getPresentAttendanceCount(long studentId, String quarter) {
        if (studentId < 1 || quarter == null) return 0;

        String sql = "SELECT COUNT(*) FROM grade_records " +
                "WHERE student_id = ? " +
                "AND LOWER(TRIM(activity)) = 'attendance' " +
                "AND LOWER(TRIM(score)) = 'present' " +
                "AND LOWER(TRIM(quarter)) = LOWER(TRIM(?))";

        try (Cursor c = getReadableDatabase().rawQuery(
                sql,
                new String[]{String.valueOf(studentId), quarter.trim()})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Exception e) {
            Log.e(TAG, "Could not count present attendance entries", e);
            return 0;
        }
    }

    public List<String> getPresentAttendanceDates(
            long studentId, String quarter) {

        List<String> dates = new ArrayList<>();
        if (studentId < 1 || quarter == null) return dates;

        String sql = "SELECT date FROM grade_records " +
                "WHERE student_id = ? " +
                "AND LOWER(TRIM(activity)) = 'attendance' " +
                "AND LOWER(TRIM(score)) = 'present' " +
                "AND LOWER(TRIM(quarter)) = LOWER(TRIM(?)) " +
                "ORDER BY date DESC";

        try (Cursor c = getReadableDatabase().rawQuery(
                sql,
                new String[]{String.valueOf(studentId), quarter.trim()})) {
            while (c.moveToNext()) {
                String date = c.getString(0);
                if (date != null && !date.trim().isEmpty()) {
                    dates.add(date);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not retrieve present attendance dates", e);
        }

        return dates;
    }

    public Cursor getPresentAttendanceRecords(long studentId, String quarter) {
        if (studentId < 1 || quarter == null) {return null;}

        String sql = "SELECT record_id, date, time, score " +
                "FROM grade_records " +
                "WHERE student_id = ? " +
                "AND LOWER(TRIM(activity)) = 'attendance' " +
                "AND LOWER(TRIM(score)) = 'present' " +
                "AND LOWER(TRIM(quarter)) = LOWER(TRIM(?)) " +
                "ORDER BY date DESC, time DESC, record_id DESC";

        return getReadableDatabase().rawQuery(
                sql,
                new String[]{String.valueOf(studentId), quarter}
        );
    }

    public Cursor getAttendanceRecords(long studentId, String quarter) {
        if (studentId < 1 || quarter == null) return null;

        String sql = "SELECT record_id, date, time, score " +
                "FROM grade_records " +
                "WHERE student_id = ? " +
                "AND LOWER(TRIM(activity)) = 'attendance' " +
                "AND LOWER(TRIM(quarter)) = LOWER(TRIM(?)) " +
                "ORDER BY date DESC, time DESC, record_id DESC";

        return getReadableDatabase().rawQuery(
                sql,
                new String[]{String.valueOf(studentId), quarter.trim()}
        );
    }

    public String getAttendanceStatus(long studentId, String date) {
        if (studentId < 1 || date == null) return "";
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT score FROM grade_records WHERE student_id = ? AND LOWER(activity) = 'attendance' AND (date = ? OR remarks = ?) ORDER BY record_id DESC LIMIT 1",
                new String[]{String.valueOf(studentId), date, date})) {
            return c.moveToFirst() ? c.getString(0) : "";
        } catch (Exception e) {
            Log.e(TAG, "Could not read attendance status from grade_records", e);
            return "";
        }
    }

    public boolean saveAttendanceStatus(
            long studentId,
            String date,
            String status,
            String quarter,
            String subject,
            String remarks) {

        if (studentId < 1 || date == null || date.trim().isEmpty()) {
            return false;
        }

        try {
            SQLiteDatabase db = getWritableDatabase();

            String cleanQuarter =
                    quarter != null ? quarter : "1st Quarter";
            String cleanSubject =
                    subject != null ? subject : "Attendance";

            String currentTime = new java.text.SimpleDateFormat(
                    "HH:mm:ss",
                    java.util.Locale.getDefault()
            ).format(new java.util.Date());

            ContentValues values = new ContentValues();
            values.put("student_id", studentId);
            values.put("subject", cleanSubject);
            values.put("quarter", cleanQuarter);
            values.put("activity", "Attendance");
            values.put("score", status != null ? status : "");
            values.put("date", date);
            values.put("remarks", remarks != null ? remarks : "");
            values.put("time", currentTime);

            long newId = db.insertOrThrow(
                    "grade_records",
                    null,
                    values
            );

            return newId != -1;

        } catch (Exception e) {
            Log.e(TAG, "Could not save attendance status", e);
            return false;
        }
    }

    public boolean deleteAttendanceStatus(long studentId, String date) {
        if (studentId < 1 || date == null) return false;
        try {
            return getWritableDatabase().delete(
                    "grade_records", "student_id = ? AND LOWER(activity) = 'attendance' AND (date = ? OR remarks = ?)",
                    new String[]{String.valueOf(studentId), date, date}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "Could not delete attendance status from grade_records", e);
            return false;
        }
    }

    public boolean deleteAttendanceRecord(long recordId) {
        if (recordId < 1) return false;

        try {
            return getWritableDatabase().delete(
                    "grade_records",
                    "record_id = ? AND LOWER(TRIM(activity)) = 'attendance'",
                    new String[]{String.valueOf(recordId)}
            ) > 0;
        } catch (Exception e) {
            Log.e(TAG, "Could not delete attendance record", e);
            return false;
        }
    }

    /**
     * Brings grade_records to the standard layout:
     * record_id, student_id, subject, quarter, score, remarks, date, activity.
     * Removes extra columns (student_name, student_level, assessment_date, date_submitted)
     * and the UNIQUE(student_id, subject, quarter) rule that blocked saving more than one
     * activity per subject/quarter. Existing rows are carried over.
     */
    private void migrateGradeRecordsLayout(SQLiteDatabase db) {
        java.util.Set<String> cols = new java.util.HashSet<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(grade_records)", null)) {
            int n = c.getColumnIndex("name");
            while (c.moveToNext()) cols.add(c.getString(n).toLowerCase());
        }
        String sql = null;
        try (Cursor c = db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='grade_records'", null)) {
            if (c.moveToFirst()) sql = c.getString(0);
        }
        boolean hasExtras = cols.contains("student_name") || cols.contains("student_level")
                || cols.contains("assessment_date") || cols.contains("date_submitted");
        boolean hasUnique = sql != null && sql.toUpperCase().contains("UNIQUE");
        if (!hasExtras && !hasUnique) return;

        db.beginTransaction();
        try {
            db.execSQL("ALTER TABLE grade_records RENAME TO grade_records_old");
            db.execSQL("DROP INDEX IF EXISTS idx_grades_student");
            createGradeRecordsTable(db);


            String idExpr = cols.contains("student_name")
                    ? "COALESCE(o.student_id, (SELECT s.student_id FROM students s " +
                      "WHERE LOWER(s.last_name || ', ' || s.first_name) = LOWER(o.student_name) LIMIT 1))"
                    : (cols.contains("student_id") ? "o.student_id" : "NULL");
            String dateExpr = cols.contains("assessment_date")
                    ? "COALESCE(NULLIF(o.assessment_date, ''), " + (cols.contains("date") ? "o.date" : "NULL") + ")"
                    : (cols.contains("date") ? "o.date" : "NULL");

            db.execSQL("INSERT INTO grade_records (student_id, subject, quarter, score, remarks, date, activity, activity_name) " +
                    "SELECT " + idExpr + ", " +
                    "COALESCE(" + (cols.contains("subject") ? "o.subject" : "NULL") + ", ''), " +
                    "COALESCE(" + (cols.contains("quarter") ? "o.quarter" : "NULL") + ", ''), " +
                    (cols.contains("score") ? "o.score" : "NULL") + ", " +
                    (cols.contains("remarks") ? "o.remarks" : "NULL") + ", " +
                    dateExpr + ", " +
                    (cols.contains("activity") ? "o.activity" : "NULL") + ", " +
                    (cols.contains("activity_name") ? "o.activity_name" : "NULL") +
                    " FROM grade_records_old o WHERE " + idExpr + " IS NOT NULL");
            db.execSQL("DROP TABLE grade_records_old");
            db.setTransactionSuccessful();
            Log.d(TAG, "grade_records migrated to standard layout.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to migrate grade_records", e);
        } finally {
            db.endTransaction();
        }
    }

    private void ensureTablesAndColumns(SQLiteDatabase db) {
        try {
            createTables(db);
            migrateGradeRecordsLayout(db);

            // Every column the app writes to. Any that are missing in an older/bundled
            // database are added here, so inserts never fail with "no such column".
            String[][] required = {
                    {"middle_name", "TEXT"},
                    {"birthdate", "TEXT"},
                    {"sex", "TEXT"},
                    {"age", "TEXT"},
                    {"guardian_name", "TEXT"},
                    {"guardian_contact", "TEXT"},
                    {"address", "TEXT"},
                    {"photo_uri", "TEXT"},
                    {"status", "TEXT DEFAULT 'Active'"},
                    {"date_added", "TEXT"},
                    {"level", "TEXT DEFAULT 'Nursery'"}
            };

            java.util.Set<String> existing = new java.util.HashSet<>();
            try (Cursor cursor = db.rawQuery("PRAGMA table_info(students)", null)) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if (nameIndex != -1) existing.add(cursor.getString(nameIndex).toLowerCase());
                }
            }

            for (String[] col : required) {
                if (!existing.contains(col[0])) {
                    try {
                        db.execSQL("ALTER TABLE students ADD COLUMN " + col[0] + " " + col[1] + ";");
                        Log.d(TAG, "Added missing column: " + col[0]);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to add column " + col[0], e);
                    }
                }
            }

            // Ensure grade_records table has all required columns
            String[][] gradeRecordsCols = {
                    {"student_id", "INTEGER"},
                    {"subject", "TEXT"},
                    {"quarter", "TEXT"},
                    {"score", "TEXT"},
                    {"remarks", "TEXT"},
                    {"date", "TEXT"},
                    {"time","TEXT"},
                    {"activity", "TEXT"},
                    {"activity_name", "TEXT"}
            };

            java.util.Set<String> existingGradeCols = new java.util.HashSet<>();
            try (Cursor cursor = db.rawQuery("PRAGMA table_info(grade_records)", null)) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if (nameIndex != -1) existingGradeCols.add(cursor.getString(nameIndex).toLowerCase());
                }
            }

            for (String[] col : gradeRecordsCols) {
                if (!existingGradeCols.contains(col[0])) {
                    try {
                        db.execSQL("ALTER TABLE grade_records ADD COLUMN " + col[0] + " " + col[1] + ";");
                        Log.d(TAG, "Added missing grade_records column: " + col[0]);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to add grade_records column " + col[0], e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in ensureTablesAndColumns", e);
        }
    }

    @SuppressWarnings("unused")
    public boolean authenticateTeacher(String username, String password) {
        if (username == null || password == null) return false;

        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM teachers WHERE LOWER(username) = LOWER(?) AND password_hash = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{username.trim(), password.trim()})) {
            return cursor.moveToFirst();
        } catch (Exception e) {
            Log.e(TAG, "Error authenticating teacher", e);
            return false;
        }
    }

    public Teacher getTeacherByCredentials(String username, String password) {
        if (username == null || password == null) return null;

        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT teacher_id, full_name, username, password_hash FROM teachers " +
                "WHERE LOWER(username) = LOWER(?) AND password_hash = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{username.trim(), password.trim()})) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("teacher_id"));
                String fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name"));
                String uName = cursor.getString(cursor.getColumnIndexOrThrow("username"));
                String pass = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"));
                return new Teacher(id, fullName, uName, pass);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching teacher by credentials", e);
        }
        return null;
    }

    public String getStudentSex(long studentId) {
        if (studentId < 1) return "";
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT sex FROM students WHERE student_id = ?",
                new String[]{String.valueOf(studentId)})) {
            return c.moveToFirst() ? (c.getString(0) != null ? c.getString(0) : "") : "";
        } catch (Exception e) {
            Log.e(TAG, "Error in getStudentSex", e);
            return "";
        }
    }

    // CRUD: (Nursery / Toddler)
    public boolean insertStudent(String firstName, String middleName, String lastName, String birthdate, String sex, String age, String guardianName, String guardianContact, String address, String photoUri, String level) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("first_name", firstName != null ? firstName : "");
            values.put("middle_name", middleName != null ? middleName : "");
            values.put("last_name", lastName != null ? lastName : "");
            values.put("birthdate", birthdate != null ? birthdate : "");
            values.put("sex", sex != null ? sex : "");
            values.put("age", age != null ? age : "");
            values.put("guardian_name", guardianName != null ? guardianName : "");
            values.put("guardian_contact", guardianContact != null ? guardianContact : "");
            values.put("address", address != null ? address : "");
            values.put("photo_uri", photoUri != null ? photoUri : "");
            values.put("level", level != null ? level : "Nursery");

            lastError = "";
            long result = db.insertOrThrow("students", null, values);
            return result != -1;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error in insertStudent", e);
            return false;
        }
    }

    // CRUD: Update Student
    public boolean updateStudent(int studentId, String firstName, String middleName, String lastName, String birthdate, String sex, String age, String guardianName, String guardianContact, String address, String photoUri, String level) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("first_name", firstName != null ? firstName : "");
            values.put("middle_name", middleName != null ? middleName : "");
            values.put("last_name", lastName != null ? lastName : "");
            values.put("birthdate", birthdate != null ? birthdate : "");
            values.put("sex", sex != null ? sex : "");
            values.put("age", age != null ? age : "");
            values.put("guardian_name", guardianName != null ? guardianName : "");
            values.put("guardian_contact", guardianContact != null ? guardianContact : "");
            values.put("address", address != null ? address : "");
            values.put("photo_uri", photoUri != null ? photoUri : "");
            values.put("level", level != null ? level : "Nursery");

            lastError = "";
            int result = db.update("students", values, "student_id = ?", new String[]{String.valueOf(studentId)});
            if (result <= 0) {
                lastError = "No student found with id " + studentId;
                Log.e(TAG, "updateStudent failed: " + lastError);
            }
            return result > 0;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error in updateStudent", e);
            return false;
        }
    }

    public boolean deleteStudent(int studentId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            int result = db.delete("students", "student_id = ?", new String[]{String.valueOf(studentId)});
            return result > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error in deleteStudent", e);
            return false;
        }
    }

    public Cursor getAllStudents() {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            return db.rawQuery("SELECT * FROM students ORDER BY last_name COLLATE NOCASE ASC", null);
        } catch (Exception e) {
            Log.e(TAG, "Error in getAllStudents", e);
            return null;
        }
    }

    private long findStudentId(SQLiteDatabase db, String studentName) {
        if (studentName == null || studentName.trim().isEmpty()) return -1;
        String name = studentName.trim();
        String query = "SELECT student_id FROM students WHERE " +
                "LOWER(last_name || ', ' || first_name) = LOWER(?) OR " +
                "LOWER(first_name || ' ' || last_name) = LOWER(?) OR " +
                "LOWER(first_name || ' ' || COALESCE(middle_name, '') || ' ' || last_name) = LOWER(?) " +
                "LIMIT 1";
        try (Cursor c = db.rawQuery(query, new String[]{name, name, name})) {
            if (c.moveToFirst()) return c.getLong(0);
        }
        return -1;
    }

    /**
     * Next "book" label for this student's subject + quarter + activity: continues the count
     * (book1, book2, book3, ...) while student, subject, quarter AND activity (Exam / Books /
     * Homework) all stay the same, and starts back at "book1" as soon as any of them changes,
     * since the count only looks at rows that already match all four.
     */
    private String getNextActivityName(SQLiteDatabase db, long studentId, String subject, String quarter, String activity) {
        int maxNum = 0;
        String prefix = "item";
        if (activity != null) {
            if (activity.equalsIgnoreCase("Exam")) prefix = "exam";
            else if (activity.equalsIgnoreCase("Homework")) prefix = "homework";
            else if (activity.equalsIgnoreCase("Books")) prefix = "book";
            else prefix = activity.toLowerCase();
        }

        String query = "SELECT activity_name FROM grade_records WHERE student_id = ? AND subject = ? AND quarter = ? AND activity = ? AND activity_name LIKE ?";
        try (Cursor c = db.rawQuery(query, new String[]{String.valueOf(studentId), subject != null ? subject : "", quarter != null ? quarter : "", activity != null ? activity : "", prefix + "%"})) {
            while (c.moveToNext()) {
                String name = c.getString(0);
                if (name == null) continue;
                try {
                    int num = Integer.parseInt(name.replaceAll("(?i)^" + prefix, "").trim());
                    if (num > maxNum) maxNum = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        return prefix + (maxNum + 1);
    }

    // studentLevel and assessmentDate are kept in the signature for compatibility; neither is stored
    // (level lives in students.level, and the date is always the present date).
    public boolean insertGradeRecord(String studentName, String studentLevel, String quarter, String subject, String activity, String activityName, String score, String assessmentDate, String remarks) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            lastError = "";

            long studentId = findStudentId(db, studentName);
            if (studentId < 0) {
                ContentValues sValues = new ContentValues();
                String[] parts = studentName != null ? studentName.split(",") : new String[0];
                String lName = parts.length > 0 ? parts[0].trim() : "Student";
                String fName = parts.length > 1 ? parts[1].trim() : "New";
                sValues.put("first_name", fName);
                sValues.put("last_name", lName);
                sValues.put("level", studentLevel != null ? studentLevel : "Nursery");
                studentId = db.insert("students", null, sValues);
            }

            String date = (assessmentDate != null && !assessmentDate.trim().isEmpty()) ? assessmentDate : new java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault()).format(new java.util.Date());

            String finalActivityName = activityName != null ? activityName.trim() : "";
            if (finalActivityName.isEmpty()) {
                if (activity != null && activity.equalsIgnoreCase("Attendance")) {
                    finalActivityName = "N/A";
                } else {
                    finalActivityName = getNextActivityName(db, studentId, subject, quarter, activity);
                }
            }

            ContentValues values = new ContentValues();
            values.put("student_id", studentId);
            values.put("subject", subject != null ? subject : "");
            values.put("quarter", quarter != null ? quarter : "1st Quarter");
            values.put("score", score != null ? score : "");
            values.put("remarks", remarks != null ? remarks : "");
            values.put("date", date);
            values.put("activity", activity != null ? activity : "");
            values.put("activity_name", finalActivityName);

            long result = db.insertOrThrow("grade_records", null, values);
            return result != -1;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error in insertGradeRecord", e);
            return false;
        }
    }

    public boolean insertGradeRecord(String studentName, String studentLevel, String quarter, String subject, String activity, String score, String assessmentDate, String remarks) {
        return insertGradeRecord(studentName, studentLevel, quarter, subject, activity, "", score, assessmentDate, remarks);
    }

    /** Inserts an independent grade entry for an already-resolved student ID. */
    public boolean insertGradeRecordForStudent(long studentId, String quarter, String subject, String activity, String activityName, String score) {
        try {
            if (studentId <= 0) {
                lastError = "No student selected.";
                return false;
            }
            ContentValues values = new ContentValues();
            values.put("student_id", studentId);
            values.put("subject", subject != null ? subject.trim() : "N/A");
            values.put("quarter", normalizeQuarter(quarter));
            values.put("activity", activity != null ? activity.trim() : "");
            values.put("activity_name", activityName != null ? activityName.trim() : "");
            values.put("score", score != null ? score : "0");
            values.put("date", new java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault()).format(new java.util.Date()));
            lastError = "";
            return getWritableDatabase().insertOrThrow("grade_records", null, values) != -1;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error in insertGradeRecordForStudent", e);
            return false;
        }
    }

    // Returns the stored columns plus student_name / student_level / assessment_date as read-only aliases
    // (joined from students), so screens that read those names keep working.
    public Cursor getAllGradeRecords() {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            return db.rawQuery("SELECT g.record_id, g.student_id, g.subject, g.quarter, g.score, g.remarks, g.date, g.activity, g.activity_name, " +
                    "g.date AS assessment_date, " +
                    "COALESCE(s.last_name || ', ' || s.first_name, '') AS student_name, " +
                    "COALESCE(s.level, '') AS student_level " +
                    "FROM grade_records g LEFT JOIN students s ON s.student_id = g.student_id " +
                    "ORDER BY g.record_id DESC", null);
        } catch (Exception e) {
            Log.e(TAG, "Error in getAllGradeRecords", e);
            return null;
        }
    }

    // ------------------------------------------------------------------------------------
    // Grade record lookup / save / edit / delete
    //
    // Cross-quarter fix: the old lookup matched the quarter with LIKE '<first 3 chars>%' and
    // looked up the student by display name on every call. Quarters are now compared by an exact,
    // normalized value ("1", "1.0", "Quarter 1", "1st" -> "1st Quarter"), and RecordsActivity
    // works on student_id / record_id so one quarter's row can never be read or overwritten
    // while another quarter is selected.
    // ------------------------------------------------------------------------------------

    /** Canonical quarter label: "1st Quarter", "2nd Quarter", "3rd Quarter", "4th Quarter". */
    public static String normalizeQuarter(String quarter) {
        if (quarter == null) return "1st Quarter";
        String q = quarter.trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(q);
        if (!m.find()) return q;
        switch (Integer.parseInt(m.group(1))) {
            case 1: return "1st Quarter";
            case 2: return "2nd Quarter";
            case 3: return "3rd Quarter";
            case 4: return "4th Quarter";
            default: return q;
        }
    }

    private static String activityKey(String activity) {
        String a = activity == null ? "" : activity.trim().toLowerCase(java.util.Locale.ROOT);
        // "Book" == "Books", "Exam" == "Exams"
        return (a.length() > 3 && a.endsWith("s")) ? a.substring(0, a.length() - 1) : a;
    }

    private static String subjectKey(String subject) {
        if (subject == null) return "n/a";
        String s = subject.trim().toLowerCase(java.util.Locale.ROOT);
        if (s.isEmpty() || s.equals("general") || s.equals("n/a") || s.equals("na") || s.equals("none")) {
            return "n/a";
        }
        return s;
    }

    /** Resolves a student's id from a display name. Returns -1 when not found. */
    public long getStudentIdByName(String studentName) {
        try {
            return findStudentId(getReadableDatabase(), studentName);
        } catch (Exception e) {
            Log.e(TAG, "Error in getStudentIdByName", e);
            return -1;
        }
    }

    private long findGradeRecordId(SQLiteDatabase db, long studentId, String quarter, String subject, String activity) {
        String q = normalizeQuarter(quarter);
        String s = subjectKey(subject);
        String a = activityKey(activity);
        boolean isSubjectAgnostic = a.equalsIgnoreCase("homework") || a.equalsIgnoreCase("attendance") || a.toLowerCase(java.util.Locale.ROOT).contains("program");

        try (Cursor c = db.rawQuery("SELECT record_id, quarter, subject, activity FROM grade_records " +
                "WHERE student_id = ? ORDER BY record_id DESC", new String[]{String.valueOf(studentId)})) {
            while (c.moveToNext()) {
                boolean quarterMatches = normalizeQuarter(c.getString(1)).equals(q);
                boolean activityMatches = activityKey(c.getString(3)).equals(a);
                boolean subjectMatches = isSubjectAgnostic || subjectKey(c.getString(2)).equals(s);

                if (quarterMatches && activityMatches && subjectMatches) {
                    return c.getLong(0);
                }
            }
        }
        return -1;
    }

    /** record_id of the row for exactly this student + quarter + subject + activity, or -1. */
    public long findGradeRecordId(long studentId, String quarter, String subject, String activity) {
        try {
            return findGradeRecordId(getReadableDatabase(), studentId, quarter, subject, activity);
        } catch (Exception e) {
            Log.e(TAG, "Error in findGradeRecordId", e);
            return -1;
        }
    }

    /** All grade records of one student: record_id, quarter, subject, activity, score, remarks, date. */
    public Cursor getGradeRecordsForStudent(long studentId) {
        try {
            return getReadableDatabase().rawQuery(
                    "SELECT record_id, quarter, subject, activity, score, remarks, date " +
                    "FROM grade_records WHERE student_id = ? ORDER BY record_id DESC",
                    new String[]{String.valueOf(studentId)});
        } catch (Exception e) {
            Log.e(TAG, "Error in getGradeRecordsForStudent", e);
            return null;
        }
    }

    public Cursor getMatchingGradeRecords(long studentId, String quarter, String subject, String activity) {
        // Filter in Java after reading this student's rows so historical quarter labels
        // ("1", "Quarter 1", "1st Quarter") and activity/subject variants are handled consistently.
        final String[] columns = {"record_id", "quarter", "subject", "activity", "activity_name", "score", "date"};
        MatrixCursor result = new MatrixCursor(columns);
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT record_id, quarter, subject, activity, activity_name, score, date " +
                "FROM grade_records WHERE student_id = ? ORDER BY record_id DESC",
                new String[]{String.valueOf(studentId)})) {
            String wantedQuarter = normalizeQuarter(quarter);
            String wantedActivity = activityKey(activity);
            String wantedSubject = subjectKey(subject);
            boolean subjectAgnostic = wantedActivity.equals("homework") || wantedActivity.equals("attendance") || wantedActivity.equals("program performance");
            while (c.moveToNext()) {
                String rowQuarter = normalizeQuarter(c.getString(1));
                String rowActivity = activityKey(c.getString(3));
                if (!rowQuarter.equalsIgnoreCase(wantedQuarter) || !rowActivity.equalsIgnoreCase(wantedActivity)) continue;
                if (!subjectAgnostic && !subjectKey(c.getString(2)).equals(wantedSubject)) continue;
                result.addRow(new Object[]{c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6)});
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getMatchingGradeRecords", e);
            result.close();
            return null;
        }
        return result;
    }

    public String getGradeRecordScore(long studentId, String quarter, String subject, String activity) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            long id = findGradeRecordId(db, studentId, quarter, subject, activity);
            if (id < 0) return null;
            try (Cursor c = db.rawQuery("SELECT score FROM grade_records WHERE record_id = ?",
                    new String[]{String.valueOf(id)})) {
                if (c.moveToFirst()) return c.getString(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getGradeRecordScore", e);
        }
        return null;
    }

    public boolean saveOrUpdateGradeRecord(long studentId, String quarter, String subject, String activity, String score) {
        return saveOrUpdateGradeRecord(studentId, quarter, subject, activity, null, score);
    }

    public boolean saveOrUpdateGradeRecord(long studentId, String quarter, String subject, String activity, String activityName, String score) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            lastError = "";
            if (studentId < 0) {
                lastError = "No student selected.";
                return false;
            }

            String date = new java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
            String cleanQuarter = normalizeQuarter(quarter);
            String cleanSubject = subject != null ? subject.trim() : "";
            String cleanActivity = activity != null ? activity.trim() : "";

            // Program Performance permits multiple named entries for the same student/quarter/subject.
            // A generic save must therefore insert a new row rather than overwrite the latest one.
            boolean independentProgramEntry = cleanActivity.equalsIgnoreCase("Program Performance");
            long recordId = independentProgramEntry ? -1 : findGradeRecordId(db, studentId, cleanQuarter, cleanSubject, cleanActivity);

            ContentValues values = new ContentValues();
            values.put("student_id", studentId);
            values.put("subject", cleanSubject);
            values.put("quarter", cleanQuarter);
            values.put("score", score != null ? score : "0");
            values.put("date", date);
            values.put("activity", cleanActivity);
            if (activityName != null && !activityName.trim().isEmpty()) {
                values.put("activity_name", activityName.trim());
            }

            if (recordId != -1) {
                return db.update("grade_records", values, "record_id = ?", new String[]{String.valueOf(recordId)}) > 0;
            }
            return db.insertOrThrow("grade_records", null, values) != -1;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error in saveOrUpdateGradeRecord", e);
            return false;
        }
    }

    /** Name-based overload kept for older callers. */
    public boolean saveOrUpdateGradeRecord(String studentName, String quarter, String subject, String activity, String score) {
        long studentId = getStudentIdByName(studentName);
        if (studentId < 0) {
            lastError = "Student \"" + studentName + "\" not found.";
            return false;
        }
        return saveOrUpdateGradeRecord(studentId, quarter, subject, activity, score);
    }

    /** Name-based overload kept for older callers. */
    public String getGradeRecordScore(String studentName, String quarter, String subject, String activity) {
        long studentId = getStudentIdByName(studentName);
        if (studentId < 0) return null;
        return getGradeRecordScore(studentId, quarter, subject, activity);
    }

    public String getGradeRecordActivityName(long studentId, String quarter, String subject, String activity) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            long id = findGradeRecordId(db, studentId, quarter, subject, activity);
            if (id < 0) return null;
            try (Cursor c = db.rawQuery("SELECT activity_name FROM grade_records WHERE record_id = ?",
                    new String[]{String.valueOf(id)})) {
                if (c.moveToFirst()) {
                    String name = c.getString(0);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getGradeRecordActivityName", e);
        }
        return null;
    }

    /** Updates the editable fields of exactly one existing record, without searching for a similar row. */
    public boolean updateGradeRecord(long recordId, String activityName, String score) {
        try {
            ContentValues values = new ContentValues();
            values.put("activity_name", activityName != null ? activityName.trim() : "");
            values.put("score", score != null ? score : "0");
            values.put("date", new java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault()).format(new java.util.Date()));
            lastError = "";
            return getWritableDatabase().update("grade_records", values, "record_id = ?",
                    new String[]{String.valueOf(recordId)}) > 0;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error in updateGradeRecord", e);
            return false;
        }
    }

    /** Edits the score of one specific record. */
    public boolean updateGradeRecordScore(long recordId, String score) {
        try {
            ContentValues values = new ContentValues();
            values.put("score", score != null ? score : "0");
            values.put("date", new java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault()).format(new java.util.Date()));
            lastError = "";
            return getWritableDatabase().update("grade_records", values, "record_id = ?",
                    new String[]{String.valueOf(recordId)}) > 0;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error in updateGradeRecordScore", e);
            return false;
        }
    }

    /** Deletes one specific record. */
    public boolean deleteGradeRecord(long recordId) {
        try {
            return getWritableDatabase().delete("grade_records", "record_id = ?",
                    new String[]{String.valueOf(recordId)}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error in deleteGradeRecord", e);
            return false;
        }
    }
}
