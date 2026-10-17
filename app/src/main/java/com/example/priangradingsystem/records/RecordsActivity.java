package com.example.priangradingsystem.records;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.R;
import com.google.android.material.button.MaterialButton;

public class RecordsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        MaterialButton btnDownloadPdf = findViewById(R.id.btnDownloadPdf);
        if (btnDownloadPdf != null) {
            btnDownloadPdf.setOnClickListener(v ->
                Toast.makeText(this, "Downloading PDF report...", Toast.LENGTH_SHORT).show()
            );
        }

        MaterialButton btnShare = findViewById(R.id.btnShare);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Student Records - Vea Arevalo");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Student Records for Vea Arevalo (Nursery)\nOverall Performance: 90% Passed");
                startActivity(Intent.createChooser(shareIntent, "Share Records via"));
            });
        }
    }
}
