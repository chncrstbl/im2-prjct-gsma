package com.example.priangradingsystem.grades;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.reports.ReportsActivity;
import com.google.android.material.button.MaterialButton;

public class GradeSummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_grade_summary);

        long studentId = getIntent().getLongExtra("STUDENT_ID", -1);
        String studentName = getIntent().getStringExtra("STUDENT_NAME");
        String studentLevel = getIntent().getStringExtra("STUDENT_LEVEL");
        String studentPhotoUri = getIntent().getStringExtra("STUDENT_PHOTO_URI");

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

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        MaterialButton btnViewFullRecords = findViewById(R.id.btnViewFullRecords);
        if (btnViewFullRecords != null) {
            btnViewFullRecords.setOnClickListener(v -> {
                Intent intent = new Intent(GradeSummaryActivity.this, ReportsActivity.class);
                if (studentId > 0) intent.putExtra("STUDENT_ID", studentId);
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
