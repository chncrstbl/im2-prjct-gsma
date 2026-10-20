package com.example.priangradingsystem.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.grades.GradeEntryActivity;
import com.example.priangradingsystem.records.RecordsActivity;
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
            btnAnalyticsCard.setOnClickListener(v -> openRecordsActivity());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
    }

    private void openStudentsActivity() {
        Intent intent = new Intent(MainActivity.this, StudentsActivity.class);
        startActivity(intent);
    }

    private void openGradesActivity() {
        Intent intent = new Intent(MainActivity.this, GradeEntryActivity.class);
        startActivity(intent);
    }

    private void openRecordsActivity() {
        Intent intent = new Intent(MainActivity.this, RecordsActivity.class);
        startActivity(intent);
    }
}