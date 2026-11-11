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

    // Set to false to activate login validation against database credentials
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

        android.widget.TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        }

        android.widget.TextView tvSignUpPrompt = findViewById(R.id.tvSignUpPrompt);
        if (tvSignUpPrompt != null) {
            tvSignUpPrompt.setOnClickListener(v -> {
                if (!isFinishing()) {
                    Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                    startActivity(intent);
                }
            });
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

    private void showForgotPasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etForgotUsername = new EditText(this);
        etForgotUsername.setHint("Enter your Username");
        etForgotUsername.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(etForgotUsername);

        final EditText etPresetCode = new EditText(this);
        etPresetCode.setHint("Enter your Preset Code");
        etPresetCode.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(etPresetCode);

        final EditText etNewPassword = new EditText(this);
        etNewPassword.setHint("Enter New Password");
        etNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPassword);

        final EditText etConfirmNewPassword = new EditText(this);
        etConfirmNewPassword.setHint("Confirm New Password");
        etConfirmNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirmNewPassword);

        builder.setView(layout);

        builder.setPositiveButton("Reset", (dialog, which) -> {
            String username = etForgotUsername.getText().toString().trim();
            String presetCode = etPresetCode.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmNewPassword.getText().toString().trim();

            if (TextUtils.isEmpty(username)) {
                Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(presetCode)) {
                Toast.makeText(this, "Preset code is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(newPass)) {
                Toast.makeText(this, "New password is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!dbHelper.isUsernameExists(username)) {
                Toast.makeText(this, "Username not found in database", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!dbHelper.verifyPresetCode(username, presetCode)) {
                Toast.makeText(this, "Invalid preset code. Cannot reset password.", Toast.LENGTH_LONG).show();
                return;
            }

            boolean updated = dbHelper.updateTeacherPassword(username, newPass);
            if (updated) {
                Toast.makeText(this, "Password reset successfully! Please log in with your new password.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to reset password. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
