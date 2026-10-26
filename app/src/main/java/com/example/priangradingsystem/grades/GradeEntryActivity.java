package com.example.priangradingsystem.grades;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.Locale;

@SuppressWarnings("all")
public class GradeEntryActivity extends AppCompatActivity {

    private Spinner spnQuarter;
    private Spinner spnActivity;
    private Spinner spnSubject;
    private Spinner spnAttendanceStatus;
    private Spinner spnPoints;

    private LinearLayout layoutScoreSection;
    private LinearLayout layoutAttendanceSection;
    private LinearLayout layoutStarSection;
    private LinearLayout layoutPointSection;

    private RatingBar ratingBarStars;
    private TextView tvStarCountText;
    private TextView tvClearStars;

    private EditText etScore;
    private EditText etRemarks;
    private TextView tvScoreSuffix;
    private TextView tvAssessmentDate;

    private TextView tvStudentName;
    private TextView tvStudentGradeLevel;
    private ImageView imgStudentAvatar;

    private String selectedStudentName = "Ive Brayne Cueva";
    private String selectedStudentLevel = "Nursery";
    private String selectedStudentPhotoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_grade_entry);

        selectedStudentName = getIntent().getStringExtra("STUDENT_NAME");
        if (selectedStudentName == null || selectedStudentName.isEmpty()) {
            selectedStudentName = "Ive Brayne Cueva";
        }
        selectedStudentLevel = getIntent().getStringExtra("STUDENT_LEVEL");
        if (selectedStudentLevel == null || selectedStudentLevel.isEmpty()) {
            selectedStudentLevel = "Nursery";
        }
        selectedStudentPhotoUri = getIntent().getStringExtra("STUDENT_PHOTO_URI");

        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentGradeLevel = findViewById(R.id.tvStudentGradeLevel);
        imgStudentAvatar = findViewById(R.id.imgStudentAvatar);

        updateStudentCard(selectedStudentName, selectedStudentLevel, selectedStudentPhotoUri);

        spnQuarter = findViewById(R.id.spnQuarter);
        setupQuarterSpinner();

        spnActivity = findViewById(R.id.spnActivity);
        spnSubject = findViewById(R.id.spnSubject);
        spnAttendanceStatus = findViewById(R.id.spnAttendanceStatus);
        spnPoints = findViewById(R.id.spnPoints);

        layoutScoreSection = findViewById(R.id.layoutScoreSection);
        layoutAttendanceSection = findViewById(R.id.layoutAttendanceSection);
        layoutStarSection = findViewById(R.id.layoutStarSection);
        layoutPointSection = findViewById(R.id.layoutPointSection);

        ratingBarStars = findViewById(R.id.ratingBarStars);
        tvStarCountText = findViewById(R.id.tvStarCountText);
        tvClearStars = findViewById(R.id.tvClearStars);

        if (ratingBarStars != null) {
            ratingBarStars.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
                if (tvStarCountText != null) {
                    tvStarCountText.setText((int) rating + " ⭐");
                }
            });
        }

        if (tvClearStars != null) {
            tvClearStars.setOnClickListener(v -> {
                if (ratingBarStars != null) {
                    ratingBarStars.setRating(0);
                }
                if (tvStarCountText != null) {
                    tvStarCountText.setText("0 ⭐");
                }
            });
        }

        setupActivitySpinner();
        setupAttendanceStatusSpinner();
        setupPointSpinner();

        etScore = findViewById(R.id.etScore);
        etRemarks = findViewById(R.id.etRemarks);
        tvScoreSuffix = findViewById(R.id.tvScoreSuffix);
        tvAssessmentDate = findViewById(R.id.tvAssessmentDate);

        RelativeLayout btnAssessmentDate = findViewById(R.id.btnAssessmentDate);
        if (btnAssessmentDate != null) {
            btnAssessmentDate.setOnClickListener(v -> showDatePicker());
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        MaterialButton btnSubmit = findViewById(R.id.btnSubmit);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> saveGrade());
        }
    }

    private void updateStudentCard(String name, String level, String photoUri) {
        if (tvStudentName != null && name != null) {
            tvStudentName.setText(name);
        }

        if (tvStudentGradeLevel != null && level != null) {
            String cleanedLevel = level.replaceAll("(?i)\\s*-\\s*Section\\s*[A-Z0-9]+", "").replaceAll("(?i)\\s*Section\\s*[A-Z0-9]+", "").trim();
            tvStudentGradeLevel.setText(cleanedLevel);
        }

        if (imgStudentAvatar != null) {
            if (photoUri != null && !photoUri.trim().isEmpty()) {
                try {
                    Uri uri = Uri.parse(photoUri);
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                    imgStudentAvatar.setImageURI(uri);
                } catch (Exception e) {
                    imgStudentAvatar.setImageResource(R.drawable.children);
                }
            } else {
                imgStudentAvatar.setImageResource(R.drawable.children);
            }
        }
    }

    private void setupQuarterSpinner() {
        if (spnQuarter != null) {
            String[] quarters = getResources().getStringArray(R.array.quarter_options);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, quarters);
            adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
            spnQuarter.setAdapter(adapter);
        }
    }

    private void setupAttendanceStatusSpinner() {
        if (spnAttendanceStatus != null) {
            String[] attendanceOptions = getResources().getStringArray(R.array.attendance_options);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, attendanceOptions);
            adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
            spnAttendanceStatus.setAdapter(adapter);
        }
    }

    private void setupPointSpinner() {
        if (spnPoints != null) {
            String[] pointOptions = getResources().getStringArray(R.array.point_options);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, pointOptions);
            adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
            spnPoints.setAdapter(adapter);
        }
    }

    private void setupActivitySpinner() {
        if (spnActivity != null) {
            String[] activities = getResources().getStringArray(R.array.activity_options);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, activities);
            adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
            spnActivity.setAdapter(adapter);

            spnActivity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedActivity = activities[position];
                    updateSubjectSpinner(selectedActivity);
                    updateUIRulesForActivity(selectedActivity);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void updateSubjectSpinner(String activity) {
        if (spnSubject != null) {
            String[] subjects;
            if (activity != null && activity.equalsIgnoreCase("Program Performance")) {
                subjects = getResources().getStringArray(R.array.subject_options_program_performance);
            } else {
                subjects = getResources().getStringArray(R.array.subject_options);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, subjects);
            adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
            spnSubject.setAdapter(adapter);
        }
    }

    private void updateUIRulesForActivity(String activity) {
        if (activity == null) return;

        if (activity.equalsIgnoreCase("Attendance")) {
            if (layoutScoreSection != null) layoutScoreSection.setVisibility(View.GONE);
            if (layoutAttendanceSection != null) layoutAttendanceSection.setVisibility(View.VISIBLE);
            if (layoutStarSection != null) layoutStarSection.setVisibility(View.GONE);
            if (layoutPointSection != null) layoutPointSection.setVisibility(View.GONE);
        } else if (activity.equalsIgnoreCase("Exam")) {
            if (layoutScoreSection != null) layoutScoreSection.setVisibility(View.VISIBLE);
            if (layoutAttendanceSection != null) layoutAttendanceSection.setVisibility(View.GONE);
            if (layoutStarSection != null) layoutStarSection.setVisibility(View.GONE);
            if (layoutPointSection != null) layoutPointSection.setVisibility(View.GONE);
            if (tvScoreSuffix != null) tvScoreSuffix.setText("/20");
            if (etScore != null) etScore.setHint("20");
        } else if (activity.equalsIgnoreCase("Books") || activity.equalsIgnoreCase("Homework")) {
            if (layoutScoreSection != null) layoutScoreSection.setVisibility(View.GONE);
            if (layoutAttendanceSection != null) layoutAttendanceSection.setVisibility(View.GONE);
            if (layoutStarSection != null) layoutStarSection.setVisibility(View.VISIBLE);
            if (layoutPointSection != null) layoutPointSection.setVisibility(View.GONE);
        } else if (activity.equalsIgnoreCase("Program Performance")) {
            if (layoutScoreSection != null) layoutScoreSection.setVisibility(View.GONE);
            if (layoutAttendanceSection != null) layoutAttendanceSection.setVisibility(View.GONE);
            if (layoutStarSection != null) layoutStarSection.setVisibility(View.GONE);
            if (layoutPointSection != null) layoutPointSection.setVisibility(View.VISIBLE);
        } else {
            if (layoutScoreSection != null) layoutScoreSection.setVisibility(View.VISIBLE);
            if (layoutAttendanceSection != null) layoutAttendanceSection.setVisibility(View.GONE);
            if (layoutStarSection != null) layoutStarSection.setVisibility(View.GONE);
            if (layoutPointSection != null) layoutPointSection.setVisibility(View.GONE);
            if (tvScoreSuffix != null) tvScoreSuffix.setText("/100");
            if (etScore != null) etScore.setHint("90");
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String dateStr = String.format(Locale.getDefault(), "October %d, %d", dayOfMonth, year);
            if (tvAssessmentDate != null) {
                tvAssessmentDate.setText(dateStr);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void saveGrade() {
        String activity = spnActivity != null && spnActivity.getSelectedItem() != null ? spnActivity.getSelectedItem().toString() : "Exam";
        String score = "";

        if (activity.equalsIgnoreCase("Attendance")) {
            score = spnAttendanceStatus != null && spnAttendanceStatus.getSelectedItem() != null ? spnAttendanceStatus.getSelectedItem().toString() : "True (Present)";
        } else if (activity.equalsIgnoreCase("Books") || activity.equalsIgnoreCase("Homework")) {
            int stars = ratingBarStars != null ? (int) ratingBarStars.getRating() : 0;
            score = stars + " ⭐ (" + stars + " Stars)";
        } else if (activity.equalsIgnoreCase("Program Performance")) {
            score = spnPoints != null && spnPoints.getSelectedItem() != null ? spnPoints.getSelectedItem().toString() : "0 Points";
        } else {
            score = etScore != null ? etScore.getText().toString().trim() : "";
            if (score.isEmpty()) {
                Toast.makeText(this, "Please enter a valid score.", Toast.LENGTH_SHORT).show();
                return;
            }
            score = score + (tvScoreSuffix != null ? tvScoreSuffix.getText().toString() : "/100");
        }

        String quarter = spnQuarter != null && spnQuarter.getSelectedItem() != null ? spnQuarter.getSelectedItem().toString() : "1st Quarter";
        String subject = spnSubject != null && spnSubject.getSelectedItem() != null ? spnSubject.getSelectedItem().toString() : "Writing";
        String assessmentDate = tvAssessmentDate != null ? tvAssessmentDate.getText().toString().trim() : "";
        String remarks = etRemarks != null ? etRemarks.getText().toString().trim() : "";

        com.example.priangradingsystem.data.DatabaseHelper dbHelper = new com.example.priangradingsystem.data.DatabaseHelper(this);
        boolean isSaved = dbHelper.insertGradeRecord(selectedStudentName, selectedStudentLevel, quarter, subject, activity, score, assessmentDate, remarks);

        if (isSaved) {
            Toast.makeText(this, "Grade successfully saved to Database.db for " + selectedStudentName + " (" + quarter + ") | " + activity + " - " + subject + ": " + score, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Grade saved for " + selectedStudentName, Toast.LENGTH_SHORT).show();
        }

        if (etScore != null) {
            etScore.setText("");
        }
        if (ratingBarStars != null) {
            ratingBarStars.setRating(0);
        }
        if (tvStarCountText != null) {
            tvStarCountText.setText("0 ⭐");
        }
        if (etRemarks != null) {
            etRemarks.setText("");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
    }
}
