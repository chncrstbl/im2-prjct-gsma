package com.example.priangradingsystem.records;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.google.android.material.button.MaterialButton;

public class RecordsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_records);

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

        MaterialButton btnDownloadPdf = findViewById(R.id.btnDownloadPdf);
        if (btnDownloadPdf != null) {
            String nameForReport = (studentName != null && !studentName.isEmpty()) ? studentName : "Student";
            btnDownloadPdf.setOnClickListener(v ->
                Toast.makeText(this, "Downloading PDF report for " + nameForReport + "...", Toast.LENGTH_SHORT).show()
            );
        }

        MaterialButton btnShare = findViewById(R.id.btnShare);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                String nameForShare = (studentName != null && !studentName.isEmpty()) ? studentName : "Vea Arevalo";
                String levelForShare = (studentLevel != null && !studentLevel.isEmpty()) ? studentLevel : "Nursery";

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Student Records - " + nameForShare);
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Student Records for " + nameForShare + " (" + levelForShare + ")\nOverall Performance: 90% Passed");
                startActivity(Intent.createChooser(shareIntent, "Share Records via"));
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
    }
}
