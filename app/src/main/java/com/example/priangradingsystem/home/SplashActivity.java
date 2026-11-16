package com.example.priangradingsystem.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.priangradingsystem.R;
import com.example.priangradingsystem.auth.LoginActivity;
import com.example.priangradingsystem.auth.SignUpActivity;
import com.example.priangradingsystem.data.DatabaseHelper;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean hasAccount = prefs.getBoolean("HAS_ACCOUNT", false) || dbHelper.hasAnyTeacher();

        if (hasAccount) {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_splash);

        Button btnGetStarted = findViewById(R.id.btnGetStarted);
        if (btnGetStarted != null) {
            btnGetStarted.setOnClickListener(v -> proceedToMain());
        }
    }

    private void proceedToMain() {
        if (!isFinishing()) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            boolean hasAccount = prefs.getBoolean("HAS_ACCOUNT", false) || dbHelper.hasAnyTeacher();
            Intent intent;
            if (!hasAccount) {
                intent = new Intent(SplashActivity.this, SignUpActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }
    }
}
