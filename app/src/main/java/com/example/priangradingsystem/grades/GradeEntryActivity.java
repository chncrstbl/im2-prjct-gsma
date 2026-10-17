package com.example.priangradingsystem.grades;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.records.RecordsActivity;
import com.google.android.material.button.MaterialButton;

public class GradeEntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grade_summary);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        MaterialButton btnViewFullRecords = findViewById(R.id.btnViewFullRecords);
        if (btnViewFullRecords != null) {
            btnViewFullRecords.setOnClickListener(v -> {
                Intent intent = new Intent(GradeEntryActivity.this, RecordsActivity.class);
                startActivity(intent);
            });
        }
    }
}
