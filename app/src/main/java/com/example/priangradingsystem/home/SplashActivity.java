package com.example.priangradingsystem.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.R;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Button btnGetStarted = findViewById(R.id.btnGetStarted);
        if (btnGetStarted != null) {
            btnGetStarted.setOnClickListener(v -> proceedToMain());
        }
    }

    private void proceedToMain() {
        if (!isFinishing()) {
            android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            boolean hasAccount = prefs.getBoolean("HAS_ACCOUNT", false);
            Intent intent;
            if (!hasAccount) {
                intent = new Intent(SplashActivity.this, com.example.priangradingsystem.auth.SignUpActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, com.example.priangradingsystem.auth.LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }
    }
}
