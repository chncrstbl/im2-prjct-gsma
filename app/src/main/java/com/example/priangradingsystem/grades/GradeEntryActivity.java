package com.example.priangradingsystem.grades;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.home.MainActivity;
import com.example.priangradingsystem.students.StudentsActivity;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.Locale;

@SuppressWarnings("all")
public class GradeEntryActivity extends AppCompatActivity {

    private Spinner spnQuarter;
    private Spinner spnSubject;
    private EditText etScore;
    private TextView tvAssessmentDate;

    private TextView tvStudentName;
    private TextView tvStudentGradeLevel;
    private ImageView imgStudentAvatar;

    private String selectedStudentName = "Ive Brayne Cueva";
    private String selectedStudentLevel = "Nursery - Section A";
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
            selectedStudentLevel = "Nursery - Section A";
        }
        selectedStudentPhotoUri = getIntent().getStringExtra("STUDENT_PHOTO_URI");

        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentGradeLevel = findViewById(R.id.tvStudentGradeLevel);
        imgStudentAvatar = findViewById(R.id.imgStudentAvatar);

        updateStudentCard(selectedStudentName, selectedStudentLevel, selectedStudentPhotoUri);

        spnQuarter = findViewById(R.id.spnQuarter);
        setupQuarterSpinner();

        spnSubject = findViewById(R.id.spnSubject);
        setupSubjectSpinner();

        etScore = findViewById(R.id.etScore);
        tvAssessmentDate = findViewById(R.id.tvAssessmentDate);

        RelativeLayout btnAssessmentDate = findViewById(R.id.btnAssessmentDate);
        if (btnAssessmentDate != null) {
            btnAssessmentDate.setOnClickListener(v -> showDatePicker());
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvSave = findViewById(R.id.tvSave);
        if (tvSave != null) {
            tvSave.setOnClickListener(v -> saveGrade());
        }

        MaterialButton btnSubmit = findViewById(R.id.btnSubmit);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> saveGrade());
        }

        setupBottomNavigation();
    }

    private void updateStudentCard(String name, String level, String photoUri) {
        if (tvStudentName != null && name != null) {
            tvStudentName.setText(name);
        }

        if (tvStudentGradeLevel != null && level != null) {
            tvStudentGradeLevel.setText(level);
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

    private void setupSubjectSpinner() {
        if (spnSubject != null) {
            String[] subjects = getResources().getStringArray(R.array.subject_options);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, subjects);
            adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
            spnSubject.setAdapter(adapter);
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
        String score = etScore != null ? etScore.getText().toString().trim() : "";
        String quarter = spnQuarter != null && spnQuarter.getSelectedItem() != null ? spnQuarter.getSelectedItem().toString() : "1st Quarter";
        String subject = spnSubject != null && spnSubject.getSelectedItem() != null ? spnSubject.getSelectedItem().toString() : "Color Recognition";

        if (score.isEmpty()) {
            Toast.makeText(this, "Please enter a valid score.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Grade saved for " + selectedStudentName + " (" + quarter + ") | " + subject + ": " + score + "/100", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(GradeEntryActivity.this, GradeSummaryActivity.class);
        if (selectedStudentName != null) intent.putExtra("STUDENT_NAME", selectedStudentName);
        if (selectedStudentLevel != null) intent.putExtra("STUDENT_LEVEL", selectedStudentLevel);
        if (selectedStudentPhotoUri != null) intent.putExtra("STUDENT_PHOTO_URI", selectedStudentPhotoUri);
        intent.putExtra("SCORE", score);
        intent.putExtra("QUARTER", quarter);
        intent.putExtra("SUBJECT", subject);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        TextView navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        TextView navStudents = findViewById(R.id.navStudents);
        if (navStudents != null) {
            navStudents.setOnClickListener(v -> {
                Intent intent = new Intent(this, StudentsActivity.class);
                startActivity(intent);
            });
        }

        TextView navAnalytics = findViewById(R.id.navAnalytics);
        if (navAnalytics != null) {
            navAnalytics.setOnClickListener(v -> {
                Intent intent = new Intent(this, GradeSummaryActivity.class);
                if (selectedStudentName != null) intent.putExtra("STUDENT_NAME", selectedStudentName);
                if (selectedStudentLevel != null) intent.putExtra("STUDENT_LEVEL", selectedStudentLevel);
                if (selectedStudentPhotoUri != null) intent.putExtra("STUDENT_PHOTO_URI", selectedStudentPhotoUri);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
    }
}
