package com.example.priangradingsystem.account;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.auth.LoginActivity;
import com.example.priangradingsystem.data.DatabaseHelper;
import com.example.priangradingsystem.model.Teacher;
import com.example.priangradingsystem.ui.ThemeManager;

public class AccountSettingsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    private TextView tvTeacherName;
    private TextView tvTeacherEmail;
    private ImageView ivTeacherAvatar;
    private SwitchCompat swNotification;
    private View btnThemeToggle;
    private View themeThumb;
    private ImageView ivThemeSun;
    private ImageView ivThemeMoon;
    private ImageView ivDecorSun;
    private ImageView ivDecorMoon;
    private View decorThemeContainer;
    private ObjectAnimator decorFloat;
    private boolean themeAnimating = false;

    private int currentTeacherId = -1;
    private String currentUsername = "";
    private Teacher currentTeacher;

    private String selectedPhotoUriString = "";
    private ImageView activeDialogAvatarView;
    private boolean isFromNewUserLogin = false;

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
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_account_settings);

        dbHelper = new DatabaseHelper(this);

        isFromNewUserLogin = getIntent().getBooleanExtra("IS_FROM_NEW_USER_LOGIN", false);
        boolean openEditProfile = getIntent().getBooleanExtra("OPEN_EDIT_PROFILE", false);

        currentTeacherId = prefs.getInt("TEACHER_ID", -1);
        currentUsername = prefs.getString("TEACHER_USERNAME", "");

        initViews();
        loadTeacherData();
        setupListeners();
        loadSettingsState();

        if (openEditProfile) {
            getWindow().getDecorView().post(this::showEditProfileDialog);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        tvTeacherName = findViewById(R.id.tvTeacherName);
        tvTeacherEmail = findViewById(R.id.tvTeacherEmail);
        ivTeacherAvatar = findViewById(R.id.ivTeacherAvatar);
        swNotification = findViewById(R.id.swNotification);
        btnThemeToggle = findViewById(R.id.btnThemeToggle);
        themeThumb = findViewById(R.id.themeThumb);
        ivThemeSun = findViewById(R.id.ivThemeSun);
        ivThemeMoon = findViewById(R.id.ivThemeMoon);
        decorThemeContainer = findViewById(R.id.decorThemeContainer);
        ivDecorSun = findViewById(R.id.ivSunAccountSetting);
        ivDecorMoon = findViewById(R.id.ivMoonAccountSetting);
    }

    private void handleBackNavigation() {
        if (isFromNewUserLogin) {
            Intent intent = new Intent(AccountSettingsActivity.this, com.example.priangradingsystem.home.MainActivity.class);
            startActivity(intent);
        }
        finish();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (isFromNewUserLogin) {
            handleBackNavigation();
        } else {
            super.onBackPressed();
        }
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
        applyThemeVisuals(ThemeManager.isDark(this), false);
        startDecorFloat();
    }

    /** Animated sun <-> moon switch. The whole app is restyled once the animation has played. */
    private void toggleAppTheme() {
        if (themeAnimating) return;
        themeAnimating = true;
        final boolean newDark = !ThemeManager.isDark(this);

        btnThemeToggle.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        applyThemeVisuals(newDark, true);

        // Let the sun/moon animation play, then switch (and cross-fade) the entire app.
        btnThemeToggle.postDelayed(() -> {
            ThemeManager.setDark(this, newDark);
            themeAnimating = false;
        }, 520);
    }

    /**
     * Puts the toggle thumb and the big sun/moon decoration into the state for {@code dark},
     * optionally animating the change.
     */
    private void applyThemeVisuals(boolean dark, boolean animate) {
        if (themeThumb == null || ivThemeSun == null || ivThemeMoon == null) return;
        final float density = getResources().getDisplayMetrics().density;
        final float thumbX = dark ? 30 * density : 0f;

        if (!animate) {
            themeThumb.setTranslationX(thumbX);
            setIconState(ivThemeSun, !dark, 90f);
            setIconState(ivThemeMoon, dark, -90f);
            if (ivDecorSun != null) setIconState(ivDecorSun, !dark, 120f);
            if (ivDecorMoon != null) setIconState(ivDecorMoon, dark, -120f);
            return;
        }

        themeThumb.animate().translationX(thumbX).setDuration(420)
                .setInterpolator(new OvershootInterpolator(1.4f)).start();
        morph(ivThemeSun, ivThemeMoon, dark, 90f, 420);
        if (ivDecorSun != null && ivDecorMoon != null) {
            morph(ivDecorSun, ivDecorMoon, dark, 120f, 520);
        }
    }

    /** Rotates/scales/fades one icon out while the other rotates/scales/fades in. */
    private void morph(ImageView sun, ImageView moon, boolean toDark, float spin, long duration) {
        ImageView outV = toDark ? sun : moon;
        ImageView inV = toDark ? moon : sun;
        float outEnd = toDark ? spin : -spin;
        float inStart = toDark ? -spin : spin;

        outV.animate().rotation(outEnd).scaleX(0.3f).scaleY(0.3f).alpha(0f)
                .setDuration(duration).start();
        inV.setRotation(inStart);
        inV.setScaleX(0.3f);
        inV.setScaleY(0.3f);
        inV.animate().rotation(0f).scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(duration).setInterpolator(new OvershootInterpolator(1.2f)).start();
    }

    private void setIconState(ImageView v, boolean visible, float hiddenRotation) {
        v.setAlpha(visible ? 1f : 0f);
        v.setScaleX(visible ? 1f : 0.3f);
        v.setScaleY(visible ? 1f : 0.3f);
        v.setRotation(visible ? 0f : hiddenRotation);
    }

    /** Gentle idle bobbing so the sun / moon feels alive. */
    private void startDecorFloat() {
        if (decorThemeContainer == null || decorFloat != null) return;
        float amp = 4 * getResources().getDisplayMetrics().density;
        decorFloat = ObjectAnimator.ofFloat(decorThemeContainer, View.TRANSLATION_Y, -amp, amp);
        decorFloat.setDuration(2200);
        decorFloat.setRepeatCount(ObjectAnimator.INFINITE);
        decorFloat.setRepeatMode(ObjectAnimator.REVERSE);
        decorFloat.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        decorFloat.start();
    }

    @Override
    protected void onDestroy() {
        if (decorFloat != null) {
            decorFloat.cancel();
            decorFloat = null;
        }
        super.onDestroy();
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
