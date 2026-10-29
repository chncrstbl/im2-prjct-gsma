package com.example.priangradingsystem.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.data.DatabaseHelper;
import com.example.priangradingsystem.home.MainActivity;
import com.example.priangradingsystem.model.Teacher;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private DatabaseHelper dbHelper;
    private boolean isPasswordVisible = false;

    // Set to true to temporarily bypass login validation for fast testing
    private static final boolean BYPASS_LOGIN = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        ImageView ivEyeToggle = findViewById(R.id.ivEyeToggle);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);

        if (ivEyeToggle != null && etPassword != null) {
            ivEyeToggle.setOnClickListener(v -> togglePasswordVisibility());
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> handleLogin());
        }
    }

    private void togglePasswordVisibility() {
        if (etPassword == null) return;
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            isPasswordVisible = false;
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            isPasswordVisible = true;
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void handleLogin() {
        String username = etUsername != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword != null ? etPassword.getText().toString().trim() : "";

        if (!BYPASS_LOGIN) {
            if (TextUtils.isEmpty(username)) {
                if (etUsername != null) {
                    etUsername.setError("Username is required");
                    etUsername.requestFocus();
                }
                return;
            }

            if (TextUtils.isEmpty(password)) {
                if (etPassword != null) {
                    etPassword.setError("Password is required");
                    etPassword.requestFocus();
                }
                return;
            }
        }

        Teacher teacher = dbHelper.getTeacherByCredentials(username, password);
        if (teacher != null) {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            prefs.edit()
                    .putInt("TEACHER_ID", teacher.getTeacherId())
                    .putString("TEACHER_NAME", teacher.getFullName())
                    .putString("TEACHER_USERNAME", teacher.getUsername())
                    .putBoolean("IS_LOGGED_IN", true)
                    .apply();

            Toast.makeText(this, "Welcome back, " + teacher.getFullName() + "!", Toast.LENGTH_SHORT).show();
        } else {
            if (!BYPASS_LOGIN) {
                Toast.makeText(this, "Invalid username or password.", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, "Login bypassed for testing.", Toast.LENGTH_SHORT).show();
        }

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
