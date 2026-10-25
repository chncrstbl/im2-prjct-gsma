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

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "Database.db";
    private static final int DATABASE_VERSION = 2;
    private final Context context;
    private final File dbPath;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
        this.dbPath = context.getDatabasePath(DATABASE_NAME);
        copyDatabaseIfNeeded();
    }

    private void copyDatabaseIfNeeded() {
        if (!dbPath.exists()) {
            File parentDir = dbPath.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    Log.w(TAG, "Failed to create directory: " + parentDir.getAbsolutePath());
                }
            }

            try (InputStream input = context.getAssets().open("Database.db");
                 OutputStream output = new FileOutputStream(dbPath)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        try {
            db.execSQL("ALTER TABLE students ADD COLUMN level TEXT DEFAULT 'Nursery';");
        } catch (Exception ignored) {}
    }

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
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("first_name", firstName);
        values.put("middle_name", middleName);
        values.put("last_name", lastName);
        values.put("birthdate", birthdate);
        values.put("sex", sex);
        values.put("age", age);
        values.put("guardian_name", guardianName);
        values.put("guardian_contact", guardianContact);
        values.put("address", address);
        values.put("photo_uri", photoUri);
        values.put("level", level != null ? level : "Nursery");

        long result = db.insert("students", null, values);
        return result != -1;
    }

    // CRUD: Update Student
    public boolean updateStudent(int studentId, String firstName, String middleName, String lastName, String birthdate, String sex, String age, String guardianName, String guardianContact, String address, String photoUri, String level) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("first_name", firstName);
        values.put("middle_name", middleName);
        values.put("last_name", lastName);
        values.put("birthdate", birthdate);
        values.put("sex", sex);
        values.put("age", age);
        values.put("guardian_name", guardianName);
        values.put("guardian_contact", guardianContact);
        values.put("address", address);
        values.put("photo_uri", photoUri);
        values.put("level", level != null ? level : "Nursery"); // Ina-update ang level

        int result = db.update("students", values, "student_id = ?", new String[]{String.valueOf(studentId)});
        return result > 0;
    }

    public boolean deleteStudent(int studentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete("students", "student_id = ?", new String[]{String.valueOf(studentId)});
        return result > 0;
    }

    public Cursor getAllStudents() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM students ORDER BY last_name COLLATE NOCASE ASC", null);
    }
}