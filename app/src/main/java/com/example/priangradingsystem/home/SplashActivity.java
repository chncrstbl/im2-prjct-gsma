package com.example.priangradingsystem.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.auth.LoginActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 3000;
    private Handler splashHandler;
    private Runnable splashRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashRunnable = this::proceedToLogin;
        splashHandler = new Handler(Looper.getMainLooper());
        splashHandler.postDelayed(splashRunnable, SPLASH_DELAY_MS);

        Button btnGetStarted = findViewById(R.id.btnGetStarted);
        if (btnGetStarted != null) {
            btnGetStarted.setOnClickListener(v -> proceedToLogin());
        }
    }

    private void proceedToLogin() {
        if (splashHandler != null && splashRunnable != null) {
            splashHandler.removeCallbacks(splashRunnable);
        }
        if (!isFinishing()) {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}