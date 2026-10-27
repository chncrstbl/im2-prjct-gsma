package com.example.priangradingsystem.grades;

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

import java.util.Locale;

@SuppressWarnings("all")
public class GradeEntryActivity extends AppCompatActivity {

    private Spinner spnQuarter;
    private Spinner spnActivity;
    private Spinner spnSubject;
    private Spinner spnAttendanceStatus;
    private Spinner spnPoints;

    private LinearLayout layoutSubjectSection;
    private LinearLayout layoutProgramNameSection;
    private LinearLayout layoutScoreSection;
    private LinearLayout layoutAttendanceSection;
    private LinearLayout layoutStarSection;
    private LinearLayout layoutPointSection;

    private RatingBar ratingBarStars;
    private TextView tvStarCountText;
    private TextView tvClearStars;

    private EditText etProgramName;
    private EditText etScore;
    private EditText etRemarks;
    private TextView tvScoreSuffix;
    private TextView tvAssessmentDate;

    private TextView tvStudentName;
    private TextView tvStudentGradeLevel;
    private TextView tvNoPhotoText;
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
        tvNoPhotoText = findViewById(R.id.tvNoPhotoText);
        imgStudentAvatar = findViewById(R.id.imgStudentAvatar);

        updateStudentCard(selectedStudentName, selectedStudentLevel, selectedStudentPhotoUri);

        spnQuarter = findViewById(R.id.spnQuarter);
        setupQuarterSpinner();

        spnActivity = findViewById(R.id.spnActivity);
        spnSubject = findViewById(R.id.spnSubject);
        spnAttendanceStatus = findViewById(R.id.spnAttendanceStatus);
        spnPoints = findViewById(R.id.spnPoints);

        layoutSubjectSection = findViewById(R.id.layoutSubjectSection);
        layoutProgramNameSection = findViewById(R.id.layoutProgramNameSection);
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

        etProgramName = findViewById(R.id.etProgramName);
        etScore = findViewById(R.id.etScore);
        etRemarks = findViewById(R.id.etRemarks);
        tvScoreSuffix = findViewById(R.id.tvScoreSuffix);
        tvAssessmentDate = findViewById(R.id.tvAssessmentDate);

