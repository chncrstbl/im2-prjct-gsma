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

import java.util.Calendar;
import java.util.Locale;

@SuppressWarnings("all")
public class GradeEntryActivity extends AppCompatActivity {

    private TextView tvAssessmentDate;
    private Spinner spnSubject;
    private EditText etScore;

    private String studentName;
    private String studentLevel;
    private String studentPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_grade_entry);

        studentName = getIntent().getStringExtra("STUDENT_NAME");
        studentLevel = getIntent().getStringExtra("STUDENT_LEVEL");
        studentPhotoUri = getIntent().getStringExtra("STUDENT_PHOTO_URI");

        TextView tvStudentName = findViewById(R.id.tvStudentName);
        TextView tvStudentGradeLevel = findViewById(R.id.tvStudentGradeLevel);
        ImageView imgStudentAvatar = findViewById(R.id.imgStudentAvatar);

        if (studentName != null && !studentName.trim().isEmpty() && tvStudentName != null) {
            tvStudentName.setText(studentName);
        }

        if (studentLevel != null && !studentLevel.trim().isEmpty() && tvStudentGradeLevel != null) {
            tvStudentGradeLevel.setText(studentLevel);
        }

        if (studentPhotoUri != null && !studentPhotoUri.trim().isEmpty() && imgStudentAvatar != null) {
            try {
                Uri uri = Uri.parse(studentPhotoUri);
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                imgStudentAvatar.setImageURI(uri);
            } catch (Exception e) {
                imgStudentAvatar.setImageResource(R.drawable.children);
            }
        }

        spnSubject = findViewById(R.id.spnSubject);
        if (spnSubject != null) {
            String[] subjects = getResources().getStringArray(R.array.subject_options);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, subjects);
            adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
            spnSubject.setAdapter(adapter);
        }

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

        setupBottomNavigation();
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
        String subject = spnSubject != null && spnSubject.getSelectedItem() != null ? spnSubject.getSelectedItem().toString() : "Color Recognition";

        if (score.isEmpty()) {
            Toast.makeText(this, "Please enter a valid score.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Grade saved for " + (studentName != null ? studentName : "Student") + " | " + subject + ": " + score + "/100", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(GradeEntryActivity.this, GradeSummaryActivity.class);
        if (studentName != null) intent.putExtra("STUDENT_NAME", studentName);
        if (studentLevel != null) intent.putExtra("STUDENT_LEVEL", studentLevel);
        if (studentPhotoUri != null) intent.putExtra("STUDENT_PHOTO_URI", studentPhotoUri);
        intent.putExtra("SCORE", score);
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
                if (studentName != null) intent.putExtra("STUDENT_NAME", studentName);
                if (studentLevel != null) intent.putExtra("STUDENT_LEVEL", studentLevel);
                if (studentPhotoUri != null) intent.putExtra("STUDENT_PHOTO_URI", studentPhotoUri);
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
