package com.example.priangradingsystem.reports;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.data.DatabaseHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/** Database-driven quarterly report. No hardcoded sample grades. */
public class ReportsActivity extends AppCompatActivity {
    private static final String TAG = "ReportsActivity";
    private static final List<String> SUBJECTS = Arrays.asList(
            "Writing", "Math", "Reading", "Life Skills", "PE & Arts");

    private TextView tvTotalScore, tvOverallPercentage, tvWritingScore, tvMathScore,
            tvReadingScore, tvLifeSkillsScore, tvPEArtsScore;
    private ProgressBar progressBarPerformance;
    private DatabaseHelper dbHelper;
    private long studentId = -1;
    private String studentName = "";
    private String selectedQuarter = "1st Quarter";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_reports);
        com.example.priangradingsystem.ui.SchoolYearBar.attach(this, new com.example.priangradingsystem.data.DatabaseHelper(this), false, false, null);
        try { dbHelper = new DatabaseHelper(this); }
        catch (Throwable t) { Log.e(TAG, "Could not open the database", t); dbHelper = null; }

        studentName = safe(getIntent().getStringExtra("STUDENT_NAME"));
        studentId = getIntent().getLongExtra("STUDENT_ID", -1L);
        if (studentId <= 0) {
            int oldId = getIntent().getIntExtra("STUDENT_ID", -1);
            if (oldId > 0) studentId = oldId;
        }

        TextView tvStudentName = findViewById(R.id.tvStudentName);
        TextView tvStudentGradeLevel = findViewById(R.id.tvStudentGradeLevel);
        ImageView imgStudentAvatar = findViewById(R.id.imgStudentAvatar);
        tvTotalScore = findViewById(R.id.tvTotalScore);
        tvOverallPercentage = findViewById(R.id.tvOverallPercentage);
        progressBarPerformance = findViewById(R.id.progressBarPerformance);
        tvWritingScore = findViewById(R.id.tvWritingScore);
        tvMathScore = findViewById(R.id.tvMathScore);
        tvReadingScore = findViewById(R.id.tvReadingScore);
        tvLifeSkillsScore = findViewById(R.id.tvLifeSkillsScore);
        tvPEArtsScore = findViewById(R.id.tvPEArtsScore);

        if (tvStudentName != null) tvStudentName.setText(studentName.isEmpty() ? "Student" : studentName);
        String level = safe(getIntent().getStringExtra("STUDENT_LEVEL"));
        if (tvStudentGradeLevel != null && !level.isEmpty()) tvStudentGradeLevel.setText(level);
        String photo = getIntent().getStringExtra("STUDENT_PHOTO_URI");
        if (imgStudentAvatar != null && photo != null && !photo.trim().isEmpty()) {
            try {
                Uri uri = Uri.parse(photo);
                imgStudentAvatar.setImageURI(uri);
            } catch (Throwable e) { Log.w(TAG, "Could not load student photo", e); }
        }
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (studentId <= 0) studentId = findStudentIdByName(studentName);
        RelativeLayout layoutQuarterDropdown = findViewById(R.id.layoutQuarterDropdown);
        TextView tvSelectedQuarter = findViewById(R.id.tvSelectedQuarter);
        if (layoutQuarterDropdown != null && tvSelectedQuarter != null) {
            String[] quarters = {"1st Quarter", "2nd Quarter", "3rd Quarter"};
            tvSelectedQuarter.setText(selectedQuarter);
            layoutQuarterDropdown.setOnClickListener(v -> {
                showCustomDropdown(v, tvSelectedQuarter, quarters, qtr -> {
                    tvSelectedQuarter.setText(qtr);
                    selectedQuarter = qtr;
                    loadQuarterData();
                });
            });
        }
        loadQuarterData();
    }

    interface OnItemSelectedListener {
        void onItemSelected(String item);
    }

    private void showCustomDropdown(View anchor, TextView targetTextView, String[] items, OnItemSelectedListener listener) {
        String currentSelected = targetTextView != null ? targetTextView.getText().toString() : "";

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

        android.widget.ListPopupWindow popupWindow = new android.widget.ListPopupWindow(this);
        popupWindow.setAdapter(adapter);
        popupWindow.setAnchorView(anchor);
        popupWindow.setBackgroundDrawable(androidx.core.content.res.ResourcesCompat.getDrawable(getResources(), R.drawable.dropdown_box, null));
        popupWindow.setWidth(anchor.getWidth());
        popupWindow.setHeight(android.widget.ListPopupWindow.WRAP_CONTENT);
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
            popupWindow.dismiss();
        });
        popupWindow.show();
    }

    private long findStudentIdByName(String name) {
        if (dbHelper == null || name == null || name.trim().isEmpty()) return -1;
        Cursor c = null;
        try {
            c = dbHelper.getAllStudents();
            if (c == null) return -1;
            String wanted = normalizeName(name);
            while (c.moveToNext()) {
                int idCol = c.getColumnIndex("student_id");
                int firstCol = c.getColumnIndex("first_name");
                int lastCol = c.getColumnIndex("last_name");
                if (idCol < 0 || firstCol < 0 || lastCol < 0) continue;
                String first = safe(c.getString(firstCol));
                String last = safe(c.getString(lastCol));
                if (normalizeName(last + ", " + first).equals(wanted)
                        || normalizeName(first + " " + last).equals(wanted)) return c.getLong(idCol);
            }
        } catch (Exception e) { Log.e(TAG, "Could not resolve student ID", e); }
        finally { if (c != null) c.close(); }
        return -1;
    }

    /** Never lets a data/database problem crash the screen: shows a message and logs the stack trace instead. */
    private void loadQuarterData() {
        try {
            loadQuarterDataUnsafe();
        } catch (Throwable t) {
            Log.e(TAG, "Report failed for quarter=" + selectedQuarter, t);
            showNoData("Could not build the report (" + t.getClass().getSimpleName() + "). See Logcat tag ReportsActivity.");
        }
    }

    private void loadQuarterDataUnsafe() {
        if (dbHelper == null) {
            showNoData("The database could not be opened.");
            return;
        }
        if (studentId <= 0) {
            showNoData("Student ID was not passed. Reopen the report from the student list.");
            return;
        }

        List<QuarterGradeCalculator.GradeRow> records = new ArrayList<>();                       // selected student
        Map<Long, List<QuarterGradeCalculator.GradeRow>> allStudentsRows = new HashMap<>();      // every student, this quarter
        Cursor cursor = null;
        try {
            cursor = dbHelper.getAllGradeRecords();
            if (cursor == null) {
                showNoData("Could not read grade records.");
                return;
            }
            int idCol = cursor.getColumnIndex("student_id");
            int subjectCol = cursor.getColumnIndex("subject");
            int quarterCol = cursor.getColumnIndex("quarter");
            int scoreCol = cursor.getColumnIndex("score");
            int activityCol = cursor.getColumnIndex("activity");
            int activityNameCol = cursor.getColumnIndex("activity_name");
            int dateCol = cursor.getColumnIndex("date");
            int timeCol = cursor.getColumnIndex("time");   // submission time; -1 if the query does not select it
            int recordIdCol = cursor.getColumnIndex("record_id");
            if (idCol < 0 || recordIdCol < 0) {
                showNoData("grade_records is missing student_id/record_id columns.");
                return;
            }
            String wantedQuarter = DatabaseHelper.normalizeQuarter(selectedQuarter);
            while (cursor.moveToNext()) {
                String quarter = quarterCol >= 0 ? cursor.getString(quarterCol) : "";
                if (!DatabaseHelper.normalizeQuarter(quarter).equalsIgnoreCase(wantedQuarter)) continue;
                long rowStudentId = cursor.getLong(idCol);
                QuarterGradeCalculator.GradeRow row = new QuarterGradeCalculator.GradeRow(
                        cursor.getLong(recordIdCol),
                        subjectCol >= 0 ? cursor.getString(subjectCol) : "",
                        activityCol >= 0 ? cursor.getString(activityCol) : "",
                        activityNameCol >= 0 ? cursor.getString(activityNameCol) : "",
                        scoreCol >= 0 ? cursor.getString(scoreCol) : "",
                        dateCol >= 0 ? cursor.getString(dateCol) : "",
                        timeCol >= 0 ? cursor.getString(timeCol) : "");
                List<QuarterGradeCalculator.GradeRow> list = allStudentsRows.get(rowStudentId);
                if (list == null) { list = new ArrayList<>(); allStudentsRows.put(rowStudentId, list); }
                list.add(row);                                   // used for the class-wide highest points
                if (rowStudentId == studentId) records.add(row); // used for this student's own grade
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed loading report records for quarter=" + selectedQuarter, e);
            showNoData("There was a problem loading this quarter's grades. Check Logcat for details.");
            return;
        } finally { if (cursor != null) cursor.close(); }

        if (records.isEmpty()) {
            showNoData("No saved grade records found for " + selectedQuarter + ".");
            return;
        }

        // Books + Homework denominator = highest Books stars + highest Homework stars among ALL students.
        QuarterGradeCalculator.Benchmarks benchmarks = QuarterGradeCalculator.Benchmarks.fromStudents(allStudentsRows.values());
        QuarterGradeCalculator.Result result = QuarterGradeCalculator.calculate(records, -1, benchmarks);
        Log.d(TAG, "Grade trace (" + selectedQuarter + ", " + records.size() + " records, " + allStudentsRows.size() + " students)\n" + result.trace());

        for (String subject : SUBJECTS) setSubjectScore(subject, subjectDisplay(result.subjects.get(subject)));

        if (result.overallComplete) {
            String value = result.overallTransmuted >= 0 ? String.valueOf(result.overallTransmuted) : "N/A";
            if (tvTotalScore != null) tvTotalScore.setText(value);
            if (tvOverallPercentage != null) tvOverallPercentage.setText(value);
            if (progressBarPerformance != null) progressBarPerformance.setProgress(Math.max(result.overallTransmuted, 0));
        } else {
            // Overall stays "Incomplete" until all five subjects have exam + attendance + books/homework.
            if (tvTotalScore != null) tvTotalScore.setText("Incomplete");
            if (tvOverallPercentage != null) tvOverallPercentage.setText("Incomplete");
            if (progressBarPerformance != null) progressBarPerformance.setProgress(0);
        }
    }

    private String subjectDisplay(QuarterGradeCalculator.SubjectResult sd) {
        if (sd == null || sd.status == QuarterGradeCalculator.Status.NO_DATA) return "N/A";
        if (sd.transmuted >= 0) return String.valueOf(sd.transmuted);
        if (sd.adjustedInitial >= 0) return String.valueOf((int) Math.round(sd.adjustedInitial));
        return "N/A";
    }

    private void setSubjectScore(String subject, String value) {
        TextView view = null;
        switch (subject) {
            case "Writing": view = tvWritingScore; break;
            case "Math": view = tvMathScore; break;
            case "Reading": view = tvReadingScore; break;
            case "Life Skills": view = tvLifeSkillsScore; break;
            case "PE & Arts": view = tvPEArtsScore; break;
        }
        if (view != null) view.setText(value);
    }

    private void showNoData(String message) {
        if (tvTotalScore != null) tvTotalScore.setText("N/A");
        if (tvOverallPercentage != null) tvOverallPercentage.setText("N/A");
        if (progressBarPerformance != null) progressBarPerformance.setProgress(0);
        for (String subject : SUBJECTS) setSubjectScore(subject, "N/A");
        Log.w(TAG, message);
        try { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); } catch (Throwable ignored) { }
    }

    private static String normalizeName(String s) { return safe(s).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim(); }
    private static String safe(String s) { return s == null ? "" : s; }

    @Override protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
    }
}