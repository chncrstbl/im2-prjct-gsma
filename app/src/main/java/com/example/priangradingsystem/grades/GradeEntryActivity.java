package com.example.priangradingsystem.grades;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

@SuppressWarnings("all")
public class GradeEntryActivity extends AppCompatActivity {

    private RelativeLayout layoutQuarterDropdown;
    private TextView tvSelectedQuarter;

    private RelativeLayout layoutActivityDropdown;
    private TextView tvSelectedActivity;

    private RelativeLayout layoutSubjectDropdown;
    private TextView tvSelectedSubject;

    private RelativeLayout layoutAttendanceStatusDropdown;
    private TextView tvSelectedAttendanceStatus;

    private RelativeLayout layoutPointDropdown;
    private TextView tvSelectedPoints;

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

    private String selectedStudentName = "";
    private long selectedStudentId = -1;
    private String selectedStudentLevel = "Nursery";
    private String selectedStudentPhotoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_grade_entry);
        com.example.priangradingsystem.ui.SchoolYearBar.attach(this, new com.example.priangradingsystem.data.DatabaseHelper(this), false, false, null);

        selectedStudentName = getIntent().getStringExtra("STUDENT_NAME");
        if (selectedStudentName == null) selectedStudentName = "";
        selectedStudentId = getIntent().getLongExtra("STUDENT_ID", -1L);
        if (selectedStudentId <= 0) {
            int oldId = getIntent().getIntExtra("STUDENT_ID", -1);
            if (oldId > 0) selectedStudentId = oldId;
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

        layoutQuarterDropdown = findViewById(R.id.layoutQuarterDropdown);
        tvSelectedQuarter = findViewById(R.id.tvSelectedQuarter);
        setupQuarterDropdown();

        layoutActivityDropdown = findViewById(R.id.layoutActivityDropdown);
        tvSelectedActivity = findViewById(R.id.tvSelectedActivity);
        setupActivityDropdown();

        layoutSubjectDropdown = findViewById(R.id.layoutSubjectDropdown);
        tvSelectedSubject = findViewById(R.id.tvSelectedSubject);
        setupSubjectDropdown();

        layoutAttendanceStatusDropdown = findViewById(R.id.layoutAttendanceStatusDropdown);
        tvSelectedAttendanceStatus = findViewById(R.id.tvSelectedAttendanceStatus);
        setupAttendanceStatusDropdown();

        layoutPointDropdown = findViewById(R.id.layoutPointDropdown);
        tvSelectedPoints = findViewById(R.id.tvSelectedPoints);
        setupPointDropdown();

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

    private void setupQuarterDropdown() {
        if (layoutQuarterDropdown != null && tvSelectedQuarter != null) {
            String[] quarters = getResources().getStringArray(R.array.quarter_options);
            layoutQuarterDropdown.setOnClickListener(v -> {
                showCustomDropdown(v, tvSelectedQuarter, quarters, selectedQuarter -> {
                    tvSelectedQuarter.setText(selectedQuarter);
                });
            });
        }
    }

    private void setupAttendanceStatusDropdown() {
        if (layoutAttendanceStatusDropdown != null && tvSelectedAttendanceStatus != null) {
            String[] attendanceOptions = getResources().getStringArray(R.array.attendance_options);
            layoutAttendanceStatusDropdown.setOnClickListener(v -> {
                showCustomDropdown(v, tvSelectedAttendanceStatus, attendanceOptions, selectedStatus -> {
                    tvSelectedAttendanceStatus.setText(selectedStatus);
                });
            });
        }
    }

    private void setupPointDropdown() {
        if (layoutPointDropdown != null && tvSelectedPoints != null) {
            String[] pointOptions = getResources().getStringArray(R.array.point_options);
            layoutPointDropdown.setOnClickListener(v -> {
                showCustomDropdown(v, tvSelectedPoints, pointOptions, selectedPoint -> {
                    tvSelectedPoints.setText(selectedPoint);
                });
            });
        }
    }

    private void setupActivityDropdown() {
        if (layoutActivityDropdown != null && tvSelectedActivity != null) {
            String[] activities = getResources().getStringArray(R.array.activity_options);
            if (activities.length > 0) {
                String current = tvSelectedActivity.getText().toString();
                if (current.isEmpty()) {
                    tvSelectedActivity.setText(activities[0]);
                    current = activities[0];
                }
                updateSubjectList(current);
                updateUIRulesForActivity(current);
            }

            layoutActivityDropdown.setOnClickListener(v -> {
                showCustomDropdown(v, tvSelectedActivity, activities, selectedActivity -> {
                    tvSelectedActivity.setText(selectedActivity);
                    updateSubjectList(selectedActivity);
                    updateUIRulesForActivity(selectedActivity);
                });
            });
        }
    }

    private void setupSubjectDropdown() {
        if (layoutSubjectDropdown != null && tvSelectedSubject != null) {
            layoutSubjectDropdown.setOnClickListener(v -> {
                String activity = tvSelectedActivity != null ? tvSelectedActivity.getText().toString() : "";
                String[] subjects;
                if (activity.equalsIgnoreCase("Program Performance")) {
                    subjects = getResources().getStringArray(R.array.subject_options_program_performance);
                } else {
                    subjects = getResources().getStringArray(R.array.subject_options);
                }
                showCustomDropdown(v, tvSelectedSubject, subjects, selectedSubject -> {
                    tvSelectedSubject.setText(selectedSubject);
                });
            });
        }
    }

    private void updateSubjectList(String activity) {
        if (tvSelectedSubject != null) {
            String[] subjects;
            if (activity != null && activity.equalsIgnoreCase("Program Performance")) {
                subjects = getResources().getStringArray(R.array.subject_options_program_performance);
            } else {
                subjects = getResources().getStringArray(R.array.subject_options);
            }
            String current = tvSelectedSubject.getText().toString();
            boolean found = false;
            for (String s : subjects) {
                if (s.equalsIgnoreCase(current)) {
                    found = true;
                    break;
                }
            }
            if (!found && subjects.length > 0) {
                tvSelectedSubject.setText(subjects[0]);
            }
        }
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

        ListPopupWindow popupWindow = new ListPopupWindow(this);
        popupWindow.setAdapter(adapter);
        popupWindow.setAnchorView(anchor);
        popupWindow.setBackgroundDrawable(androidx.core.content.res.ResourcesCompat.getDrawable(getResources(), R.drawable.dropdown_box, null));
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
            popupWindow.dismiss();
        });
        popupWindow.show();
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
            if (activity.equalsIgnoreCase("Homework")) {
                if (layoutSubjectSection != null) layoutSubjectSection.setVisibility(View.GONE);
            } else {
                if (layoutSubjectSection != null) layoutSubjectSection.setVisibility(View.VISIBLE);
            }
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
        String activity = tvSelectedActivity != null ? tvSelectedActivity.getText().toString() : "Exam";
        String score = "";

        if (activity.equalsIgnoreCase("Attendance")) {
            String attStatus = tvSelectedAttendanceStatus != null ? tvSelectedAttendanceStatus.getText().toString() : "Present";
            score = attStatus.toLowerCase().contains("absent") ? "absent" : "present";
        } else if (activity.equalsIgnoreCase("Books") || activity.equalsIgnoreCase("Homework")) {
            int stars = ratingBarStars != null ? (int) ratingBarStars.getRating() : 0;
            score = stars + " ⭐ (" + stars + " Stars)";
        } else if (activity.equalsIgnoreCase("Program Performance")) {
            score = tvSelectedPoints != null ? tvSelectedPoints.getText().toString() : "0 Points";
        } else {
            String rawScore = etScore != null ? etScore.getText().toString().trim() : "";
            if (rawScore.isEmpty()) {
                Toast.makeText(this, "Please enter a valid score.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (activity.equalsIgnoreCase("Exam")) {
                try {
                    int examVal = Integer.parseInt(rawScore);
                    if (examVal < 1 || examVal > 20) {
                        Toast.makeText(this, "Exam score must be a whole number from 1 to 20.", Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a whole number from 1 to 20.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            score = rawScore + (tvScoreSuffix != null ? tvScoreSuffix.getText().toString() : "/100");
        }

        String quarter = tvSelectedQuarter != null ? tvSelectedQuarter.getText().toString() : "1st Quarter";
        String subject = "N/A";
        if (!activity.equalsIgnoreCase("Attendance") && !activity.equalsIgnoreCase("Homework")) {
            if (tvSelectedSubject != null) {
                subject = tvSelectedSubject.getText().toString();
            }
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

        com.example.priangradingsystem.data.DatabaseHelper dbHelper =
                new com.example.priangradingsystem.data.DatabaseHelper(this);

        boolean isSaved;

        // Prefer the student's id (two students can share a name); fall back to the name.
        long studentIdToSave = selectedStudentId > 0 ? selectedStudentId
                : dbHelper.getStudentIdByName(selectedStudentName);
        if (studentIdToSave <= 0) {
            Toast.makeText(this, "No student selected. Go back and choose a student first.", Toast.LENGTH_LONG).show();
            return;
        }

        if (activity.equalsIgnoreCase("Attendance")) {
            isSaved = dbHelper.saveAttendanceStatus(
                    studentIdToSave,
                    assessmentDate,
                    score,
                    quarter,
                    subject,
                    remarks
            );

        } else {
            isSaved = dbHelper.addGradeRecord(
                    studentIdToSave,
                    quarter,
                    subject,
                    activity,
                    activityName,
                    score,
                    assessmentDate,
                    remarks
            );
        }

        if (!isSaved) {
            Toast.makeText(this, "Failed to save grade: " + dbHelper.getLastError(), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Grade saved for " + selectedStudentName + " (" + quarter + ") | " + activity + (activity.equalsIgnoreCase("Attendance") || activity.equalsIgnoreCase("Homework") ? "" : " - " + subject) + ": " + score, Toast.LENGTH_LONG).show();

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