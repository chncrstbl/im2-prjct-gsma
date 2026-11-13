package com.example.priangradingsystem.account;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.auth.LoginActivity;
import com.example.priangradingsystem.data.DatabaseHelper;
import com.example.priangradingsystem.model.Teacher;

public class AccountSettingsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    private TextView tvTeacherName;
    private TextView tvTeacherEmail;
    private ImageView ivTeacherAvatar;
    private SwitchCompat swNotification;
    private LinearLayout btnThemeToggle;
    private ImageView ivThemeSun;
    private ImageView ivThemeMoon;

    private int currentTeacherId = -1;
    private String currentUsername = "";
    private Teacher currentTeacher;

    private String selectedPhotoUriString = "";
    private ImageView activeDialogAvatarView;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                try {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            try {
                                getContentResolver().takePersistableUriPermission(
                                        selectedImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                                );
                            } catch (Exception ignored) {}

                            selectedPhotoUriString = selectedImageUri.toString();
                            if (activeDialogAvatarView != null) {
                                activeDialogAvatarView.setImageURI(selectedImageUri);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("AccountSettings", "Error picking profile photo", e);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("DARK_MODE_ENABLED", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_account_settings);

        dbHelper = new DatabaseHelper(this);

        currentTeacherId = prefs.getInt("TEACHER_ID", -1);
        currentUsername = prefs.getString("TEACHER_USERNAME", "");

        initViews();
        loadTeacherData();
        setupListeners();
        loadSettingsState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvTeacherName = findViewById(R.id.tvTeacherName);
        tvTeacherEmail = findViewById(R.id.tvTeacherEmail);
        ivTeacherAvatar = findViewById(R.id.ivTeacherAvatar);
        swNotification = findViewById(R.id.swNotification);
        btnThemeToggle = findViewById(R.id.btnThemeToggle);
        ivThemeSun = findViewById(R.id.ivThemeSun);
        ivThemeMoon = findViewById(R.id.ivThemeMoon);
    }

    private void loadTeacherData() {
        if (currentTeacherId != -1) {
            currentTeacher = dbHelper.getTeacherById(currentTeacherId);
        }
        if (currentTeacher == null && !TextUtils.isEmpty(currentUsername)) {
            currentTeacher = dbHelper.getTeacherByUsername(currentUsername);
        }

        String savedPhotoUri = prefs.getString("TEACHER_PROFILE_URI", "");
        if (ivTeacherAvatar != null && !TextUtils.isEmpty(savedPhotoUri)) {
            try {
                ivTeacherAvatar.setImageURI(Uri.parse(savedPhotoUri));
            } catch (Exception ignored) {}
        }

        if (currentTeacher != null) {
            currentTeacherId = currentTeacher.getTeacherId();
            currentUsername = currentTeacher.getUsername();

            if (tvTeacherName != null) {
                tvTeacherName.setText(currentTeacher.getFullName());
            }
            if (tvTeacherEmail != null) {
                String email = currentTeacher.getEmail();
                if (TextUtils.isEmpty(email)) {
                    email = currentTeacher.getUsername() + "@gmail.com";
                }
                tvTeacherEmail.setText(email);
            }
        } else {
            String savedName = prefs.getString("TEACHER_NAME", "Teacher");
            if (tvTeacherName != null) tvTeacherName.setText(savedName);
            if (tvTeacherEmail != null) tvTeacherEmail.setText(currentUsername + "@gmail.com");
        }
    }

    private void setupListeners() {
        View btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        }

        View btnPersonalInfo = findViewById(R.id.btnPersonalInfo);
        if (btnPersonalInfo != null) {
            btnPersonalInfo.setOnClickListener(v -> showPersonalInfoDialog());
        }

        View btnChangePassword = findViewById(R.id.btnChangePassword);
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        if (swNotification != null) {
            swNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("NOTIFICATIONS_ENABLED", isChecked).apply();
                Toast.makeText(this, isChecked ? "Notifications enabled" : "Notifications disabled", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnThemeToggle != null) {
            btnThemeToggle.setOnClickListener(v -> toggleAppTheme());
        }

        View btnLogOut = findViewById(R.id.btnLogOut);
        if (btnLogOut != null) {
            btnLogOut.setOnClickListener(v -> showLogoutConfirmationDialog());
        }
    }

    private void loadSettingsState() {
        boolean notifEnabled = prefs.getBoolean("NOTIFICATIONS_ENABLED", true);
        if (swNotification != null) {
            swNotification.setChecked(notifEnabled);
        }

        boolean isDarkMode = prefs.getBoolean("DARK_MODE_ENABLED", false);
        updateThemeUI(isDarkMode);
    }

    private void toggleAppTheme() {
        boolean isDarkMode = prefs.getBoolean("DARK_MODE_ENABLED", false);
        boolean newDarkModeState = !isDarkMode;

        prefs.edit().putBoolean("DARK_MODE_ENABLED", newDarkModeState).apply();

        if (newDarkModeState) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Toast.makeText(this, "Dark Mode enabled", Toast.LENGTH_SHORT).show();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(this, "Light Mode enabled", Toast.LENGTH_SHORT).show();
        }
        recreate();
    }

    private void updateThemeUI(boolean isDarkMode) {
        if (btnThemeToggle != null) {
            btnThemeToggle.setBackgroundResource(isDarkMode ? R.drawable.dark_mode_container : R.drawable.light_mode_container);
        }
        if (ivThemeSun != null && ivThemeMoon != null) {
            if (isDarkMode) {
                ivThemeSun.setAlpha(0.4f);
                ivThemeMoon.setAlpha(1.0f);
            } else {
                ivThemeSun.setAlpha(1.0f);
                ivThemeMoon.setAlpha(0.4f);
            }
        }
    }

    private void showEditProfileDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView ivDialogProfileAvatar = dialogView.findViewById(R.id.ivDialogProfileAvatar);
        View btnSelectPhoto = dialogView.findViewById(R.id.btnSelectPhoto);
        EditText etFullName = dialogView.findViewById(R.id.etDialogFullName);
        EditText etEmail = dialogView.findViewById(R.id.etDialogEmail);
        View btnCancel = dialogView.findViewById(R.id.btnCancelEditProfile);
        View btnSave = dialogView.findViewById(R.id.btnSaveEditProfile);

        selectedPhotoUriString = prefs.getString("TEACHER_PROFILE_URI", "");
        if (ivDialogProfileAvatar != null && !TextUtils.isEmpty(selectedPhotoUriString)) {
            try {
                ivDialogProfileAvatar.setImageURI(Uri.parse(selectedPhotoUriString));
            } catch (Exception ignored) {}
        }

        View.OnClickListener openPicker = v -> {
            activeDialogAvatarView = ivDialogProfileAvatar;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            pickImageLauncher.launch(intent);
        };

        if (btnSelectPhoto != null) btnSelectPhoto.setOnClickListener(openPicker);
        if (ivDialogProfileAvatar != null) ivDialogProfileAvatar.setOnClickListener(openPicker);

        if (etFullName != null) {
            etFullName.setText(currentTeacher != null ? currentTeacher.getFullName() : tvTeacherName.getText().toString());
        }
        if (etEmail != null) {
            etEmail.setText(currentTeacher != null && !TextUtils.isEmpty(currentTeacher.getEmail()) ? currentTeacher.getEmail() : tvTeacherEmail.getText().toString());
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String newName = etFullName != null ? etFullName.getText().toString().trim() : "";
                String newEmail = etEmail != null ? etEmail.getText().toString().trim() : "";

                if (TextUtils.isEmpty(newName)) {
                    Toast.makeText(this, "Full name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                prefs.edit().putString("TEACHER_PROFILE_URI", selectedPhotoUriString).apply();
                if (ivTeacherAvatar != null && !TextUtils.isEmpty(selectedPhotoUriString)) {
                    try {
                        ivTeacherAvatar.setImageURI(Uri.parse(selectedPhotoUriString));
                    } catch (Exception ignored) {}
                }

                if (currentTeacherId != -1) {
                    boolean success = dbHelper.updateTeacherProfile(currentTeacherId, newName, newEmail);
                    if (success) {
                        prefs.edit().putString("TEACHER_NAME", newName).apply();
                        if (tvTeacherName != null) tvTeacherName.setText(newName);
                        if (tvTeacherEmail != null) tvTeacherEmail.setText(newEmail);
                        loadTeacherData();
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    prefs.edit().putString("TEACHER_NAME", newName).apply();
                    if (tvTeacherName != null) tvTeacherName.setText(newName);
                    if (tvTeacherEmail != null) tvTeacherEmail.setText(newEmail);
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        }

        dialog.show();
    }

    private void showPersonalInfoDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_personal_information, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvName = dialogView.findViewById(R.id.tvInfoFullName);
        TextView tvUser = dialogView.findViewById(R.id.tvInfoUsername);
        TextView tvMail = dialogView.findViewById(R.id.tvInfoEmail);
        TextView tvCode = dialogView.findViewById(R.id.tvInfoPresetCode);
        View btnClose = dialogView.findViewById(R.id.btnCloseInfo);

        String name = currentTeacher != null ? currentTeacher.getFullName() : tvTeacherName.getText().toString();
        String username = currentTeacher != null ? currentTeacher.getUsername() : currentUsername;
        String email = currentTeacher != null && !TextUtils.isEmpty(currentTeacher.getEmail()) ? currentTeacher.getEmail() : tvTeacherEmail.getText().toString();
        String presetCode = currentTeacher != null && !TextUtils.isEmpty(currentTeacher.getPresetCode()) ? currentTeacher.getPresetCode() : "N/A";

        if (tvName != null) tvName.setText(name);
        if (tvUser != null) tvUser.setText(username);
        if (tvMail != null) tvMail.setText(email);
        if (tvCode != null) tvCode.setText(presetCode);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etPresetCode = dialogView.findViewById(R.id.etDialogPresetCode);
        EditText etNewPassword = dialogView.findViewById(R.id.etDialogNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etDialogConfirmPassword);
        ImageView ivEyeToggleNewPassword = dialogView.findViewById(R.id.ivEyeToggleNewPassword);
        ImageView ivEyeToggleConfirmPassword = dialogView.findViewById(R.id.ivEyeToggleConfirmPassword);
        View btnCancel = dialogView.findViewById(R.id.btnCancelChangePassword);
        View btnUpdate = dialogView.findViewById(R.id.btnUpdatePassword);

        final boolean[] isNewPassVisible = {false};
        final boolean[] isConfirmPassVisible = {false};

        if (ivEyeToggleNewPassword != null && etNewPassword != null) {
            ivEyeToggleNewPassword.setOnClickListener(v -> {
                if (isNewPassVisible[0]) {
                    etNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    isNewPassVisible[0] = false;
                } else {
                    etNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    isNewPassVisible[0] = true;
                }
                etNewPassword.setSelection(etNewPassword.getText().length());
            });
        }

        if (ivEyeToggleConfirmPassword != null && etConfirmPassword != null) {
            ivEyeToggleConfirmPassword.setOnClickListener(v -> {
                if (isConfirmPassVisible[0]) {
                    etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    isConfirmPassVisible[0] = false;
                } else {
                    etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    isConfirmPassVisible[0] = true;
                }
                etConfirmPassword.setSelection(etConfirmPassword.getText().length());
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> {
                String presetCode = etPresetCode != null ? etPresetCode.getText().toString().trim() : "";
                String newPassword = etNewPassword != null ? etNewPassword.getText().toString().trim() : "";
                String confirmPassword = etConfirmPassword != null ? etConfirmPassword.getText().toString().trim() : "";

                if (TextUtils.isEmpty(presetCode)) {
                    Toast.makeText(this, "Preset code is required to change password", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(newPassword)) {
                    Toast.makeText(this, "New password cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!dbHelper.verifyPresetCode(currentUsername, presetCode)) {
                    Toast.makeText(this, "Invalid preset code. Cannot reset password.", Toast.LENGTH_LONG).show();
                    return;
                }

                boolean updated = dbHelper.updateTeacherPassword(currentUsername, newPassword);
                if (updated) {
                    Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Failed to update password.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        dialog.show();
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out from your account?")
                .setPositiveButton("Log Out", (dialog, which) -> handleLogout())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void handleLogout() {
        prefs.edit().putBoolean("IS_LOGGED_IN", false).apply();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(AccountSettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
