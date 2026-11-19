package com.example.priangradingsystem.reports;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
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
import androidx.core.content.FileProvider;

import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.data.DatabaseHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
    private TextView tvPassedBadge, tvExcellentCount, tvGoodCount, tvNeedsImprovementCount;
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
        tvPassedBadge = findViewById(R.id.tvPassedBadge);
        tvExcellentCount = findViewById(R.id.tvExcellentCount);
        tvGoodCount = findViewById(R.id.tvGoodCount);
        tvNeedsImprovementCount = findViewById(R.id.tvNeedsImprovementCount);

        com.google.android.material.button.MaterialButton btnExportPdf = findViewById(R.id.btnExportPdf);
        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> exportReportToPdf());
        }

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
                view.setTextColor(view.getContext().getColor(R.color.text_strong));
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

        int excellentCount = 0;
        int goodCount = 0;
        int needsImprovementCount = 0;

        for (String subject : SUBJECTS) {
            String displayVal = subjectDisplay(result.subjects.get(subject));
            setSubjectScore(subject, displayVal);
            try {
                int val = Integer.parseInt(displayVal);
                if (val >= 90) excellentCount++;
                else if (val >= 75) goodCount++;
                else needsImprovementCount++;
            } catch (NumberFormatException e) {
                needsImprovementCount++;
            }
        }

        if (tvExcellentCount != null) tvExcellentCount.setText(String.valueOf(excellentCount));
        if (tvGoodCount != null) tvGoodCount.setText(String.valueOf(goodCount));
        if (tvNeedsImprovementCount != null) tvNeedsImprovementCount.setText(String.valueOf(needsImprovementCount));

        if (result.overallComplete) {
            String value = result.overallTransmuted >= 0 ? result.overallTransmuted + "%" : "N/A";
            if (tvTotalScore != null) tvTotalScore.setText(value);
            if (tvOverallPercentage != null) tvOverallPercentage.setText(value);
            if (progressBarPerformance != null) progressBarPerformance.setProgress(Math.max(result.overallTransmuted, 0));
            if (tvPassedBadge != null) {
                if (result.overallTransmuted >= 75) {
                    tvPassedBadge.setText("Passed");
                    tvPassedBadge.setTextColor(getColor(R.color.status_pass));
                    tvPassedBadge.setBackgroundResource(R.drawable.bg_badge_passed);
                } else {
                    tvPassedBadge.setText("Needs Improvement");
                    tvPassedBadge.setTextColor(getColor(R.color.status_fail));
                    tvPassedBadge.setBackgroundColor(getColor(R.color.badge_fail_bg));
                }
            }
        } else {
            // Overall stays "Incomplete" until all five subjects have exam + attendance + books/homework.
            if (tvTotalScore != null) tvTotalScore.setText("Incomplete");
            if (tvOverallPercentage != null) tvOverallPercentage.setText("Incomplete");
            if (progressBarPerformance != null) progressBarPerformance.setProgress(0);
            if (tvPassedBadge != null) {
                tvPassedBadge.setText("Incomplete");
                tvPassedBadge.setTextColor(getColor(R.color.status_fail));
                tvPassedBadge.setBackgroundColor(getColor(R.color.badge_fail_bg));
            }
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
        if (tvPassedBadge != null) tvPassedBadge.setText("N/A");
        if (tvExcellentCount != null) tvExcellentCount.setText("0");
        if (tvGoodCount != null) tvGoodCount.setText("0");
        if (tvNeedsImprovementCount != null) tvNeedsImprovementCount.setText("0");
        for (String subject : SUBJECTS) setSubjectScore(subject, "N/A");
        Log.w(TAG, message);
        try { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); } catch (Throwable ignored) { }
    }

    private void exportReportToPdf() {
        try {
            PdfDocument pdfDocument = new PdfDocument();
            // A4 size: 595 x 842 points
            int pageWidth = 595;
            int pageHeight = 842;
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Background
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, pageWidth, pageHeight, bgPaint);

            int margin = 36;
            int contentWidth = pageWidth - (margin * 2);
            int y = 40;

            // 1. Header Banner (Yellow Header Bar)
            Paint headerBgPaint = new Paint();
            headerBgPaint.setColor(Color.parseColor("#FFCC00"));
            canvas.drawRoundRect(new RectF(margin, y, margin + contentWidth, y + 54), 12, 12, headerBgPaint);

            Paint headerTextPaint = new Paint();
            headerTextPaint.setColor(Color.WHITE);
            headerTextPaint.setTextSize(20);
            headerTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("PRIAN GRADING SYSTEM", margin + 16, y + 34, headerTextPaint);

            Paint subTextPaint = new Paint();
            subTextPaint.setColor(Color.WHITE);
            subTextPaint.setTextSize(13);
            subTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            String reportTitle = "GRADE REPORT";
            float titleWidth = subTextPaint.measureText(reportTitle);
            canvas.drawText(reportTitle, margin + contentWidth - 16 - titleWidth, y + 34, subTextPaint);

            y += 70;

            // 2. Student Info Card
            Paint cardBg = new Paint();
            cardBg.setColor(Color.parseColor("#F9FAFB"));
            cardBg.setStyle(Paint.Style.FILL);

            Paint cardBorder = new Paint();
            cardBorder.setColor(Color.parseColor("#E5E7EB"));
            cardBorder.setStyle(Paint.Style.STROKE);
            cardBorder.setStrokeWidth(1.5f);

            RectF infoCardRect = new RectF(margin, y, margin + contentWidth, y + 76);
            canvas.drawRoundRect(infoCardRect, 10, 10, cardBg);
            canvas.drawRoundRect(infoCardRect, 10, 10, cardBorder);

            Paint labelPaint = new Paint();
            labelPaint.setColor(Color.parseColor("#6B7280"));
            labelPaint.setTextSize(11);
            labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            Paint valuePaint = new Paint();
            valuePaint.setColor(Color.BLACK);
            valuePaint.setTextSize(14);
            valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            // Student Name
            canvas.drawText("STUDENT NAME", margin + 16, y + 24, labelPaint);
            TextView tvName = findViewById(R.id.tvStudentName);
            String nameStr = tvName != null ? tvName.getText().toString() : studentName;
            canvas.drawText(nameStr.isEmpty() ? "Student" : nameStr, margin + 16, y + 44, valuePaint);

            // Grade Level
            TextView tvLevel = findViewById(R.id.tvStudentGradeLevel);
            String levelStr = tvLevel != null ? tvLevel.getText().toString() : "";
            if (!levelStr.isEmpty()) {
                Paint levelPaint = new Paint();
                levelPaint.setColor(Color.parseColor("#4B5563"));
                levelPaint.setTextSize(12);
                canvas.drawText(levelStr, margin + 16, y + 62, levelPaint);
            }

            // Quarter & Date on Right Side
            canvas.drawText("QUARTER", margin + contentWidth - 180, y + 24, labelPaint);
            canvas.drawText(selectedQuarter, margin + contentWidth - 180, y + 44, valuePaint);

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            String currentDate = dateFormat.format(new Date());
            canvas.drawText("EXPORTED: " + currentDate, margin + contentWidth - 180, y + 62, labelPaint);

            y += 90;

            // 3. Total Score Card
            RectF scoreCardRect = new RectF(margin, y, margin + contentWidth, y + 90);
            canvas.drawRoundRect(scoreCardRect, 10, 10, cardBg);
            canvas.drawRoundRect(scoreCardRect, 10, 10, cardBorder);

            canvas.drawText("TOTAL SCORE", margin + 16, y + 24, labelPaint);
            String scoreStr = tvTotalScore != null ? tvTotalScore.getText().toString() : "N/A";
            Paint bigScorePaint = new Paint();
            bigScorePaint.setColor(scoreStr.contains("Incomplete") || scoreStr.contains("N/A") ? Color.parseColor("#D92D20") : Color.parseColor("#00A859"));
            bigScorePaint.setTextSize(32);
            bigScorePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(scoreStr, margin + 16, y + 62, bigScorePaint);

            // Status Badge on right of score card
            String statusStr = tvPassedBadge != null ? tvPassedBadge.getText().toString() : "Passed";
            Paint badgeBg = new Paint();
            badgeBg.setColor(statusStr.equalsIgnoreCase("Passed") ? Color.parseColor("#E6F4EA") : Color.parseColor("#FEE4E2"));
            RectF badgeRect = new RectF(margin + contentWidth - 140, y + 28, margin + contentWidth - 16, y + 58);
            canvas.drawRoundRect(badgeRect, 15, 15, badgeBg);

            Paint badgeTextPaint = new Paint();
            badgeTextPaint.setColor(statusStr.equalsIgnoreCase("Passed") ? Color.parseColor("#00A859") : Color.parseColor("#D92D20"));
            badgeTextPaint.setTextSize(13);
            badgeTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            float bw = badgeTextPaint.measureText(statusStr);
            canvas.drawText(statusStr, badgeRect.centerX() - (bw / 2), y + 47, badgeTextPaint);

            y += 104;

            // 4. Overall Performance Card
            RectF perfCardRect = new RectF(margin, y, margin + contentWidth, y + 100);
            canvas.drawRoundRect(perfCardRect, 10, 10, cardBg);
            canvas.drawRoundRect(perfCardRect, 10, 10, cardBorder);

            canvas.drawText("OVERALL PERFORMANCE", margin + 16, y + 24, labelPaint);
            String perfStr = tvOverallPercentage != null ? tvOverallPercentage.getText().toString() : "N/A";
            canvas.drawText("Overall: " + perfStr, margin + 16, y + 46, valuePaint);

            String excellentStr = tvExcellentCount != null ? tvExcellentCount.getText().toString() : "0";
            String goodStr = tvGoodCount != null ? tvGoodCount.getText().toString() : "0";
            String needsImpStr = tvNeedsImprovementCount != null ? tvNeedsImprovementCount.getText().toString() : "0";

            Paint legendPaint = new Paint();
            legendPaint.setColor(Color.parseColor("#374151"));
            legendPaint.setTextSize(12);

            Paint dotPaint = new Paint();

            // Excellent dot
            dotPaint.setColor(Color.parseColor("#00A859"));
            canvas.drawCircle(margin + 16, y + 68, 4, dotPaint);
            canvas.drawText("Excellent (90-100%): " + excellentStr + " subjects", margin + 26, y + 72, legendPaint);

            // Good dot
            dotPaint.setColor(Color.parseColor("#FFD600"));
            canvas.drawCircle(margin + 200, y + 68, 4, dotPaint);
            canvas.drawText("Good (75-89%): " + goodStr + " subjects", margin + 210, y + 72, legendPaint);

            // Needs Imp dot
            dotPaint.setColor(Color.parseColor("#D92D20"));
            canvas.drawCircle(margin + 360, y + 68, 4, dotPaint);
            canvas.drawText("Needs Imp: " + needsImpStr, margin + 370, y + 72, legendPaint);

            y += 114;

            // 5. Subject Breakdown Section
            Paint secTitlePaint = new Paint();
            secTitlePaint.setColor(Color.BLACK);
            secTitlePaint.setTextSize(15);
            secTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("SUBJECT BREAKDOWN", margin, y + 16, secTitlePaint);

            y += 26;

            Paint tableHeaderBg = new Paint();
            tableHeaderBg.setColor(Color.parseColor("#E5E7EB"));
            RectF thRect = new RectF(margin, y, margin + contentWidth, y + 26);
            canvas.drawRoundRect(thRect, 6, 6, tableHeaderBg);

            Paint thTextPaint = new Paint();
            thTextPaint.setColor(Color.parseColor("#374151"));
            thTextPaint.setTextSize(11);
            thTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            canvas.drawText("SUBJECT", margin + 12, y + 18, thTextPaint);
            canvas.drawText("GRADE / SCORE", margin + contentWidth - 120, y + 18, thTextPaint);

            y += 26;

            String[][] subjectScores = {
                    {"Writing", tvWritingScore != null ? tvWritingScore.getText().toString() : "N/A"},
                    {"Math", tvMathScore != null ? tvMathScore.getText().toString() : "N/A"},
                    {"Reading", tvReadingScore != null ? tvReadingScore.getText().toString() : "N/A"},
                    {"Life Skills", tvLifeSkillsScore != null ? tvLifeSkillsScore.getText().toString() : "N/A"},
                    {"PE & Arts", tvPEArtsScore != null ? tvPEArtsScore.getText().toString() : "N/A"}
            };

            Paint rowTextPaint = new Paint();
            rowTextPaint.setColor(Color.parseColor("#1F2937"));
            rowTextPaint.setTextSize(13);
            rowTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            Paint rowDivider = new Paint();
            rowDivider.setColor(Color.parseColor("#E5E7EB"));
            rowDivider.setStrokeWidth(1f);

            for (String[] row : subjectScores) {
                canvas.drawText(row[0], margin + 12, y + 22, rowTextPaint);

                Paint scoreColorPaint = new Paint();
                scoreColorPaint.setTextSize(13);
                scoreColorPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                String valStr = row[1];
                if (valStr.contains("N/A") || valStr.contains("Incomplete")) {
                    scoreColorPaint.setColor(Color.parseColor("#6B7280"));
                } else {
                    try {
                        int scoreVal = Integer.parseInt(valStr.replace("%", "").trim());
                        if (scoreVal >= 90) scoreColorPaint.setColor(Color.parseColor("#00A859"));
                        else if (scoreVal >= 75) scoreColorPaint.setColor(Color.parseColor("#FFA000"));
                        else scoreColorPaint.setColor(Color.parseColor("#D92D20"));
                    } catch (Exception e) {
                        scoreColorPaint.setColor(Color.parseColor("#1F2937"));
                    }
                }

                canvas.drawText(valStr, margin + contentWidth - 120, y + 22, scoreColorPaint);
                canvas.drawLine(margin, y + 32, margin + contentWidth, y + 32, rowDivider);
                y += 34;
            }

            // Footer
            Paint footerPaint = new Paint();
            footerPaint.setColor(Color.parseColor("#9CA3AF"));
            footerPaint.setTextSize(10);
            String footerText = "Generated by Prian Grading System • " + currentDate;
            float fw = footerPaint.measureText(footerText);
            canvas.drawText(footerText, (pageWidth / 2f) - (fw / 2f), pageHeight - 30, footerPaint);

            pdfDocument.finishPage(page);

            // Save PDF File to Public Documents Directory
            String safeName = (studentName.isEmpty() ? "Student" : studentName).replaceAll("[^a-zA-Z0-9_]", "_");
            String safeQuarter = selectedQuarter.replaceAll("[^a-zA-Z0-9_]", "_");
            String fileName = "Grade_Report_" + safeName + "_" + safeQuarter + ".pdf";

            Uri savedUri = null;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOCUMENTS);

                android.content.ContentResolver resolver = getContentResolver();
                savedUri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), values);
                if (savedUri != null) {
                    try (java.io.OutputStream os = resolver.openOutputStream(savedUri)) {
                        pdfDocument.writeTo(os);
                    }
                }
            }

            if (savedUri == null) {
                File docsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS);
                if (docsDir != null && !docsDir.exists()) {
                    boolean created = docsDir.mkdirs();
                    if (!created) Log.w(TAG, "Could not create directory: " + docsDir);
                }
                if (docsDir == null || !docsDir.exists()) {
                    docsDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);
                    if (docsDir != null && !docsDir.exists()) {
                        boolean created = docsDir.mkdirs();
                        if (!created) Log.w(TAG, "Could not create directory: " + docsDir);
                    }
                }
                File targetFile = new File(docsDir, fileName);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(targetFile)) {
                    pdfDocument.writeTo(fos);
                }
                savedUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", targetFile);
            }

            pdfDocument.close();

            Toast.makeText(this, "PDF saved to Documents folder!", Toast.LENGTH_LONG).show();

            // Open PDF with intent
            if (savedUri != null) {
                try {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.setDataAndType(savedUri, "application/pdf");
                    viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(viewIntent, "Open PDF Grade Report"));
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "No PDF viewer found on device", e);
                }
            }

        } catch (Throwable t) {
            Log.e(TAG, "Failed to export PDF report", t);
            Toast.makeText(this, "Could not export PDF (" + t.getMessage() + ")", Toast.LENGTH_SHORT).show();
        }
    }

    private static String normalizeName(String s) { return safe(s).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim(); }
    private static String safe(String s) { return s == null ? "" : s; }

    @Override protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
    }
}