package com.example.priangradingsystem.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.grades.GradeEntryActivity;
import com.example.priangradingsystem.students.StudentsActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStudentsCard = findViewById(R.id.btnStudentsCard);
        if (btnStudentsCard != null) {
            btnStudentsCard.setOnClickListener(v -> openStudentsActivity());
        }

        TextView navStudents = findViewById(R.id.navStudents);
        if (navStudents != null) {
            navStudents.setOnClickListener(v -> openStudentsActivity());
        }

        Button btnGradesCard = findViewById(R.id.btnGradesCard);
        if (btnGradesCard != null) {
            btnGradesCard.setOnClickListener(v -> openGradesActivity());
        }

        TextView navGrades = findViewById(R.id.navGrades);
        if (navGrades != null) {
            navGrades.setOnClickListener(v -> openGradesActivity());
        }
    }

    private void openStudentsActivity() {
        Intent intent = new Intent(MainActivity.this, StudentsActivity.class);
        startActivity(intent);
    }

    private void openGradesActivity() {
        Intent intent = new Intent(MainActivity.this, GradeEntryActivity.class);
        startActivity(intent);
    }
}
