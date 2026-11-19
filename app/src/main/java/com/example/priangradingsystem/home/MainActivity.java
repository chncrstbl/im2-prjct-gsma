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
        playEntranceAnimation(savedInstanceState == null);
    }

    /** Cards pop in one after another; the banner follows. */
    private void playEntranceAnimation(boolean firstLaunch) {
        if (!firstLaunch) return;
        int[] ids = {R.id.btnStudentsCard, R.id.btnGradesCard, R.id.btnReportsCard, R.id.btnAnalyticsCard};
        float rise = 24 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < ids.length; i++) {
            View card = findViewById(ids[i]);
            if (card == null) continue;
            card.setAlpha(0f);
            card.setScaleX(0.9f);
            card.setScaleY(0.9f);
            card.setTranslationY(rise);
            card.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                    .setStartDelay(80L + i * 70L).setDuration(380)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f)).start();
        }
        View sun = findViewById(R.id.ivDecorSun);
        if (sun != null) {
            sun.animate().rotationBy(360f).setDuration(1400)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
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
