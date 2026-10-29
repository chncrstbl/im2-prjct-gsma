package com.example.priangradingsystem.reports;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.data.DatabaseHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        dbHelper = new DatabaseHelper(this);

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
            } catch (Exception e) { Log.w(TAG, "Could not load student photo", e); }
        }
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (studentId <= 0) studentId = findStudentIdByName(studentName);
        Spinner spinnerQuarter = findViewById(R.id.spinnerQuarter);
        if (spinnerQuarter != null) {
            String[] quarters = {"1st Quarter", "2nd Quarter", "3rd Quarter"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, quarters);
            adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
            spinnerQuarter.setAdapter(adapter);
            spinnerQuarter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedQuarter = quarters[position];
                    loadQuarterData();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) { }
            });
        } else {
            loadQuarterData();
        }
    }

    private long findStudentIdByName(String name) {
        if (name == null || name.trim().isEmpty()) return -1;
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

    private void loadQuarterData() {
        if (studentId <= 0) {
            showNoData("Student ID was not passed. Reopen the report from the student list.");
            return;
        }

        List<QuarterGradeCalculator.GradeRow> records = new ArrayList<>();
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
                if (cursor.getLong(idCol) != studentId) continue;
                String quarter = quarterCol >= 0 ? cursor.getString(quarterCol) : "";
                if (!DatabaseHelper.normalizeQuarter(quarter).equalsIgnoreCase(wantedQuarter)) continue;
                records.add(new QuarterGradeCalculator.GradeRow(
                        cursor.getLong(recordIdCol),
                        subjectCol >= 0 ? cursor.getString(subjectCol) : "",
                        activityCol >= 0 ? cursor.getString(activityCol) : "",
                        activityNameCol >= 0 ? cursor.getString(activityNameCol) : "",
                        scoreCol >= 0 ? cursor.getString(scoreCol) : "",
                        dateCol >= 0 ? cursor.getString(dateCol) : "",
                        timeCol >= 0 ? cursor.getString(timeCol) : ""));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed loading report records for quarter=" + selectedQuarter, e);
            showNoData("There was a problem loading this quarter's grades. Check Logcat for details.");
            return;
        } finally { if (cursor != null) cursor.close(); }

        if (records.isEmpty()) {
            showNoData("No saved grade records found for " + selectedQuarter + ".");
            return;
        }

        QuarterGradeCalculator.Result result = QuarterGradeCalculator.calculate(records);
        Log.d(TAG, "Grade trace (" + selectedQuarter + ", " + records.size() + " records)\n" + result.trace());

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
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private static String normalizeName(String s) { return safe(s).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim(); }
    private static String safe(String s) { return s == null ? "" : s; }

    @Override protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
    }
}