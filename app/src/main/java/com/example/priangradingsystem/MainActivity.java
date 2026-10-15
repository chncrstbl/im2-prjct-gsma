package com.example.priangradingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

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
    }

    private void openStudentsActivity() {
        Intent intent = new Intent(MainActivity.this, StudentsActivity.class);
        startActivity(intent);
    }
}