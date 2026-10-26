package com.example.priangradingsystem.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

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
    private static final int DATABASE_VERSION = 3;
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
        } catch (Exception e) {
            Log.e(TAG, "Error creating database tables", e);
        }
    }

    private void createGradeRecordsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS grade_records (" +
                "record_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER, " +
                "student_name TEXT NOT NULL, " +
                "student_level TEXT, " +
                "quarter TEXT, " +
                "subject TEXT, " +
                "activity TEXT, " +
                "score TEXT, " +
                "assessment_date TEXT, " +
                "date TEXT, " +
                "remarks TEXT, " +
                "date_submitted TEXT DEFAULT CURRENT_TIMESTAMP" +
                ");");
    }

    /**
     * The bundled Database.db ships an old grade_records table (student_id NOT NULL,
     * quarter REAL, UNIQUE(student_id, subject, quarter)) that rejects the app's inserts.
     * Rebuild it with the current layout and carry over any existing rows.
     */
    private void migrateLegacyGradeRecords(SQLiteDatabase db) {
        boolean legacy = false;
        java.util.Set<String> oldCols = new java.util.HashSet<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(grade_records)", null)) {
            int n = c.getColumnIndex("name");
            int nn = c.getColumnIndex("notnull");
            while (c.moveToNext()) {
                String col = c.getString(n).toLowerCase();
                oldCols.add(col);
                if (col.equals("student_id") && c.getInt(nn) == 1) legacy = true;
            }
        }
        if (!legacy && oldCols.contains("student_name")) return;

        try {
            db.execSQL("ALTER TABLE grade_records RENAME TO grade_records_old");
            db.execSQL("DROP INDEX IF EXISTS idx_grades_student");
            createGradeRecordsTable(db);

            String[] copyCols = {"student_id", "student_name", "student_level", "quarter", "subject",
                    "activity", "score", "assessment_date", "date", "remarks"};
            StringBuilder target = new StringBuilder();
            StringBuilder source = new StringBuilder();
            for (String col : copyCols) {
                if (oldCols.contains(col)) {
                    if (target.length() > 0) { target.append(", "); source.append(", "); }
                    target.append(col);
                    source.append("o.").append(col);
                }
            }
            if (!oldCols.contains("student_name") && oldCols.contains("student_id")) {
                target.append(", student_name");
                source.append(", COALESCE((SELECT s.last_name || ', ' || s.first_name FROM students s " +
                        "WHERE s.student_id = o.student_id), 'Unknown')");
            }
            if (target.length() > 0) {
                db.execSQL("INSERT INTO grade_records (" + target + ") SELECT " + source +
                        " FROM grade_records_old o");
            }
            db.execSQL("DROP TABLE grade_records_old");
            Log.d(TAG, "Migrated legacy grade_records table.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to migrate grade_records", e);
        }
    }

    private void ensureTablesAndColumns(SQLiteDatabase db) {
        try {
            createTables(db);
            migrateLegacyGradeRecords(db);

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

    public boolean insertGradeRecord(String studentName, String studentLevel, String quarter, String subject, String activity, String score, String assessmentDate, String remarks) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("student_name", studentName != null ? studentName : "");
            values.put("student_level", studentLevel != null ? studentLevel : "");
            values.put("quarter", quarter != null ? quarter : "1st Quarter");
            values.put("subject", subject != null ? subject : "");
            values.put("activity", activity != null ? activity : "");
            values.put("score", score != null ? score : "");
            values.put("assessment_date", assessmentDate != null ? assessmentDate : "");
            values.put("remarks", remarks != null ? remarks : "");

            lastError = "";
            long result = db.insertOrThrow("grade_records", null, values);
            return result != -1;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error in insertGradeRecord", e);
            return false;
        }
    }

    public Cursor getAllGradeRecords() {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            return db.rawQuery("SELECT * FROM grade_records ORDER BY record_id DESC", null);
        } catch (Exception e) {
            Log.e(TAG, "Error in getAllGradeRecords", e);
            return null;
        }
    }
}