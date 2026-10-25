package com.example.priangradingsystem.reports;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;

public class ReportsActivity extends AppCompatActivity {

    private TextView tvTotalScore, tvOverallPercentage, tvWritingScore, tvMathScore, tvReadingScore, tvLifeSkillsScore, tvPEArtsScore;
    private ProgressBar progressBarPerformance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_reports);

        String studentName = getIntent().getStringExtra("STUDENT_NAME");
        String studentLevel = getIntent().getStringExtra("STUDENT_LEVEL");
        String studentPhotoUri = getIntent().getStringExtra("STUDENT_PHOTO_URI");

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

        if (studentName != null && !studentName.trim().isEmpty() && tvStudentName != null) {
            tvStudentName.setText(studentName);
        } else if (tvStudentName != null) {
            tvStudentName.setText("Vea Arevalo");
        }

        if (studentLevel != null && !studentLevel.trim().isEmpty() && tvStudentGradeLevel != null) {
            tvStudentGradeLevel.setText(studentLevel);
        } else if (tvStudentGradeLevel != null) {
            tvStudentGradeLevel.setText("Nursery");
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

        Spinner spinnerQuarter = findViewById(R.id.spinnerQuarter);
        if (spinnerQuarter != null) {
            String[] quarters = new String[]{"Quarter 1", "Quarter 2", "Quarter 3"};
            ArrayAdapter<String> quarterAdapter = new ArrayAdapter<>(
                    this,
                    R.layout.item_spinner,
                    quarters
            );
            quarterAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
            spinnerQuarter.setAdapter(quarterAdapter);

            spinnerQuarter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateQuarterData(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void updateQuarterData(int quarterIndex) {
        if (quarterIndex == 0) { // Quarter 1
            if (tvTotalScore != null) tvTotalScore.setText("91%");
            if (tvOverallPercentage != null) tvOverallPercentage.setText("91%");
            if (progressBarPerformance != null) progressBarPerformance.setProgress(91);

            if (tvWritingScore != null) tvWritingScore.setText("92%");
            if (tvMathScore != null) tvMathScore.setText("85%");
            if (tvReadingScore != null) tvReadingScore.setText("88%");
            if (tvLifeSkillsScore != null) tvLifeSkillsScore.setText("97%");
            if (tvPEArtsScore != null) tvPEArtsScore.setText("95%");
        } else if (quarterIndex == 1) { // Quarter 2
            if (tvTotalScore != null) tvTotalScore.setText("89%");
            if (tvOverallPercentage != null) tvOverallPercentage.setText("89%");
            if (progressBarPerformance != null) progressBarPerformance.setProgress(89);

            if (tvWritingScore != null) tvWritingScore.setText("90%");
            if (tvMathScore != null) tvMathScore.setText("88%");
            if (tvReadingScore != null) tvReadingScore.setText("86%");
            if (tvLifeSkillsScore != null) tvLifeSkillsScore.setText("92%");
            if (tvPEArtsScore != null) tvPEArtsScore.setText("89%");
        } else if (quarterIndex == 2) { // Quarter 3
            if (tvTotalScore != null) tvTotalScore.setText("94%");
            if (tvOverallPercentage != null) tvOverallPercentage.setText("94%");
            if (progressBarPerformance != null) progressBarPerformance.setProgress(94);

            if (tvWritingScore != null) tvWritingScore.setText("95%");
            if (tvMathScore != null) tvMathScore.setText("92%");
            if (tvReadingScore != null) tvReadingScore.setText("94%");
            if (tvLifeSkillsScore != null) tvLifeSkillsScore.setText("98%");
            if (tvPEArtsScore != null) tvPEArtsScore.setText("93%");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
    }
}
