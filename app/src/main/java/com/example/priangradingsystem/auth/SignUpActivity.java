package com.example.priangradingsystem.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.priangradingsystem.R;
import com.example.priangradingsystem.data.DatabaseHelper;
import com.google.android.material.button.MaterialButton;

public class SignUpActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private CheckBox cbPrivacy;
    private DatabaseHelper dbHelper;
    private String currentPresetCode;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        dbHelper = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbPrivacy = findViewById(R.id.cbPrivacy);
        TextView tvPresetCode = findViewById(R.id.tvPresetCode);
        ImageView ivEyeToggle = findViewById(R.id.ivEyeToggle);
        ImageView ivEyeToggleConfirm = findViewById(R.id.ivEyeToggleConfirm);
        MaterialButton btnCreateAccount = findViewById(R.id.btnCreateAccount);
        TextView tvLoginPrompt = findViewById(R.id.tvLoginPrompt);

        // Generate preset code
        currentPresetCode = "LL-" + (1000 + new java.util.Random().nextInt(9000));
        if (tvPresetCode != null) {
            tvPresetCode.setText(currentPresetCode);
        }

        if (ivEyeToggle != null && etPassword != null) {
            ivEyeToggle.setOnClickListener(v -> togglePasswordVisibility());
        }

        if (ivEyeToggleConfirm != null && etConfirmPassword != null) {
            ivEyeToggleConfirm.setOnClickListener(v -> toggleConfirmPasswordVisibility());
        }

        if (btnCreateAccount != null) {
            btnCreateAccount.setOnClickListener(v -> handleSignUp());
        }

        if (tvLoginPrompt != null) {
            tvLoginPrompt.setOnClickListener(v -> navigateToLogin());
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

    private void toggleConfirmPasswordVisibility() {
        if (etConfirmPassword == null) return;
        if (isConfirmPasswordVisible) {
            etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            isConfirmPasswordVisible = false;
        } else {
            etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            isConfirmPasswordVisible = true;
        }
        etConfirmPassword.setSelection(etConfirmPassword.getText().length());
    }

    private void handleSignUp() {
        String username = etUsername != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword != null ? etConfirmPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Please confirm your password");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        if (cbPrivacy != null && !cbPrivacy.isChecked()) {
            Toast.makeText(this, "Please agree with privacy policy", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbHelper.isUsernameExists(username)) {
            etUsername.setError("Username already exists");
            etUsername.requestFocus();
            return;
        }

        boolean success = dbHelper.insertTeacher(username, username, password, null, currentPresetCode);
        if (success) {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("HAS_ACCOUNT", true).apply();

            Toast.makeText(this, "Account created successfully! Please log in.", Toast.LENGTH_LONG).show();
            navigateToLogin();
        } else {
            Toast.makeText(this, "Username already exists or error creating account.", Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToLogin() {
        if (!isFinishing()) {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
