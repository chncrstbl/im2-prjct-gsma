package com.example.priangradingsystem.records;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.data.DatabaseHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecordsActivity extends AppCompatActivity {

    private static final String[] SUBJECTS = {"Writing", "Math", "Reading", "Life Skills", "PE & Arts"};
    private static final String[] ACTIVITIES = {"Books", "Homework", "Attendance", "Exam", "Program Performance"};

    private Button btn1stQtr, btn2ndQtr, btn3rdQtr;
    private int currentQuarter = 1;
    private DatabaseHelper dbHelper;
    private String studentName = "Vea Arevalo";
    /** Resolved once; every read/write below is keyed on this id, never on the display name. */
    private long studentId = -1;

    /** One row of the student's grade_records table. */
    private static class GradeRow {
        long id;
        String quarter, subject, activity, score, date;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_records);

        dbHelper = new DatabaseHelper(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Student info from intent if available
        String nameFromIntent = getIntent().getStringExtra("STUDENT_NAME");
        if (nameFromIntent != null && !nameFromIntent.trim().isEmpty()) {
            studentName = nameFromIntent.trim();
        }

        // Prefer an explicit id; fall back to the name.
        long idFromIntent = getIntent().getLongExtra("STUDENT_ID", -1);
        if (idFromIntent < 0) idFromIntent = getIntent().getIntExtra("STUDENT_ID", -1);
        studentId = idFromIntent > 0 ? idFromIntent : dbHelper.getStudentIdByName(studentName);
        if (studentId < 0) {
            Toast.makeText(this, "Student \"" + studentName + "\" was not found in the database.", Toast.LENGTH_LONG).show();
        }

        String studentLevel = getIntent().getStringExtra("STUDENT_LEVEL");
        String studentPhotoUri = getIntent().getStringExtra("STUDENT_PHOTO_URI");

        TextView tvStudentName = findViewById(R.id.tvStudentName);
        TextView tvStudentGradeLevel = findViewById(R.id.tvStudentGradeLevel);
        ImageView imgStudentAvatar = findViewById(R.id.imgStudentAvatar);

        if (tvStudentName != null) {
            tvStudentName.setText(studentName);
        }
        if (studentLevel != null && !studentLevel.trim().isEmpty() && tvStudentGradeLevel != null) {
            tvStudentGradeLevel.setText(studentLevel);
        }
        TextView tvNoPhotoText = findViewById(R.id.tvNoPhotoText);
        boolean hasPhoto = false;

        if (studentPhotoUri != null && !studentPhotoUri.trim().isEmpty() && imgStudentAvatar != null) {
            try {
                Uri uri = Uri.parse(studentPhotoUri);
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                imgStudentAvatar.setImageURI(uri);
                imgStudentAvatar.setVisibility(View.VISIBLE);
                if (tvNoPhotoText != null) tvNoPhotoText.setVisibility(View.GONE);
                hasPhoto = true;
            } catch (Exception e) {
                hasPhoto = false;
            }
        }

        String studentGender = getIntent().getStringExtra("STUDENT_GENDER");
        if ((studentGender == null || studentGender.trim().isEmpty()) && dbHelper != null && studentId > 0) {
            studentGender = dbHelper.getStudentSex(studentId);
        }

        if (!hasPhoto) {
            boolean isMale = (studentGender != null && (studentGender.equalsIgnoreCase("Male") || studentGender.equalsIgnoreCase("M")))
                    || (studentName != null && (studentName.contains("John") || studentName.contains("Mark")));

            if (imgStudentAvatar != null) {
                imgStudentAvatar.setImageResource(isMale ? R.drawable.profile_foreground_placeholder_male : R.drawable.profile_foreground_placeholder_female);
                imgStudentAvatar.setVisibility(View.VISIBLE);
            }
            if (tvNoPhotoText != null) {
                tvNoPhotoText.setVisibility(View.GONE);
            }
        }

        // Quarter Selection Buttons
        btn1stQtr = findViewById(R.id.btn1stQtr);
        btn2ndQtr = findViewById(R.id.btn2ndQtr);
        btn3rdQtr = findViewById(R.id.btn3rdQtr);

        if (btn1stQtr != null) {
            btn1stQtr.setOnClickListener(v -> selectQuarter(1, true));
        }
        if (btn2ndQtr != null) {
            btn2ndQtr.setOnClickListener(v -> selectQuarter(2, true));
        }
        if (btn3rdQtr != null) {
            btn3rdQtr.setOnClickListener(v -> selectQuarter(3, true));
        }

        selectQuarter(1, false);

        // Activity Dropdown Menu
        View layoutActivityDropdown = findViewById(R.id.layoutActivityDropdown);
        TextView tvSelectedActivity = findViewById(R.id.tvSelectedActivity);
        if (layoutActivityDropdown != null && tvSelectedActivity != null) {
            layoutActivityDropdown.setOnClickListener(v -> {
                showCustomDropdown(v, tvSelectedActivity, ACTIVITIES, selectedActivity -> {
                    tvSelectedActivity.setText(selectedActivity);
                    if (selectedActivity.equalsIgnoreCase("Exam")) {
                        TextView tvSelectedSubject = findViewById(R.id.tvSelectedSubject);
                        if (tvSelectedSubject != null) {
                            tvSelectedSubject.setText("Select Subject");
                        }
                    }
                    updateUiForActivity();
                });
            });
        }

        // Subject Dropdown Menu
        View layoutSubjectDropdown = findViewById(R.id.layoutSubjectDropdown);
        TextView tvSelectedSubject = findViewById(R.id.tvSelectedSubject);
        if (layoutSubjectDropdown != null && tvSelectedSubject != null) {
            layoutSubjectDropdown.setOnClickListener(v -> {
                String[] options = currentActivity().equalsIgnoreCase("Program Performance")
                        ? new String[]{"Life Skills", "PE & Arts"}
                        : SUBJECTS;
                showCustomDropdown(v, tvSelectedSubject, options, selectedSubject -> {
                    tvSelectedSubject.setText(selectedSubject);
                    updateUiForActivity();
                });
            });
        }

        updateUiForActivity();

        // EDIT: the record for the current Quarter + Activity + Subject
        ImageView btnEditRecord = findViewById(R.id.btnEditRecord);
        if (btnEditRecord != null) {
            btnEditRecord.setOnClickListener(v -> {
                String activity = currentActivity();
                if (!requireStudentAndSubject()) return;

                String subject = currentSubjectForActivity();

                if (activity.equalsIgnoreCase("Program Performance")) {
                    String[] options = new String[]{"0/2", "1/2", "2/2"};
                    new AlertDialog.Builder(this)
                            .setTitle("Program Performance Score")
                            .setItems(options, (dialog, which) -> {
                                String selectedScore = options[which];
                                if (dbHelper.saveOrUpdateGradeRecord(studentId, getQuarterString(), subject, activity, selectedScore)) {
                                    Toast.makeText(this, "Program Performance saved: " + selectedScore, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Could not save record.", Toast.LENGTH_SHORT).show();
                                }
                                updateUiForActivity();
                            })
                            .show();
                } else if (activity.equalsIgnoreCase("Attendance")) {
                    String stored = dbHelper.getGradeRecordScore(studentId, getQuarterString(), subject, activity);
                    String cleanDigits = (stored != null) ? stored.replaceAll("[^0-9]", "").trim() : "0";
                    if (cleanDigits.isEmpty()) cleanDigits = "0";

                    EditText input = new EditText(this);
                    input.setHint("e.g. 20");
                    input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                    input.setText(cleanDigits);
                    input.setSelection(input.getText().length());

                    new AlertDialog.Builder(this)
                            .setTitle("Days Present")
                            .setView(input)
                            .setPositiveButton("Save", (dialog, which) -> {
                                String numStr = input.getText().toString().trim();
                                if (numStr.isEmpty()) numStr = "0";
                                String formatted = numStr + " Days";
                                if (dbHelper.saveOrUpdateGradeRecord(studentId, getQuarterString(), subject, activity, formatted)) {
                                    Toast.makeText(this, "Attendance saved: " + formatted, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Could not save attendance.", Toast.LENGTH_SHORT).show();
                                }
                                updateUiForActivity();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else if (activity.equalsIgnoreCase("Exam")) {
                    String stored = dbHelper.getGradeRecordScore(studentId, getQuarterString(), subject, "Exam");
                    showExamScoreDialog(stored != null ? stored : "", newScore -> {
                        if (dbHelper.saveOrUpdateGradeRecord(studentId, getQuarterString(), subject, "Exam", newScore)) {
                            Toast.makeText(this, "Exam score saved: " + newScore, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Could not save exam score.", Toast.LENGTH_SHORT).show();
                        }
                        updateUiForActivity();
                    });
                } else {
                    String[] ratings = new String[]{"No Star", "1 Star", "2 Stars", "3 Stars"};
                    new AlertDialog.Builder(this)
                            .setTitle("Rate Grade Record")
                            .setItems(ratings, (dialog, which) -> {
                                if (dbHelper.saveOrUpdateGradeRecord(studentId, getQuarterString(), subject, activity, String.valueOf(which))) {
                                    Toast.makeText(this, "Grade record saved: " + ((which == 0) ? "No stars" : (which + " star(s)")), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Could not save grade record.", Toast.LENGTH_SHORT).show();
                                }
                                updateUiForActivity();
                            })
                            .show();
                }
            });
        }

        // DELETE: the record for the current Quarter + Activity + Subject
        ImageView btnDeleteRecord = findViewById(R.id.btnDeleteRecord);
        if (btnDeleteRecord != null) {
            btnDeleteRecord.setOnClickListener(v -> {
                String activity = currentActivity();
                if (!requireStudentAndSubject()) return;

                String subject = currentSubjectForActivity();
                long recordId = dbHelper.findGradeRecordId(studentId, getQuarterString(), subject, activity);
                if (recordId < 0) {
                    Toast.makeText(this, "No saved record for " + getQuarterString() + " / " + activity + ".", Toast.LENGTH_SHORT).show();
                    return;
                }
                GradeRow row = new GradeRow();
                row.id = recordId;
                row.quarter = getQuarterString();
                row.subject = subject;
                row.activity = activity;
                confirmDelete(row, this::updateUiForActivity);
            });
        }

    }

    // ---------------------------------------------------------------------------------
    // View all of the student's grade_records (tap a row to edit or delete it)
    // ---------------------------------------------------------------------------------

    private List<GradeRow> loadStudentRecords() {
        List<GradeRow> rows = new ArrayList<>();
        if (studentId < 0) return rows;
        try (Cursor c = dbHelper.getGradeRecordsForStudent(studentId)) {
            if (c == null) return rows;
            while (c.moveToNext()) {
                GradeRow r = new GradeRow();
                r.id = c.getLong(c.getColumnIndexOrThrow("record_id"));
                r.quarter = DatabaseHelper.normalizeQuarter(c.getString(c.getColumnIndexOrThrow("quarter")));
                r.subject = nz(c.getString(c.getColumnIndexOrThrow("subject")));
                r.activity = nz(c.getString(c.getColumnIndexOrThrow("activity")));
                r.score = nz(c.getString(c.getColumnIndexOrThrow("score")));
                r.date = nz(c.getString(c.getColumnIndexOrThrow("date")));
                rows.add(r);
            }
        }
        Collections.sort(rows, (a, b) -> {
            int cmp = a.quarter.compareTo(b.quarter);
            if (cmp == 0) cmp = a.subject.compareToIgnoreCase(b.subject);
            if (cmp == 0) cmp = a.activity.compareToIgnoreCase(b.activity);
            return cmp;
        });
        return rows;
    }

    private void showRecordsDialog() {
        if (studentId < 0) {
            Toast.makeText(this, "Student not found in the database.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<GradeRow> rows = loadStudentRecords();
        if (rows.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(studentName + " - Grade Records")
                    .setMessage("No grade records saved yet.")
                    .setPositiveButton("Close", null)
                    .show();
            return;
        }

        String[] labels = new String[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            GradeRow r = rows.get(i);
            String subject = r.subject.isEmpty() ? "-" : r.subject;
            labels[i] = r.quarter + "\n" + subject + " \u2022 " + r.activity + "  \u2192  " + describeScore(r)
                    + (r.date.isEmpty() ? "" : "\n" + r.date);
        }

        new AlertDialog.Builder(this)
                .setTitle(studentName + " - Grade Records")
                .setItems(labels, (dialog, which) -> showRowActions(rows.get(which)))
                .setPositiveButton("Close", null)
                .show();
    }

    private void showRowActions(GradeRow row) {
        new AlertDialog.Builder(this)
                .setTitle(row.quarter + " \u2022 " + row.subject + " \u2022 " + row.activity)
                .setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) editRow(row);
                    else confirmDelete(row, this::afterRecordChanged);
                })
                .setNegativeButton("Back", (d, w) -> showRecordsDialog())
                .show();
    }

    private void editRow(GradeRow row) {
        if (row.activity.equalsIgnoreCase("Program Performance")) {
            String[] options = new String[]{"0/2", "1/2", "2/2"};
            new AlertDialog.Builder(this)
                    .setTitle("Program Performance Score")
                    .setItems(options, (dialog, which) -> {
                        boolean ok = dbHelper.updateGradeRecordScore(row.id, options[which]);
                        Toast.makeText(this, ok ? "Score updated." : "Could not update record.", Toast.LENGTH_SHORT).show();
                        afterRecordChanged();
                    })
                    .setNegativeButton("Cancel", (d, w) -> showRecordsDialog())
                    .show();
        } else if (row.activity.equalsIgnoreCase("Exam")) {
            showExamScoreDialog(row.score, newScore -> {
                boolean ok = dbHelper.updateGradeRecordScore(row.id, newScore);
                Toast.makeText(this, ok ? "Exam score updated." : "Could not update record.", Toast.LENGTH_SHORT).show();
                afterRecordChanged();
            });
        } else if (row.activity.equalsIgnoreCase("Attendance")) {
            EditText input = new EditText(this);
            input.setText(row.score);
            new AlertDialog.Builder(this)
                    .setTitle("Edit Attendance Counter")
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newCount = input.getText().toString().trim();
                        boolean ok = dbHelper.updateGradeRecordScore(row.id, newCount);
                        Toast.makeText(this, ok ? "Attendance updated." : "Could not update record.", Toast.LENGTH_SHORT).show();
                        afterRecordChanged();
                    })
                    .setNegativeButton("Cancel", (d, w) -> showRecordsDialog())
                    .show();
        } else {
            String[] ratings = new String[]{"No Star", "1 Star", "2 Stars", "3 Stars"};
            new AlertDialog.Builder(this)
                    .setTitle("Rate: " + row.subject + " \u2022 " + row.activity)
                    .setItems(ratings, (dialog, which) -> {
                        boolean ok = dbHelper.updateGradeRecordScore(row.id, String.valueOf(which));
                        Toast.makeText(this, ok ? "Grade record updated." : "Could not update record.", Toast.LENGTH_SHORT).show();
                        afterRecordChanged();
                    })
                    .setNegativeButton("Cancel", (d, w) -> showRecordsDialog())
                    .show();
        }
    }

    private void confirmDelete(GradeRow row, Runnable onDeleted) {
        new AlertDialog.Builder(this)
                .setTitle("Delete grade record?")
                .setMessage(row.quarter + " \u2022 " + row.subject + " \u2022 " + row.activity
                        + "\n\nThis cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean ok = dbHelper.deleteGradeRecord(row.id);
                    Toast.makeText(this, ok ? "Grade record deleted." : "Could not delete record.", Toast.LENGTH_SHORT).show();
                    if (ok && onDeleted != null) onDeleted.run();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void afterRecordChanged() {
        updateUiForActivity();
        showRecordsDialog();
    }

    interface OnScoreEntered {
        void onScore(String score);
    }

    private void showExamScoreDialog(String initial, OnScoreEntered callback) {
        EditText input = new EditText(this);
        input.setHint("e.g. 15/20");
        input.setText(initial == null ? "" : initial);
        input.setSelection(input.getText().length());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Exam Score")
                .setView(input)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String s = input.getText().toString().trim().replaceAll("\\s+", "");
            if (!isValidExamScore(s)) {
                input.setError("Use the format score/total, e.g. 15/20");
                return;
            }
            dialog.dismiss();
            callback.onScore(s);
        }));
        dialog.show();
    }

    private boolean isValidExamScore(String s) {
        if (!s.matches("\\d{1,4}/\\d{1,4}")) return false;
        String[] p = s.split("/");
        return Integer.parseInt(p[1]) > 0 && Integer.parseInt(p[0]) <= Integer.parseInt(p[1]);
    }

    private String describeScore(GradeRow r) {
        if (r.activity.equalsIgnoreCase("Exam") || r.activity.equalsIgnoreCase("Program Performance") || r.activity.equalsIgnoreCase("Attendance")) {
            return r.score.isEmpty() ? "-" : r.score;
        }
        int stars = parseStars(r.score);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 3; i++) sb.append(i <= stars ? '\u2605' : '\u2606');
        return sb.toString();
    }

    private int parseStars(String score) {
        try {
            String clean = score == null ? "" : score.replaceAll("[^0-9]", "").trim();
            if (clean.isEmpty()) return 0;
            return Math.min(3, Integer.parseInt(clean));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private String currentActivity() {
        TextView tv = findViewById(R.id.tvSelectedActivity);
        return tv != null ? tv.getText().toString() : "Books";
    }

    private String currentSubject() {
        TextView tv = findViewById(R.id.tvSelectedSubject);
        return tv != null ? tv.getText().toString() : "";
    }

    private String currentSubjectForActivity() {
        String activity = currentActivity();
        if (activity.equalsIgnoreCase("Homework") || activity.equalsIgnoreCase("Attendance") || activity.equalsIgnoreCase("Program Performance")) {
            return "General";
        }
        return currentSubject();
    }

    private boolean isValidSubject(String subject) {
        return Arrays.asList(SUBJECTS).contains(subject) || subject.equalsIgnoreCase("General");
    }

    private boolean requireStudentAndSubject() {
        if (studentId < 0) {
            Toast.makeText(this, "Student not found in the database.", Toast.LENGTH_SHORT).show();
            return false;
        }
        String activity = currentActivity();
        if (activity.equalsIgnoreCase("Homework") || activity.equalsIgnoreCase("Attendance") || activity.equalsIgnoreCase("Program Performance")) {
            return true;
        }
        if (!isValidSubject(currentSubject())) {
            Toast.makeText(this, "Please select a subject first.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private String getQuarterString() {
        return (currentQuarter == 1) ? "1st Quarter" : (currentQuarter == 2) ? "2nd Quarter" : "3rd Quarter";
    }

    private void updateUiForActivity() {
        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        TextView tvSelectedActivity = findViewById(R.id.tvSelectedActivity);
        TextView tvRecordSubtitle = findViewById(R.id.tvRecordSubtitle);
        View tvSubjectLabel = findViewById(R.id.tvSubjectLabel);
        View layoutSubjectDropdown = findViewById(R.id.layoutSubjectDropdown);
        View layoutStarsContainer = findViewById(R.id.layoutStarsContainer);
        View layoutExamScoreContainer = findViewById(R.id.layoutExamScoreContainer);
        View cardGradeRecord = findViewById(R.id.cardGradeRecord);

        String activity = (tvSelectedActivity != null) ? tvSelectedActivity.getText().toString() : "Books";
        boolean isExam = activity.equalsIgnoreCase("Exam");
        boolean isAttendance = activity.equalsIgnoreCase("Attendance");
        boolean isHomework = activity.equalsIgnoreCase("Homework");
        boolean isProgramPerformance = activity.equalsIgnoreCase("Program Performance");

        ImageView ivRecordSymbol = findViewById(R.id.ivRecordSymbol);
        TextView tvRecordTitle = findViewById(R.id.tvRecordTitle);

        if (tvRecordTitle != null) {
            String dbActivityName = dbHelper.getGradeRecordActivityName(studentId, getQuarterString(), currentSubjectForActivity(), activity);
            if (dbActivityName != null && !dbActivityName.trim().isEmpty()) {
                tvRecordTitle.setText(dbActivityName);
            } else {
                tvRecordTitle.setText("Grade Record");
            }
        }

        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText(isExam ? "Grade Records" : "Grade Record");
        }

        boolean hideSubject = isHomework || isAttendance || isProgramPerformance;
        if (tvSubjectLabel != null) tvSubjectLabel.setVisibility(hideSubject ? View.GONE : View.VISIBLE);
        if (layoutSubjectDropdown != null) layoutSubjectDropdown.setVisibility(hideSubject ? View.GONE : View.VISIBLE);

        if (cardGradeRecord != null) cardGradeRecord.setVisibility(View.VISIBLE);

        if (isExam) {
            if (ivRecordSymbol != null) ivRecordSymbol.setVisibility(View.VISIBLE);
            if (tvRecordTitle != null) tvRecordTitle.setVisibility(View.VISIBLE);
            if (tvRecordSubtitle != null) {
                tvRecordSubtitle.setText(getQuarterString());
                tvRecordSubtitle.setVisibility(View.VISIBLE);
            }
            if (layoutStarsContainer != null) layoutStarsContainer.setVisibility(View.GONE);
            if (layoutExamScoreContainer != null) layoutExamScoreContainer.setVisibility(View.VISIBLE);
            loadExamScoreForCurrentSelection("0/20");
        } else if (isProgramPerformance) {
            if (ivRecordSymbol != null) ivRecordSymbol.setVisibility(View.VISIBLE);
            if (tvRecordTitle != null) tvRecordTitle.setVisibility(View.VISIBLE);
            if (tvRecordSubtitle != null) tvRecordSubtitle.setVisibility(View.GONE);
            if (layoutStarsContainer != null) layoutStarsContainer.setVisibility(View.GONE);
            if (layoutExamScoreContainer != null) layoutExamScoreContainer.setVisibility(View.VISIBLE);
            loadExamScoreForCurrentSelection("0/2");
        } else if (isAttendance) {
            if (ivRecordSymbol != null) ivRecordSymbol.setVisibility(View.GONE);
            if (tvRecordTitle != null) tvRecordTitle.setVisibility(View.GONE);
            if (tvRecordSubtitle != null) tvRecordSubtitle.setVisibility(View.GONE);
            if (layoutStarsContainer != null) layoutStarsContainer.setVisibility(View.GONE);
            if (layoutExamScoreContainer != null) layoutExamScoreContainer.setVisibility(View.VISIBLE);
            loadAttendanceScoreForCurrentSelection("0 Days");
        } else {
            // Books or Homework
            if (ivRecordSymbol != null) ivRecordSymbol.setVisibility(View.VISIBLE);
            if (tvRecordTitle != null) tvRecordTitle.setVisibility(View.VISIBLE);
            if (tvRecordSubtitle != null) tvRecordSubtitle.setVisibility(View.GONE);
            if (layoutStarsContainer != null) layoutStarsContainer.setVisibility(View.VISIBLE);
            if (layoutExamScoreContainer != null) layoutExamScoreContainer.setVisibility(View.GONE);
            loadRatingForCurrentSelection();
        }
    }

    private void loadExamScoreForCurrentSelection(String defaultScore) {
        TextView tvExamScore = findViewById(R.id.tvExamScore);
        if (tvExamScore == null) return;

        String score = defaultScore;
        String subject = currentSubjectForActivity();
        String activity = currentActivity();

        if (studentId >= 0) {
            String dbScore = dbHelper.getGradeRecordScore(studentId, getQuarterString(), subject, activity);
            if (dbScore != null && !dbScore.trim().isEmpty()) {
                score = dbScore.trim();
                if (activity.equalsIgnoreCase("Program Performance")) {
                    if (score.equalsIgnoreCase("2 Points") || score.equalsIgnoreCase("2")) score = "2/2";
                    else if (score.equalsIgnoreCase("1 Point") || score.equalsIgnoreCase("1")) score = "1/2";
                    else if (score.equalsIgnoreCase("0 Points") || score.equalsIgnoreCase("0")) score = "0/2";
                }
            }
        }
        tvExamScore.setText(score);
    }

    private void loadAttendanceScoreForCurrentSelection(String defaultScore) {
        TextView tvExamScore = findViewById(R.id.tvExamScore);
        if (tvExamScore == null) return;

        String score = defaultScore;
        if (studentId >= 0) {
            String dbScore = dbHelper.getGradeRecordScore(studentId, getQuarterString(), "General", "Attendance");
            if (dbScore != null && !dbScore.trim().isEmpty()) {
                String trimmed = dbScore.trim();
                if (trimmed.equalsIgnoreCase("present")) {
                    score = "Present";
                } else if (trimmed.equalsIgnoreCase("absent")) {
                    score = "Absent";
                } else {
                    String cleanDigits = trimmed.replaceAll("[^0-9]", "").trim();
                    score = cleanDigits.isEmpty() ? trimmed : cleanDigits + " Days";
                }
            }
        }
        tvExamScore.setText(score);
    }

    private void loadRatingForCurrentSelection() {
        ImageView ivStar1 = findViewById(R.id.ivStar1);
        ImageView ivStar2 = findViewById(R.id.ivStar2);
        ImageView ivStar3 = findViewById(R.id.ivStar3);
        if (ivStar1 == null || ivStar2 == null || ivStar3 == null) return;

        setRatingInternal(0, ivStar1, ivStar2, ivStar3);

        String subject = currentSubjectForActivity();
        if (studentId < 0) return;

        String dbScore = dbHelper.getGradeRecordScore(studentId, getQuarterString(), subject, currentActivity());
        setRatingInternal(parseStars(dbScore), ivStar1, ivStar2, ivStar3);
    }

    private void setRatingInternal(int rating, ImageView s1, ImageView s2, ImageView s3) {
        s1.setImageResource(rating >= 1 ? R.drawable.star_filled : R.drawable.star_normal);
        s2.setImageResource(rating >= 2 ? R.drawable.star_filled : R.drawable.star_normal);
        s3.setImageResource(rating >= 3 ? R.drawable.star_filled : R.drawable.star_normal);
    }

    interface OnItemSelectedListener {
        void onItemSelected(String item);
    }

    private void showCustomDropdown(View anchor, TextView targetTextView, String[] items, OnItemSelectedListener listener) {
        String currentSelected = targetTextView.getText().toString();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                items
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.BLACK);
                view.setTextSize(16);
                view.setPadding(32, 24, 32, 24);

                String itemText = getItem(position);
                if (itemText != null && itemText.equals(currentSelected)) {
                    view.setBackgroundResource(R.drawable.dropdown_selected);
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT);
                }
                return view;
            }
        };

        ListPopupWindow popupWindow = new ListPopupWindow(this);
        popupWindow.setAdapter(adapter);
        popupWindow.setAnchorView(anchor);
        popupWindow.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.dropdown_box, null));
        popupWindow.setWidth(anchor.getWidth());
        popupWindow.setHeight(ListPopupWindow.WRAP_CONTENT);
        popupWindow.setModal(true);

        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(currentSelected)) {
                popupWindow.setSelection(i);
                break;
            }
        }

        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            String selected = items[position];
            listener.onItemSelected(selected);
            Toast.makeText(this, selected, Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });
        popupWindow.show();
    }

    private void selectQuarter(int quarter, boolean showToast) {
        if (btn1stQtr == null || btn2ndQtr == null || btn3rdQtr == null) return;

        currentQuarter = quarter;

        btn1stQtr.setBackgroundResource(quarter == 1 ? R.drawable.highlighted_quarter : R.drawable.regular_quarter);
        btn1stQtr.setTextColor(quarter == 1 ? Color.BLACK : Color.DKGRAY);

        btn2ndQtr.setBackgroundResource(quarter == 2 ? R.drawable.highlighted_quarter : R.drawable.regular_quarter);
        btn2ndQtr.setTextColor(quarter == 2 ? Color.BLACK : Color.DKGRAY);

        btn3rdQtr.setBackgroundResource(quarter == 3 ? R.drawable.highlighted_quarter : R.drawable.regular_quarter);
        btn3rdQtr.setTextColor(quarter == 3 ? Color.BLACK : Color.DKGRAY);

        updateUiForActivity();

        if (showToast) {
            String qName = (quarter == 1) ? "1st" : (quarter == 2) ? "2nd" : "3rd";
            Toast.makeText(this, qName + " Quarter Selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
    }
}