        // The date is always today's date (not selectable).
        if (tvAssessmentDate != null) {
            tvAssessmentDate.setText(getTodayDate());
        }
        RelativeLayout btnAssessmentDate = findViewById(R.id.btnAssessmentDate);
        if (btnAssessmentDate != null) {
            btnAssessmentDate.setOnClickListener(null);
            btnAssessmentDate.setClickable(false);
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
            boolean hasPhoto = false;
            if (photoUri != null && !photoUri.trim().isEmpty()) {
                try {
                    Uri uri = Uri.parse(photoUri);
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                    imgStudentAvatar.setImageURI(uri);
                    imgStudentAvatar.setVisibility(View.VISIBLE);
                    hasPhoto = true;
                } catch (Exception e) {
                    hasPhoto = false;
                }
            }

            if (!hasPhoto) {
                imgStudentAvatar.setImageDrawable(null);
                imgStudentAvatar.setVisibility(View.GONE);
                if (tvNoPhotoText != null) {
                    tvNoPhotoText.setVisibility(View.VISIBLE);
                }
            } else {
                if (tvNoPhotoText != null) {
                    tvNoPhotoText.setVisibility(View.GONE);
                }
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
            if (layoutSubjectSection != null) layoutSubjectSection.setVisibility(View.GONE);
            if (layoutProgramNameSection != null) layoutProgramNameSection.setVisibility(View.GONE);
            if (layoutScoreSection != null) layoutScoreSection.setVisibility(View.GONE);
            if (layoutAttendanceSection != null) layoutAttendanceSection.setVisibility(View.VISIBLE);
            if (layoutStarSection != null) layoutStarSection.setVisibility(View.GONE);
            if (layoutPointSection != null) layoutPointSection.setVisibility(View.GONE);
        } else if (activity.equalsIgnoreCase("Exam")) {
            if (layoutSubjectSection != null) layoutSubjectSection.setVisibility(View.VISIBLE);
            if (layoutProgramNameSection != null) layoutProgramNameSection.setVisibility(View.GONE);
            if (layoutScoreSection != null) layoutScoreSection.setVisibility(View.VISIBLE);
            if (layoutAttendanceSection != null) layoutAttendanceSection.setVisibility(View.GONE);
            if (layoutStarSection != null) layoutStarSection.setVisibility(View.GONE);
            if (layoutPointSection != null) layoutPointSection.setVisibility(View.GONE);
            if (tvScoreSuffix != null) tvScoreSuffix.setText("/20");
            if (etScore != null) {
                etScore.setHint("20");
                etScore.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2)});
            }
        } else if (activity.equalsIgnoreCase("Books") || activity.equalsIgnoreCase("Homework")) {
            if (layoutSubjectSection != null) layoutSubjectSection.setVisibility(View.VISIBLE);
            if (layoutProgramNameSection != null) layoutProgramNameSection.setVisibility(View.GONE);
            if (layoutScoreSection != null) layoutScoreSection.setVisibility(View.GONE);
            if (layoutAttendanceSection != null) layoutAttendanceSection.setVisibility(View.GONE);
            if (layoutStarSection != null) layoutStarSection.setVisibility(View.VISIBLE);
            if (layoutPointSection != null) layoutPointSection.setVisibility(View.GONE);
        } else if (activity.equalsIgnoreCase("Program Performance")) {
            if (layoutSubjectSection != null) layoutSubjectSection.setVisibility(View.VISIBLE);
            if (layoutProgramNameSection != null) layoutProgramNameSection.setVisibility(View.VISIBLE);
            if (layoutScoreSection != null) layoutScoreSection.setVisibility(View.GONE);
            if (layoutAttendanceSection != null) layoutAttendanceSection.setVisibility(View.GONE);
            if (layoutStarSection != null) layoutStarSection.setVisibility(View.GONE);
            if (layoutPointSection != null) layoutPointSection.setVisibility(View.VISIBLE);
        } else {
            if (layoutSubjectSection != null) layoutSubjectSection.setVisibility(View.VISIBLE);
            if (layoutProgramNameSection != null) layoutProgramNameSection.setVisibility(View.GONE);
            if (layoutScoreSection != null) layoutScoreSection.setVisibility(View.VISIBLE);
            if (layoutAttendanceSection != null) layoutAttendanceSection.setVisibility(View.GONE);
            if (layoutStarSection != null) layoutStarSection.setVisibility(View.GONE);
            if (layoutPointSection != null) layoutPointSection.setVisibility(View.GONE);
            if (tvScoreSuffix != null) tvScoreSuffix.setText("/100");
            if (etScore != null) {
                etScore.setHint("90");
                etScore.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(3)});
            }
        }
    }

    private String getTodayDate() {
        return new java.text.SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(new java.util.Date());
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
            String rawScore = etScore != null ? etScore.getText().toString().trim() : "";
            if (rawScore.isEmpty()) {
                Toast.makeText(this, "Please enter a valid score.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (activity.equalsIgnoreCase("Exam")) {
                try {
                    int examVal = Integer.parseInt(rawScore);
                    if (examVal < 0 || examVal > 20) {
                        Toast.makeText(this, "Exam score must be between 0 and 20 only.", Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid number between 0 and 20.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            score = rawScore + (tvScoreSuffix != null ? tvScoreSuffix.getText().toString() : "/100");
        }

        String quarter = spnQuarter != null && spnQuarter.getSelectedItem() != null ? spnQuarter.getSelectedItem().toString() : "1st Quarter";
        String subject = "General";
        if (!activity.equalsIgnoreCase("Attendance") && spnSubject != null && spnSubject.getSelectedItem() != null) {
            subject = spnSubject.getSelectedItem().toString();
        }
        String assessmentDate = getTodayDate();
        String remarks = etRemarks != null ? etRemarks.getText().toString().trim() : "";

        String activityName = "";
        if (activity.equalsIgnoreCase("Program Performance")) {
            activityName = etProgramName != null ? etProgramName.getText().toString().trim() : "";
            if (activityName.isEmpty()) {
                Toast.makeText(this, "Please enter a program name.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        com.example.priangradingsystem.data.DatabaseHelper dbHelper = new com.example.priangradingsystem.data.DatabaseHelper(this);
        boolean isSaved = dbHelper.insertGradeRecord(selectedStudentName, selectedStudentLevel, quarter, subject, activity, activityName, score, assessmentDate, remarks);

        if (!isSaved) {
            Toast.makeText(this, "Failed to save grade: " + dbHelper.getLastError(), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Grade saved for " + selectedStudentName + " (" + quarter + ") | " + activity + (activity.equalsIgnoreCase("Attendance") ? "" : " - " + subject) + ": " + score, Toast.LENGTH_LONG).show();

        if (etScore != null) {
            etScore.setText("");
        }
        if (etProgramName != null) {
            etProgramName.setText("");
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
