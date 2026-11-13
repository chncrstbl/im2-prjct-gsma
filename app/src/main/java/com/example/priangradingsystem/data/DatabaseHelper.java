package com.example.priangradingsystem.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.priangradingsystem.model.Teacher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * All database access. Layout and migrations live in {@link DbSchema}.
 *
 * School years: students are enrolled per school year ({@code enrollments}) and every grade record
 * carries a {@code school_year_id}. Every read and write below works on the SELECTED school year
 * ({@link SchoolYearContext}, default = the current one), so a score or report can never mix years.
 */
@SuppressWarnings("all")
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "Database.db";
    private static final int DATABASE_VERSION = DbSchema.VERSION;

    // Only used to seed a brand-new database that has no teacher at all (the bundled Database.db has one).
    private static final String SEED_TEACHER_NAME = "Marian F. Ceron";
    private static final String SEED_TEACHER_USER = "Prian";
    private static final String SEED_TEACHER_PASS = "Ham20456";

    private final Context context;
    private final File dbPath;
    private String lastError = "";

    public String getLastError() { return lastError; }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
        this.dbPath = context.getDatabasePath(DATABASE_NAME);
        copyDatabaseIfNeeded();
        backupBeforeUpgrade();
    }

    // ------------------------------------------------------------------------------------------
    // File handling, creation, migration
    // ------------------------------------------------------------------------------------------

    private void copyDatabaseIfNeeded() {
        if (dbPath.exists() && dbPath.length() > 0) return;
        File parentDir = dbPath.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            Log.w(TAG, "Failed to create directory: " + parentDir.getAbsolutePath());
        }
        File tmp = new File(dbPath.getPath() + ".tmp");
        try (InputStream input = context.getAssets().open(DATABASE_NAME);
             OutputStream output = new FileOutputStream(tmp)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = input.read(buffer)) > 0) output.write(buffer, 0, n);
            output.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error copying database from assets", e);
            tmp.delete();
            return;
        }
        if (dbPath.exists()) dbPath.delete();
        if (!tmp.renameTo(dbPath)) Log.e(TAG, "Could not move the copied database into place");
    }

    /** Before an older database is converted, keep a copy of the original files next to it (best effort). */
    private void backupBeforeUpgrade() {
        try {
            if (!dbPath.exists() || dbPath.length() == 0) return;
            int version;
            SQLiteDatabase peek = SQLiteDatabase.openDatabase(dbPath.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            try { version = peek.getVersion(); } finally { peek.close(); }
            if (version <= 0 || version >= DATABASE_VERSION) return;
            for (String suffix : new String[]{"", "-wal", "-shm"}) {
                File src = new File(dbPath.getPath() + suffix);
                File dst = new File(dbPath.getPath() + ".v" + version + ".bak" + suffix);
                if (src.exists() && !dst.exists()) copyFile(src, dst);
            }
            Log.i(TAG, "Backed up database version " + version + " before upgrading");
        } catch (Throwable t) {
            Log.w(TAG, "Could not back up the database before upgrading", t);
        }
    }

    private static void copyFile(File from, File to) throws IOException {
        try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        migrate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        migrate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Not during migration (it rebuilds tables); from here on deletes cascade properly.
        db.setForeignKeyConstraintsEnabled(true);
    }

    private void migrate(SQLiteDatabase db) {
        DbSchema.upgrade(new DbExec(db), Calendar.getInstance());
        upgradeLegacyPasswords(db);
        seedTeacherIfNone(db);
    }

    private void upgradeLegacyPasswords(SQLiteDatabase db) {
        List<String[]> rows = new DbExec(db).query("SELECT teacher_id, password_hash FROM teachers");
        for (String[] r : rows) {
            if (!PasswordHasher.isHashed(r[1])) {
                db.execSQL("UPDATE teachers SET password_hash = ? WHERE teacher_id = ?",
                        new Object[]{PasswordHasher.hash(r[1] == null ? "" : r[1]), Long.parseLong(r[0])});
            }
        }
    }

    private void seedTeacherIfNone(SQLiteDatabase db) {
        List<String[]> n = new DbExec(db).query("SELECT COUNT(*) FROM teachers");
        if (!n.isEmpty() && "0".equals(n.get(0)[0])) {
            db.execSQL("INSERT INTO teachers (full_name, username, password_hash) VALUES (?, ?, ?)",
                    new Object[]{SEED_TEACHER_NAME, SEED_TEACHER_USER, PasswordHasher.hash(SEED_TEACHER_PASS)});
        }
    }

    /** Lets {@link DbSchema} run on a real SQLiteDatabase. */
    private static final class DbExec implements DbSchema.SqlExec {
        private final SQLiteDatabase db;
        DbExec(SQLiteDatabase db) { this.db = db; }

        @Override public void exec(String sql, Object... args) {
            if (args == null || args.length == 0) db.execSQL(sql); else db.execSQL(sql, args);
        }

        @Override public List<String[]> query(String sql, String... args) {
            List<String[]> rows = new ArrayList<>();
            try (Cursor c = db.rawQuery(sql, (args == null || args.length == 0) ? null : args)) {
                while (c.moveToNext()) {
                    String[] row = new String[c.getColumnCount()];
                    for (int i = 0; i < row.length; i++) row[i] = c.getString(i);
                    rows.add(row);
                }
            }
            return rows;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Teachers
    // ------------------------------------------------------------------------------------------

    public boolean isEmailExists(String email) {
        if (email == null) return false;
        try {
            SQLiteDatabase db = getWritableDatabase();
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM teachers WHERE LOWER(email) = LOWER(?)", new String[]{email.trim()})) {
                if (c.moveToFirst()) {
                    return c.getInt(0) > 0;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking email existence", e);
        }
        return false;
    }

    public boolean isUsernameExists(String username) {
        if (username == null) return false;
        try {
            SQLiteDatabase db = getWritableDatabase();
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM teachers WHERE LOWER(username) = LOWER(?)", new String[]{username.trim()})) {
                if (c.moveToFirst()) {
                    return c.getInt(0) > 0;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking username existence", e);
        }
        return false;
    }

    public boolean verifyPresetCode(String username, String presetCode) {
        if (username == null || presetCode == null) return false;
        try {
            SQLiteDatabase db = getWritableDatabase();
            try (Cursor c = db.rawQuery("SELECT preset_code FROM teachers WHERE LOWER(username) = LOWER(?)", new String[]{username.trim()})) {
                if (c.moveToFirst()) {
                    String storedCode = c.getString(0);
                    return storedCode != null && storedCode.trim().equalsIgnoreCase(presetCode.trim());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verifying preset code", e);
        }
        return false;
    }

    public boolean insertTeacher(String fullName, String username, String password, String email, String presetCode) {
        if (username == null || password == null || fullName == null) return false;
        try {
            SQLiteDatabase db = getWritableDatabase();
            String passwordHash = PasswordHasher.hash(password);
            ContentValues values = new ContentValues();
            values.put("full_name", fullName.trim());
            values.put("username", username.trim());
            values.put("password_hash", passwordHash);
            values.put("email", email != null ? email.trim() : null);
            values.put("preset_code", presetCode != null ? presetCode.trim() : null);
            long id = db.insert("teachers", null, values);
            return id != -1;
        } catch (Exception e) {
            Log.e(TAG, "Error inserting teacher", e);
            return false;
        }
    }

    public boolean insertTeacher(String fullName, String username, String password) {
        return insertTeacher(fullName, username, password, null, null);
    }

    public boolean insertTeacher(String fullName, String username, String password, String email) {
        return insertTeacher(fullName, username, password, email, null);
    }

    public boolean updateTeacherPassword(String username, String newPassword) {
        if (username == null || newPassword == null) return false;
        try {
            SQLiteDatabase db = getWritableDatabase();
            String hashed = PasswordHasher.hash(newPassword);
            ContentValues values = new ContentValues();
            values.put("password_hash", hashed);
            int rows = db.update("teachers", values, "LOWER(username) = LOWER(?)", new String[]{username.trim()});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating teacher password", e);
            return false;
        }
    }

    public Teacher getTeacherByCredentials(String username, String password) {
        if (username == null || password == null) return null;
        try {
            SQLiteDatabase db = getWritableDatabase();
            try (Cursor c = db.rawQuery("SELECT teacher_id, full_name, username, password_hash, email, preset_code FROM teachers " +
                    "WHERE LOWER(username) = LOWER(?)", new String[]{username.trim()})) {
                while (c.moveToNext()) {
                    String stored = c.getString(3);
                    if (!PasswordHasher.verify(password, stored)) continue;
                    int id = c.getInt(0);
                    if (!PasswordHasher.isHashed(stored)) {
                        // Old plain-text password: store it hashed from now on.
                        stored = PasswordHasher.hash(password);
                        db.execSQL("UPDATE teachers SET password_hash = ? WHERE teacher_id = ?",
                                new Object[]{stored, (long) id});
                    }
                    return new Teacher(id, c.getString(1), c.getString(2), stored, c.getString(4), c.getString(5));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching teacher by credentials", e);
        }
        return null;
    }

    public Teacher getTeacherById(int teacherId) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            try (Cursor c = db.rawQuery("SELECT teacher_id, full_name, username, password_hash, email, preset_code FROM teachers " +
                    "WHERE teacher_id = ?", new String[]{String.valueOf(teacherId)})) {
                if (c.moveToFirst()) {
                    return new Teacher(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching teacher by id", e);
        }
        return null;
    }

    public Teacher getTeacherByUsername(String username) {
        if (username == null) return null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            try (Cursor c = db.rawQuery("SELECT teacher_id, full_name, username, password_hash, email, preset_code FROM teachers " +
                    "WHERE LOWER(username) = LOWER(?)", new String[]{username.trim()})) {
                if (c.moveToFirst()) {
                    return new Teacher(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching teacher by username", e);
        }
        return null;
    }

    public boolean updateTeacherProfile(int teacherId, String fullName, String email) {
        if (fullName == null) return false;
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("full_name", fullName.trim());
            values.put("email", email != null ? email.trim() : "");
            int rows = db.update("teachers", values, "teacher_id = ?", new String[]{String.valueOf(teacherId)});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating teacher profile", e);
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------
    // School years
    // ------------------------------------------------------------------------------------------

    public enum CarryOver { SAME_LEVEL, PROMOTE, NONE }

    private SchoolYear readYear(SQLiteDatabase db, String where, String... args) {
        try (Cursor c = db.rawQuery("SELECT school_year_id, label, is_current FROM school_years WHERE " + where
                + " ORDER BY label DESC LIMIT 1", args)) {
            return c.moveToFirst() ? new SchoolYear(c.getLong(0), c.getString(1), c.getInt(2) == 1) : null;
        }
    }

    /** The school year being shown: the selected one, otherwise the current one. Never null. */
    public SchoolYear getSelectedSchoolYear() {
        SQLiteDatabase db = getWritableDatabase();
        long selected = SchoolYearContext.getSelectedId();
        SchoolYear y = selected > 0 ? readYear(db, "school_year_id = ?", String.valueOf(selected)) : null;
        if (y == null) y = readYear(db, "is_current = 1");
        if (y == null) {
            // Repair: a database without any school year gets the one for today.
            DbSchema.upgrade(new DbExec(db), Calendar.getInstance());
            y = readYear(db, "is_current = 1");
        }
        return y;
    }

    public long getSelectedSchoolYearId() { return getSelectedSchoolYear().id; }

    public SchoolYear getCurrentSchoolYear() {
        SchoolYear y = readYear(getWritableDatabase(), "is_current = 1");
        return y != null ? y : getSelectedSchoolYear();
    }

    public SchoolYear getSchoolYear(long id) {
        return readYear(getReadableDatabase(), "school_year_id = ?", String.valueOf(id));
    }

    /** All school years, newest first. */
    public List<SchoolYear> getSchoolYears() {
        List<SchoolYear> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT school_year_id, label, is_current FROM school_years ORDER BY label DESC", null)) {
            while (c.moveToNext()) out.add(new SchoolYear(c.getLong(0), c.getString(1), c.getInt(2) == 1));
        }
        return out;
    }

    /** Makes every screen show this school year. Returns false when it does not exist. */
    public boolean selectSchoolYear(long id) {
        if (getSchoolYear(id) == null) return false;
        SchoolYearContext.setSelectedId(id);
        return true;
    }

    /** Creates a school year ("2026-2027"). Returns its id, or -1 (see {@link #getLastError()}). */
    public long createSchoolYear(String rawLabel, boolean makeCurrent) {
        lastError = "";
        String label = SchoolYear.normalizeLabel(rawLabel);
        if (!SchoolYear.isValidLabel(label)) {
            lastError = "Use two consecutive years, for example 2027-2028.";
            return -1;
        }
        try {
            SQLiteDatabase db = getWritableDatabase();
            if (readYear(db, "label = ?", label) != null) {
                lastError = "School year " + label + " already exists.";
                return -1;
            }
            ContentValues v = new ContentValues();
            v.put("label", label);
            v.put("is_current", 0);
            long id = db.insertOrThrow("school_years", null, v);
            if (makeCurrent) setCurrentSchoolYear(id);
            return id;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error in createSchoolYear", e);
            return -1;
        }
    }

    public boolean setCurrentSchoolYear(long id) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            if (readYear(db, "school_year_id = ?", String.valueOf(id)) == null) return false;
            db.execSQL("UPDATE school_years SET is_current = CASE WHEN school_year_id = ? THEN 1 ELSE 0 END",
                    new Object[]{id});
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in setCurrentSchoolYear", e);
            return false;
        }
    }

    /** Level a student moves to when promoted. Nursery is the top level here, so it has none (null). */
    static String promotedLevel(String level) {
        String l = level == null ? "" : level.trim();
        if (l.equalsIgnoreCase("Toddler") || l.equalsIgnoreCase("Pre-K")) return "Nursery";
        if (l.equalsIgnoreCase("Nursery")) return null;
        return l.isEmpty() ? "Nursery" : l;
    }

    /**
     * Enrolls the students of one school year in another. Students already enrolled in the target are
     * skipped. Grades are never copied. Returns how many students were enrolled.
     */
    public int carryOverStudents(long fromYearId, long toYearId, CarryOver mode) {
        if (mode == CarryOver.NONE || fromYearId == toYearId) return 0;
        int count = 0;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            List<String[]> rows = new DbExec(db).query(
                    "SELECT student_id, level FROM enrollments WHERE school_year_id = ?", String.valueOf(fromYearId));
            for (String[] r : rows) {
                String level = mode == CarryOver.PROMOTE ? promotedLevel(r[1]) : r[1];
                if (level == null) continue;
                db.execSQL("INSERT OR IGNORE INTO enrollments (student_id, school_year_id, level) VALUES (?, ?, ?)",
                        new Object[]{Long.parseLong(r[0]), toYearId, level});
                try (Cursor c = db.rawQuery("SELECT changes()", null)) {
                    if (c.moveToFirst()) count += c.getInt(0);
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error in carryOverStudents", e);
            count = 0;
        } finally {
            db.endTransaction();
        }
        return count;
    }

    // ------------------------------------------------------------------------------------------
    // Students
    // ------------------------------------------------------------------------------------------

    /** Age in whole years, computed from the birthdate (so it is right in every school year). */
    private static final String AGE_SQL =
            "MAX(0, CASE " +
            "WHEN s.birthdate GLOB '[0-9][0-9]/[0-9][0-9]/[0-9][0-9][0-9][0-9]' THEN " +
            "CAST(strftime('%Y','now','localtime') AS INTEGER) - CAST(substr(s.birthdate,7,4) AS INTEGER) " +
            "- (strftime('%m%d','now','localtime') < substr(s.birthdate,1,2) || substr(s.birthdate,4,2)) " +
            "WHEN s.birthdate GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]' THEN " +
            "CAST(strftime('%Y','now','localtime') AS INTEGER) - CAST(substr(s.birthdate,1,4) AS INTEGER) " +
            "- (strftime('%m%d','now','localtime') < substr(s.birthdate,6,2) || substr(s.birthdate,9,2)) " +
            "ELSE NULL END)";

    /** Guardian's full name from its parts, without stray spaces. */
    private static final String GUARDIAN_SQL =
            "TRIM(TRIM(s.guardian_first_name || CASE WHEN s.guardian_middle_name <> '' THEN ' ' || s.guardian_middle_name ELSE '' END) " +
            "|| CASE WHEN s.guardian_last_name <> '' THEN ' ' || s.guardian_last_name ELSE '' END " +
            "|| CASE WHEN s.guardian_extension <> '' THEN ' ' || s.guardian_extension ELSE '' END)";

    private static final String STUDENT_SELECT =
            "SELECT s.student_id, s.first_name, s.middle_name, s.last_name, s.name_extension, s.birthdate, s.sex, " +
            "s.guardian_first_name, s.guardian_middle_name, s.guardian_last_name, s.guardian_extension, " +
            "s.guardian_contact, s.address, s.photo_uri, " +
            "e.enrollment_id, e.level, e.school_year_id, y.label AS school_year, " + GUARDIAN_SQL + " AS guardian_name, " +
            AGE_SQL + " AS age " +
            "FROM enrollments e " +
            "JOIN students s ON s.student_id = e.student_id " +
            "JOIN school_years y ON y.school_year_id = e.school_year_id ";

    /** Students enrolled in the selected school year. */
    public Cursor getAllStudents() {
        try {
            return getStudentsForYear(getSelectedSchoolYearId());
        } catch (Exception e) {
            Log.e(TAG, "Error in getAllStudents", e);
            return null;
        }
    }

    public Cursor getStudentsForYear(long yearId) {
        return getReadableDatabase().rawQuery(STUDENT_SELECT + "WHERE e.school_year_id = ? " +
                "ORDER BY s.last_name COLLATE NOCASE ASC, s.first_name COLLATE NOCASE ASC",
                new String[]{String.valueOf(yearId)});
    }

    public int getStudentCount(long yearId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM enrollments WHERE school_year_id = ?", new String[]{String.valueOf(yearId)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String nz(String s) { return s == null ? "" : s.trim(); }

    private static ContentValues studentValues(String first, String middle, String last, String ext, String birthdate,
                                               String sex, String gFirst, String gMiddle, String gLast, String gExt,
                                               String gContact, String address, String photoUri) {
        ContentValues v = new ContentValues();
        v.put("first_name", nz(first));
        v.put("middle_name", nz(middle));
        v.put("last_name", nz(last));
        v.put("name_extension", nz(ext));
        v.put("birthdate", nz(birthdate));
        v.put("sex", nz(sex));
        v.put("guardian_first_name", nz(gFirst));
        v.put("guardian_middle_name", nz(gMiddle));
        v.put("guardian_last_name", nz(gLast));
        v.put("guardian_extension", nz(gExt));
        v.put("guardian_contact", nz(gContact));
        v.put("address", nz(address));
        v.put("photo_uri", nz(photoUri));
        return v;
    }

    private static String cleanLevel(String level) {
        String l = nz(level);
        return l.isEmpty() ? "Nursery" : l;
    }

    /** Adds a student and enrolls them in {@code schoolYearId}. Returns the new student id, or -1. */
    public long insertStudent(String first, String middle, String last, String ext, String birthdate, String sex,
                              String gFirst, String gMiddle, String gLast, String gExt, String gContact,
                              String address, String photoUri, String level, long schoolYearId) {
        lastError = "";
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long id = db.insertOrThrow("students", null, studentValues(first, middle, last, ext, birthdate, sex,
                    gFirst, gMiddle, gLast, gExt, gContact, address, photoUri));
            ContentValues e = new ContentValues();
            e.put("student_id", id);
            e.put("school_year_id", schoolYearId);
            e.put("level", cleanLevel(level));
            db.insertOrThrow("enrollments", null, e);
            db.setTransactionSuccessful();
            return id;
        } catch (Exception ex) {
            lastError = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            Log.e(TAG, "Error in insertStudent", ex);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    /** Updates the student, and the level of their enrollment in {@code schoolYearId}. */
    public boolean updateStudent(long studentId, String first, String middle, String last, String ext, String birthdate,
                                 String sex, String gFirst, String gMiddle, String gLast, String gExt,
                                 String gContact, String address, String photoUri, String level, long schoolYearId) {
        lastError = "";
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int n = db.update("students", studentValues(first, middle, last, ext, birthdate, sex, gFirst, gMiddle,
                    gLast, gExt, gContact, address, photoUri), "student_id = ?", new String[]{String.valueOf(studentId)});
            if (n <= 0) {
                lastError = "No student found with id " + studentId;
                return false;
            }
            ContentValues e = new ContentValues();
            e.put("level", cleanLevel(level));
            int m = db.update("enrollments", e, "student_id = ? AND school_year_id = ?",
                    new String[]{String.valueOf(studentId), String.valueOf(schoolYearId)});
            if (m == 0) {
                e.put("student_id", studentId);
                e.put("school_year_id", schoolYearId);
                db.insertOrThrow("enrollments", null, e);
            }
            db.setTransactionSuccessful();
            return true;
        } catch (Exception ex) {
            lastError = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            Log.e(TAG, "Error in updateStudent", ex);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /** In how many school years this student is enrolled. */
    public int getEnrollmentCount(long studentId) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM enrollments WHERE student_id = ?",
                new String[]{String.valueOf(studentId)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isEnrolled(long studentId, long yearId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM enrollments WHERE student_id = ? AND school_year_id = ?",
                new String[]{String.valueOf(studentId), String.valueOf(yearId)})) {
            return c.moveToFirst();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean enrollStudent(long studentId, long yearId, String level) {
        lastError = "";
        try {
            ContentValues e = new ContentValues();
            e.put("student_id", studentId);
            e.put("school_year_id", yearId);
            e.put("level", cleanLevel(level));
            return getWritableDatabase().insertOrThrow("enrollments", null, e) != -1;
        } catch (Exception ex) {
            lastError = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            Log.e(TAG, "Error in enrollStudent", ex);
            return false;
        }
    }

    /** Id of a student with the same name and birthdate (any school year), or -1. Used to avoid duplicates. */
    public long findStudentByIdentity(String first, String last, String birthdate) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT student_id FROM students WHERE LOWER(TRIM(first_name)) = LOWER(?) " +
                "AND LOWER(TRIM(last_name)) = LOWER(?) AND TRIM(COALESCE(birthdate, '')) = ? " +
                "ORDER BY student_id LIMIT 1", new String[]{nz(first), nz(last), nz(birthdate)})) {
            return c.moveToFirst() ? c.getLong(0) : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /** Removes the student from one school year, including that year's grades. Other years are kept. */
    public boolean unenrollStudent(long studentId, long yearId) {
        lastError = "";
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("grade_records", "student_id = ? AND school_year_id = ?",
                    new String[]{String.valueOf(studentId), String.valueOf(yearId)});
            int n = db.delete("enrollments", "student_id = ? AND school_year_id = ?",
                    new String[]{String.valueOf(studentId), String.valueOf(yearId)});
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM enrollments WHERE student_id = ?",
                    new String[]{String.valueOf(studentId)})) {
                if (c.moveToFirst() && c.getInt(0) == 0) {
                    // Nowhere left to appear: remove the person too, so nobody is invisible.
                    db.delete("grade_records", "student_id = ?", new String[]{String.valueOf(studentId)});
                    db.delete("students", "student_id = ?", new String[]{String.valueOf(studentId)});
                }
            }
            db.setTransactionSuccessful();
            return n > 0;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error in unenrollStudent", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /** Permanently deletes the student with all enrollments and grades of every school year. */
    public boolean deleteStudent(long studentId) {
        lastError = "";
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("grade_records", "student_id = ?", new String[]{String.valueOf(studentId)});
            db.delete("enrollments", "student_id = ?", new String[]{String.valueOf(studentId)});
            int n = db.delete("students", "student_id = ?", new String[]{String.valueOf(studentId)});
            db.setTransactionSuccessful();
            return n > 0;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error in deleteStudent", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public String getStudentSex(long studentId) {
        if (studentId < 1) return "";
        try (Cursor c = getReadableDatabase().rawQuery("SELECT sex FROM students WHERE student_id = ?",
                new String[]{String.valueOf(studentId)})) {
            return c.moveToFirst() && c.getString(0) != null ? c.getString(0) : "";
        } catch (Exception e) {
            Log.e(TAG, "Error in getStudentSex", e);
            return "";
        }
    }

    /** Looks a student up by display name among those enrolled in the selected school year. */
    public long getStudentIdByName(String studentName) {
        if (studentName == null || studentName.trim().isEmpty()) return -1;
        String name = studentName.trim();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT s.student_id FROM students s JOIN enrollments e ON e.student_id = s.student_id " +
                "WHERE e.school_year_id = ? AND (" +
                "LOWER(s.last_name || ', ' || s.first_name) = LOWER(?) OR " +
                "LOWER(s.first_name || ' ' || s.last_name) = LOWER(?) OR " +
                "LOWER(s.first_name || ' ' || COALESCE(s.middle_name, '') || ' ' || s.last_name) = LOWER(?)) " +
                "ORDER BY s.student_id LIMIT 1",
                new String[]{String.valueOf(getSelectedSchoolYearId()), name, name, name})) {
            return c.moveToFirst() ? c.getLong(0) : -1;
        } catch (Exception e) {
            Log.e(TAG, "Error in getStudentIdByName", e);
            return -1;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Grade records (always the selected school year)
    // ------------------------------------------------------------------------------------------

    /** Canonical quarter label: "1st Quarter", "2nd Quarter", "3rd Quarter", "4th Quarter". */
    public static String normalizeQuarter(String quarter) {
        if (quarter == null) return "1st Quarter";
        String q = quarter.trim();
        Matcher m = Pattern.compile("(\\d+)").matcher(q);
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
        String a = activity == null ? "" : activity.trim().toLowerCase(Locale.ROOT);
        return (a.length() > 3 && a.endsWith("s")) ? a.substring(0, a.length() - 1) : a; // "Books" == "Book"
    }

    private static String subjectKey(String subject) {
        if (subject == null) return "n/a";
        String s = subject.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty() || s.equals("general") || s.equals("n/a") || s.equals("na") || s.equals("none")) return "n/a";
        return s;
    }

    private static String today() {
        return new java.text.SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).format(new java.util.Date());
    }

    private static String now() {
        return new java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(new java.util.Date());
    }

    /** Every grade record of the selected school year (all students), newest first. */
    public Cursor getAllGradeRecords() {
        try {
            return getReadableDatabase().rawQuery(
                    "SELECT g.record_id, g.student_id, g.subject, g.quarter, g.score, g.remarks, g.date, g.time, " +
                    "g.activity, g.activity_name, g.date AS assessment_date, " +
                    "s.last_name || ', ' || s.first_name AS student_name, e.level AS student_level " +
                    "FROM grade_records g " +
                    "JOIN enrollments e ON e.student_id = g.student_id AND e.school_year_id = g.school_year_id " +
                    "JOIN students s ON s.student_id = g.student_id " +
                    "WHERE g.school_year_id = ? ORDER BY g.record_id DESC",
                    new String[]{String.valueOf(getSelectedSchoolYearId())});
        } catch (Exception e) {
            Log.e(TAG, "Error in getAllGradeRecords", e);
            return null;
        }
    }

    /** One student's records in the selected school year: record_id, quarter, subject, activity, score, remarks, date. */
    public Cursor getGradeRecordsForStudent(long studentId) {
        try {
            return getReadableDatabase().rawQuery(
                    "SELECT record_id, quarter, subject, activity, score, remarks, date FROM grade_records " +
                    "WHERE student_id = ? AND school_year_id = ? ORDER BY record_id DESC",
                    new String[]{String.valueOf(studentId), String.valueOf(getSelectedSchoolYearId())});
        } catch (Exception e) {
            Log.e(TAG, "Error in getGradeRecordsForStudent", e);
            return null;
        }
    }

    public Cursor getMatchingGradeRecords(long studentId, String quarter, String subject, String activity) {
        // Filtered in Java so old label variants ("1", "Quarter 1", "1st Quarter") and activity/subject
        // spellings are matched the same way everywhere.
        final String[] columns = {"record_id", "quarter", "subject", "activity", "activity_name", "score", "date"};
        MatrixCursor result = new MatrixCursor(columns);
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT record_id, quarter, subject, activity, activity_name, score, date FROM grade_records " +
                "WHERE student_id = ? AND school_year_id = ? ORDER BY record_id DESC",
                new String[]{String.valueOf(studentId), String.valueOf(getSelectedSchoolYearId())})) {
            String wantedQuarter = normalizeQuarter(quarter);
            String wantedActivity = activityKey(activity);
            String wantedSubject = subjectKey(subject);
            boolean subjectAgnostic = wantedActivity.equals("homework") || wantedActivity.equals("attendance")
                    || wantedActivity.equals("program performance");
            while (c.moveToNext()) {
                if (!normalizeQuarter(c.getString(1)).equalsIgnoreCase(wantedQuarter)
                        || !activityKey(c.getString(3)).equalsIgnoreCase(wantedActivity)) continue;
                if (!subjectAgnostic && !subjectKey(c.getString(2)).equals(wantedSubject)) continue;
                result.addRow(new Object[]{c.getLong(0), c.getString(1), c.getString(2), c.getString(3),
                        c.getString(4), c.getString(5), c.getString(6)});
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getMatchingGradeRecords", e);
            result.close();
            return null;
        }
        return result;
    }

    public int getPresentAttendanceCount(long studentId, String quarter) {
        if (studentId < 1 || quarter == null) return 0;
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM grade_records WHERE student_id = ? AND school_year_id = ? " +
                "AND LOWER(TRIM(activity)) = 'attendance' AND LOWER(TRIM(score)) = 'present' " +
                "AND LOWER(TRIM(quarter)) = LOWER(TRIM(?))",
                new String[]{String.valueOf(studentId), String.valueOf(getSelectedSchoolYearId()),
                        normalizeQuarter(quarter)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Exception e) {
            Log.e(TAG, "Could not count present attendance entries", e);
            return 0;
        }
    }

    /** Attendance rows (present and absent) of one student and quarter: record_id, date, time, score. */
    public Cursor getAttendanceRecords(long studentId, String quarter) {
        if (studentId < 1 || quarter == null) return null;
        try {
            return getReadableDatabase().rawQuery(
                    "SELECT record_id, date, time, score FROM grade_records WHERE student_id = ? AND school_year_id = ? " +
                    "AND LOWER(TRIM(activity)) = 'attendance' AND LOWER(TRIM(quarter)) = LOWER(TRIM(?)) " +
                    "ORDER BY date DESC, time DESC, record_id DESC",
                    new String[]{String.valueOf(studentId), String.valueOf(getSelectedSchoolYearId()),
                            normalizeQuarter(quarter)});
        } catch (Exception e) {
            Log.e(TAG, "Error in getAttendanceRecords", e);
            return null;
        }
    }

    /** Inserts one row for the selected school year after checking the student is enrolled in it. */
    private boolean insertRow(long studentId, String quarter, String subject, String activity, String activityName,
                              String score, String date, String remarks) {
        lastError = "";
        try {
            SQLiteDatabase db = getWritableDatabase();
            SchoolYear year = getSelectedSchoolYear();
            if (studentId <= 0) {
                lastError = "No student selected.";
                return false;
            }
            if (!isEnrolled(studentId, year.id)) {
                lastError = "This student is not enrolled in school year " + year.label + ".";
                return false;
            }
            ContentValues v = new ContentValues();
            v.put("student_id", studentId);
            v.put("school_year_id", year.id);
            v.put("subject", subject != null && !subject.trim().isEmpty() ? subject.trim() : "N/A");
            v.put("quarter", normalizeQuarter(quarter));
            v.put("activity", activity != null ? activity.trim() : "");
            v.put("activity_name", activityName);
            v.put("score", score != null ? score : "");
            v.put("remarks", remarks != null ? remarks : "");
            v.put("date", date != null && !date.trim().isEmpty() ? date : today());
            v.put("time", now());
            return db.insertOrThrow("grade_records", null, v) != -1;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error inserting grade record", e);
            return false;
        }
    }

    public boolean saveAttendanceStatus(long studentId, String date, String status, String quarter,
                                        String subject, String remarks) {
        if (date == null || date.trim().isEmpty()) return false;
        return insertRow(studentId, quarter, subject != null ? subject : "Attendance", "Attendance", null,
                status != null ? status : "", date, remarks);
    }

    /**
     * Saves a score for a student. When {@code activityName} is blank a numbered name is generated
     * (book1, book2, homework1, exam1 ...), counting only this student's rows of the same school year,
     * quarter, subject and activity.
     */
    public boolean addGradeRecord(long studentId, String quarter, String subject, String activity,
                                  String activityName, String score, String date, String remarks) {
        String name = activityName != null ? activityName.trim() : "";
        if (name.isEmpty()) {
            if (activity != null && activity.equalsIgnoreCase("Attendance")) {
                name = "N/A";
            } else {
                name = getNextActivityName(getWritableDatabase(), studentId, getSelectedSchoolYearId(),
                        subject, quarter, activity);
            }
        }
        return insertRow(studentId, quarter, subject, activity, name, score, date, remarks);
    }

    /**
     * Saves one score for a student chosen by name (older call). The student must already be enrolled in the
     * selected school year; this never creates a student.
     */
    public boolean insertGradeRecord(String studentName, String quarter, String subject, String activity,
                                     String activityName, String score, String assessmentDate, String remarks) {
        long id = getStudentIdByName(studentName);
        if (id <= 0) {
            lastError = "Student not found in school year " + getSelectedSchoolYear().label + ".";
            return false;
        }
        return addGradeRecord(id, quarter, subject, activity, activityName, score, assessmentDate, remarks);
    }

    /** Inserts an independent grade entry (used for new Program Performance entries). */
    public boolean insertGradeRecordForStudent(long studentId, String quarter, String subject, String activity,
                                               String activityName, String score) {
        return insertRow(studentId, quarter, subject != null ? subject.trim() : "N/A", activity,
                activityName != null ? activityName.trim() : "", score != null ? score : "0", null, null);
    }

    private String getNextActivityName(SQLiteDatabase db, long studentId, long yearId, String subject,
                                       String quarter, String activity) {
        int maxNum = 0;
        String prefix = "item";
        if (activity != null) {
            if (activity.equalsIgnoreCase("Exam")) prefix = "exam";
            else if (activity.equalsIgnoreCase("Homework")) prefix = "homework";
            else if (activity.equalsIgnoreCase("Books")) prefix = "book";
            else prefix = activity.toLowerCase(Locale.ROOT);
        }
        try (Cursor c = db.rawQuery("SELECT activity_name FROM grade_records WHERE student_id = ? " +
                "AND school_year_id = ? AND subject = ? AND quarter = ? AND activity = ? AND activity_name LIKE ?",
                new String[]{String.valueOf(studentId), String.valueOf(yearId), subject != null ? subject : "",
                        normalizeQuarter(quarter), activity != null ? activity : "", prefix + "%"})) {
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

    private long findGradeRecordId(SQLiteDatabase db, long studentId, String quarter, String subject, String activity) {
        String q = normalizeQuarter(quarter);
        String s = subjectKey(subject);
        String a = activityKey(activity);
        boolean subjectAgnostic = a.equals("homework") || a.equals("attendance") || a.contains("program");
        try (Cursor c = db.rawQuery("SELECT record_id, quarter, subject, activity FROM grade_records " +
                "WHERE student_id = ? AND school_year_id = ? ORDER BY record_id DESC",
                new String[]{String.valueOf(studentId), String.valueOf(getSelectedSchoolYearId())})) {
            while (c.moveToNext()) {
                if (normalizeQuarter(c.getString(1)).equals(q) && activityKey(c.getString(3)).equals(a)
                        && (subjectAgnostic || subjectKey(c.getString(2)).equals(s))) {
                    return c.getLong(0);
                }
            }
        }
        return -1;
    }

    public boolean saveOrUpdateGradeRecord(long studentId, String quarter, String subject, String activity, String score) {
        return saveOrUpdateGradeRecord(studentId, quarter, subject, activity, null, score);
    }

    public boolean saveOrUpdateGradeRecord(long studentId, String quarter, String subject, String activity,
                                           String activityName, String score) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            lastError = "";
            String cleanActivity = activity != null ? activity.trim() : "";
            // Program Performance allows several named entries, so it never overwrites an existing row.
            long recordId = cleanActivity.equalsIgnoreCase("Program Performance")
                    ? -1 : findGradeRecordId(db, studentId, quarter, subject != null ? subject.trim() : "", cleanActivity);
            if (recordId != -1) {
                ContentValues v = new ContentValues();
                v.put("score", score != null ? score : "0");
                v.put("date", today());
                if (activityName != null && !activityName.trim().isEmpty()) v.put("activity_name", activityName.trim());
                return db.update("grade_records", v, "record_id = ?", new String[]{String.valueOf(recordId)}) > 0;
            }
            return insertRow(studentId, quarter, subject, cleanActivity,
                    activityName != null ? activityName.trim() : "", score != null ? score : "0", null, null);
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error in saveOrUpdateGradeRecord", e);
            return false;
        }
    }

    /** Updates the editable fields of exactly one existing record. */
    public boolean updateGradeRecord(long recordId, String activityName, String score) {
        try {
            ContentValues v = new ContentValues();
            v.put("activity_name", activityName != null ? activityName.trim() : "");
            v.put("score", score != null ? score : "0");
            v.put("date", today());
            lastError = "";
            return getWritableDatabase().update("grade_records", v, "record_id = ?",
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
            ContentValues v = new ContentValues();
            v.put("score", score != null ? score : "0");
            v.put("date", today());
            lastError = "";
            return getWritableDatabase().update("grade_records", v, "record_id = ?",
                    new String[]{String.valueOf(recordId)}) > 0;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e(TAG, "Error in updateGradeRecordScore", e);
            return false;
        }
    }

    public boolean deleteGradeRecord(long recordId) {
        try {
            return getWritableDatabase().delete("grade_records", "record_id = ?",
                    new String[]{String.valueOf(recordId)}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error in deleteGradeRecord", e);
            return false;
        }
    }

    public boolean deleteAttendanceRecord(long recordId) {
        if (recordId < 1) return false;
        try {
            return getWritableDatabase().delete("grade_records",
                    "record_id = ? AND LOWER(TRIM(activity)) = 'attendance'",
                    new String[]{String.valueOf(recordId)}) > 0;
        } catch (Exception e) {
            Log.e(TAG, "Could not delete attendance record", e);
            return false;
        }
    }
}
