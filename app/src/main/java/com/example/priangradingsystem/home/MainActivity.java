package com.example.priangradingsystem.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.grades.SelectStudentGradeActivity;
import com.example.priangradingsystem.records.StudentRecordsActivity;
import com.example.priangradingsystem.reports.SelectStudentsActivity;
import com.example.priangradingsystem.students.StudentsActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("DARK_MODE_ENABLED", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_main);

        View btnStudentsCard = findViewById(R.id.btnStudentsCard);
        if (btnStudentsCard != null) {
            btnStudentsCard.setOnClickListener(v -> openStudentsActivity());
        }

        View btnGradesCard = findViewById(R.id.btnGradesCard);
        if (btnGradesCard != null) {
            btnGradesCard.setOnClickListener(v -> openGradesActivity());
        }

        View btnReportsCard = findViewById(R.id.btnReportsCard);
        if (btnReportsCard != null) {
            btnReportsCard.setOnClickListener(v -> openRecordsActivity());
        }

        View btnAnalyticsCard = findViewById(R.id.btnAnalyticsCard);
        if (btnAnalyticsCard != null) {
            btnAnalyticsCard.setOnClickListener(v -> openReportsActivity());
        }

        View ivProfile = findViewById(R.id.ivProfile);
        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> openAccountSettingsActivity());
        }
        updateProfileAvatar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);

        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("DARK_MODE_ENABLED", false);
        int targetMode = isDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        if (androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() != targetMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode);
            recreate();
            return;
        }
        updateProfileAvatar();
    }

    private void updateProfileAvatar() {
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String profileUri = prefs.getString("TEACHER_PROFILE_URI", "");
        android.widget.ImageView ivProfile = findViewById(R.id.ivProfile);
        if (ivProfile != null) {
            if (!android.text.TextUtils.isEmpty(profileUri)) {
                try {
                    ivProfile.setImageURI(android.net.Uri.parse(profileUri));
                } catch (Exception e) {
                    ivProfile.setImageResource(R.drawable.profile_teacher);
                }
            } else {
                ivProfile.setImageResource(R.drawable.profile_teacher);
            }
        }
    }

    private void openAccountSettingsActivity() {
        Intent intent = new Intent(MainActivity.this, com.example.priangradingsystem.account.AccountSettingsActivity.class);
        startActivity(intent);
    }

    private void openStudentsActivity() {
        Intent intent = new Intent(MainActivity.this, StudentsActivity.class);
        startActivity(intent);
    }

    private void openGradesActivity() {
        Intent intent = new Intent(MainActivity.this, SelectStudentGradeActivity.class);
        startActivity(intent);
    }

    private void openRecordsActivity() {
        Intent intent = new Intent(MainActivity.this, StudentRecordsActivity.class);
        startActivity(intent);
    }

    private void openReportsActivity() {
        Intent intent = new Intent(MainActivity.this, SelectStudentsActivity.class);
        startActivity(intent);
    }
}
