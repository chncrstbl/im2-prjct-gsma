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
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.data.DatabaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("all")
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
        String quarter, subject, activity, activityName, score, date;
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
                showCustomDropdown(v, tvSelectedSubject, SUBJECTS, selectedSubject -> {
                    tvSelectedSubject.setText(selectedSubject);
                    updateUiForActivity();
                });
            });
        }

        updateUiForActivity();
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
                        String formattedScore = which + " ⭐ (" + which + " Stars)";
                        boolean ok = dbHelper.updateGradeRecordScore(row.id, formattedScore);
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
        View tvSubjectLabel = findViewById(R.id.tvSubjectLabel);
        View layoutSubjectDropdown = findViewById(R.id.layoutSubjectDropdown);
        LinearLayout layoutCardsContainer = findViewById(R.id.layoutCardsContainer);

        if (layoutCardsContainer == null) return;

        String activity = (tvSelectedActivity != null) ? tvSelectedActivity.getText().toString() : "Books";
        boolean isExam = activity.equalsIgnoreCase("Exam");
        boolean isAttendance = activity.equalsIgnoreCase("Attendance");
        boolean isHomework = activity.equalsIgnoreCase("Homework");
        boolean isProgramPerformance = activity.equalsIgnoreCase("Program Performance");

        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText(isExam ? "Grade Records" : "Grade Record");
        }

        boolean hideSubject = isHomework || isAttendance || isProgramPerformance;
        if (tvSubjectLabel != null) tvSubjectLabel.setVisibility(hideSubject ? View.GONE : View.VISIBLE);
        if (layoutSubjectDropdown != null) layoutSubjectDropdown.setVisibility(hideSubject ? View.GONE : View.VISIBLE);

        layoutCardsContainer.removeAllViews();

        List<GradeRow> rows = new ArrayList<>();
        if (studentId >= 0 && (!requireStudentAndSubject() || true)) {
            try (Cursor c = dbHelper.getMatchingGradeRecords(studentId, getQuarterString(), currentSubjectForActivity(), activity)) {
                if (c != null) {
                    while (c.moveToNext()) {
                        GradeRow r = new GradeRow();
                        r.id = c.getLong(c.getColumnIndexOrThrow("record_id"));
                        r.quarter = DatabaseHelper.normalizeQuarter(c.getString(c.getColumnIndexOrThrow("quarter")));
                        r.subject = nz(c.getString(c.getColumnIndexOrThrow("subject")));
                        r.activity = nz(c.getString(c.getColumnIndexOrThrow("activity")));
                        r.activityName = nz(c.getString(c.getColumnIndexOrThrow("activity_name")));
                        r.score = nz(c.getString(c.getColumnIndexOrThrow("score")));
                        r.date = nz(c.getString(c.getColumnIndexOrThrow("date")));
                        rows.add(r);
                    }
                }
            } catch (Exception ignored) {}
        }

        if (rows.isEmpty()) {
            GradeRow defaultRow = new GradeRow();
            defaultRow.id = -1;
            defaultRow.quarter = getQuarterString();
            defaultRow.subject = currentSubjectForActivity();
            defaultRow.activity = activity;
            defaultRow.activityName = isProgramPerformance ? "Program Performance" : "Grade Record";
            if (isExam) defaultRow.score = "0/20";
            else if (isProgramPerformance) defaultRow.score = "0/2";
            else if (isAttendance) defaultRow.score = "0 Days";
            else defaultRow.score = "0";

            View card = createGradeCardView(defaultRow);
            layoutCardsContainer.addView(card);
        } else {
            for (GradeRow r : rows) {
                View card = createGradeCardView(r);
                layoutCardsContainer.addView(card);
            }
        }
    }

    private View createGradeCardView(GradeRow r) {
        boolean isExam = r.activity.equalsIgnoreCase("Exam");
        boolean isAttendance = r.activity.equalsIgnoreCase("Attendance");
        boolean isProgramPerformance = r.activity.equalsIgnoreCase("Program Performance");
        boolean isStars = r.activity.equalsIgnoreCase("Books") || r.activity.equalsIgnoreCase("Homework");

        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, (int) (16 * getResources().getDisplayMetrics().density));
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(16 * getResources().getDisplayMetrics().density);
        card.setStrokeColor(Color.parseColor("#DDDDDD"));
        card.setStrokeWidth((int) (1.5f * getResources().getDisplayMetrics().density));

        RelativeLayout relativeLayout = new RelativeLayout(this);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        relativeLayout.setPadding(p, p, p, p);
        card.addView(relativeLayout);

        ImageView ivSymbol = new ImageView(this);
        ivSymbol.setId(View.generateViewId());
        int symbolSize = (int) (32 * getResources().getDisplayMetrics().density);
        RelativeLayout.LayoutParams symbolParams = new RelativeLayout.LayoutParams(symbolSize, symbolSize);
        symbolParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        symbolParams.addRule(RelativeLayout.CENTER_VERTICAL);
        ivSymbol.setLayoutParams(symbolParams);
        ivSymbol.setImageResource(R.drawable.grade_record_symbol);
        ivSymbol.setVisibility(isAttendance ? View.GONE : View.VISIBLE);
        relativeLayout.addView(ivSymbol);

        LinearLayout titleContainer = new LinearLayout(this);
        titleContainer.setId(View.generateViewId());
        titleContainer.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout.LayoutParams titleParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.addRule(RelativeLayout.CENTER_VERTICAL);
        if (!isAttendance) {
            titleParams.addRule(RelativeLayout.END_OF, ivSymbol.getId());
            titleParams.setMarginStart((int) (12 * getResources().getDisplayMetrics().density));
        } else {
            titleParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        }
        titleContainer.setLayoutParams(titleParams);

        TextView tvTitle = new TextView(this);
        String titleText = r.activityName;
        if (titleText == null || titleText.trim().isEmpty()) {
            titleText = isProgramPerformance ? "Program Performance" : "Grade Record";
        }
        tvTitle.setText(titleText);
        tvTitle.setTextColor(Color.BLACK);
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setVisibility(isAttendance ? View.GONE : View.VISIBLE);
        titleContainer.addView(tvTitle);

        if (isExam) {
            TextView tvSubtitle = new TextView(this);
            tvSubtitle.setText(r.quarter);
            tvSubtitle.setTextColor(Color.parseColor("#888888"));
            tvSubtitle.setTextSize(12);
            titleContainer.addView(tvSubtitle);
        }
        relativeLayout.addView(titleContainer);

        LinearLayout actionsContainer = new LinearLayout(this);
        actionsContainer.setId(View.generateViewId());
        actionsContainer.setOrientation(LinearLayout.HORIZONTAL);
        RelativeLayout.LayoutParams actionsParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        actionsParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        actionsParams.addRule(RelativeLayout.CENTER_VERTICAL);
        actionsContainer.setLayoutParams(actionsParams);

        int iconSize = (int) (24 * getResources().getDisplayMetrics().density);

        ImageView btnEdit = new ImageView(this);
        btnEdit.setImageResource(R.drawable.edit_record);
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        editParams.setMarginEnd((int) (12 * getResources().getDisplayMetrics().density));
        btnEdit.setLayoutParams(editParams);
        btnEdit.setOnClickListener(v -> onEditCardClicked(r));
        actionsContainer.addView(btnEdit);

        ImageView btnDelete = new ImageView(this);
        btnDelete.setImageResource(R.drawable.delete_record);
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        btnDelete.setOnClickListener(v -> onDeleteCardClicked(r));
        actionsContainer.addView(btnDelete);

        relativeLayout.addView(actionsContainer);

        if (isStars) {
            int starsCount = parseStars(r.score);
            LinearLayout starsContainer = new LinearLayout(this);
            starsContainer.setOrientation(LinearLayout.HORIZONTAL);
            RelativeLayout.LayoutParams starsParams = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            starsParams.addRule(RelativeLayout.CENTER_VERTICAL);
            starsParams.addRule(RelativeLayout.START_OF, actionsContainer.getId());
            starsParams.setMarginEnd((int) (16 * getResources().getDisplayMetrics().density));
            starsContainer.setLayoutParams(starsParams);

            for (int i = 1; i <= 3; i++) {
                ImageView star = new ImageView(this);
                star.setImageResource(i <= starsCount ? R.drawable.star_filled : R.drawable.star_normal);
                LinearLayout.LayoutParams starParams = new LinearLayout.LayoutParams(iconSize, iconSize);
                if (i < 3) starParams.setMarginEnd((int) (4 * getResources().getDisplayMetrics().density));
                star.setLayoutParams(starParams);
                starsContainer.addView(star);
            }
            relativeLayout.addView(starsContainer);
        } else {
            TextView tvScore = new TextView(this);
            String scoreText = r.score;
            if (isProgramPerformance) {
                if (scoreText.equalsIgnoreCase("2 Points") || scoreText.equalsIgnoreCase("2")) scoreText = "2/2";
                else if (scoreText.equalsIgnoreCase("1 Point") || scoreText.equalsIgnoreCase("1")) scoreText = "1/2";
                else if (scoreText.equalsIgnoreCase("0 Points") || scoreText.equalsIgnoreCase("0")) scoreText = "0/2";
            } else if (isAttendance) {
                String cleanDigits = scoreText.replaceAll("[^0-9]", "").trim();
                scoreText = cleanDigits.isEmpty() ? "0 Days" : cleanDigits + " Days";
            }
            tvScore.setText(scoreText);
            tvScore.setTextColor(Color.BLACK);
            tvScore.setTextSize(22);
            tvScore.setTypeface(null, android.graphics.Typeface.BOLD);

            RelativeLayout.LayoutParams scoreParams = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            scoreParams.addRule(RelativeLayout.CENTER_VERTICAL);
            scoreParams.addRule(RelativeLayout.START_OF, actionsContainer.getId());
            scoreParams.setMarginEnd((int) (16 * getResources().getDisplayMetrics().density));
            tvScore.setLayoutParams(scoreParams);

            relativeLayout.addView(tvScore);
        }

        return card;
    }

    private void onEditCardClicked(GradeRow r) {
        String activity = r.activity;
        String subject = r.subject.isEmpty() ? "General" : r.subject;

        if (activity.equalsIgnoreCase("Program Performance")) {
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (16 * getResources().getDisplayMetrics().density);
            container.setPadding(pad, pad, pad, pad);

            TextView labelName = new TextView(this);
            labelName.setText("Program Name:");
            labelName.setTextSize(14);
            labelName.setTextColor(Color.BLACK);
            container.addView(labelName);

            EditText inputName = new EditText(this);
            inputName.setHint("e.g. Nutrition Month");
            inputName.setText(r.activityName != null ? r.activityName : "");
            container.addView(inputName);

            TextView labelScore = new TextView(this);
            labelScore.setText("\nScore:");
            labelScore.setTextSize(14);
            labelScore.setTextColor(Color.BLACK);
            container.addView(labelScore);

            Spinner spnScore = new Spinner(this);
            String[] scores = new String[]{"0/2", "1/2", "2/2"};
            ArrayAdapter<String> scoreAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, scores);
            scoreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spnScore.setAdapter(scoreAdapter);
            container.addView(spnScore);

            new AlertDialog.Builder(this)
                    .setTitle("Edit Program Performance")
                    .setView(container)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String progName = inputName.getText().toString().trim();
                        String selectedScore = spnScore.getSelectedItem().toString();
                        if (r.id > 0) {
                            dbHelper.updateGradeRecordScore(r.id, selectedScore);
                            dbHelper.saveOrUpdateGradeRecord(studentId, r.quarter, subject, activity, progName, selectedScore);
                        } else {
                            dbHelper.saveOrUpdateGradeRecord(studentId, getQuarterString(), subject, activity, progName, selectedScore);
                        }
                        Toast.makeText(this, "Saved: " + selectedScore, Toast.LENGTH_SHORT).show();
                        updateUiForActivity();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else if (activity.equalsIgnoreCase("Attendance")) {
            String cleanDigits = r.score != null ? r.score.replaceAll("[^0-9]", "").trim() : "0";
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
                        if (r.id > 0) {
                            dbHelper.updateGradeRecordScore(r.id, formatted);
                        } else {
                            dbHelper.saveOrUpdateGradeRecord(studentId, getQuarterString(), subject, activity, formatted);
                        }
                        Toast.makeText(this, "Attendance saved: " + formatted, Toast.LENGTH_SHORT).show();
                        updateUiForActivity();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else if (activity.equalsIgnoreCase("Exam")) {
            showExamScoreDialog(r.score, newScore -> {
                if (r.id > 0) {
                    dbHelper.updateGradeRecordScore(r.id, newScore);
                } else {
                    dbHelper.saveOrUpdateGradeRecord(studentId, getQuarterString(), subject, "Exam", newScore);
                }
                Toast.makeText(this, "Exam score saved: " + newScore, Toast.LENGTH_SHORT).show();
                updateUiForActivity();
            });
        } else {
            String[] ratings = new String[]{"No Star", "1 Star", "2 Stars", "3 Stars"};
            new AlertDialog.Builder(this)
                    .setTitle("Rate Grade Record")
                    .setItems(ratings, (dialog, which) -> {
                        String formattedScore = which + " ⭐ (" + which + " Stars)";
                        if (r.id > 0) {
                            dbHelper.updateGradeRecordScore(r.id, formattedScore);
                        } else {
                            dbHelper.saveOrUpdateGradeRecord(studentId, getQuarterString(), subject, activity, formattedScore);
                        }
                        Toast.makeText(this, "Saved: " + ((which == 0) ? "No stars" : (which + " star(s)")), Toast.LENGTH_SHORT).show();
                        updateUiForActivity();
                    })
                    .show();
        }
    }

    private void onDeleteCardClicked(GradeRow r) {
        if (r.id > 0) {
            confirmDelete(r, this::updateUiForActivity);
        } else {
            Toast.makeText(this, "No record saved yet to delete.", Toast.LENGTH_SHORT).show();
        }
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
