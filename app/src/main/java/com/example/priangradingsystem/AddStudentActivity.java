package com.example.priangradingsystem;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.util.Locale;

public class AddStudentActivity extends AppCompatActivity {

    private static final String TAG = "AddStudentActivity";
    private ImageView imgProfilePhoto;
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                try {
                    if (uri != null) {
                        selectedImageUri = uri;
                        try {
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        } catch (Exception e) {
                            Log.w(TAG, "Could not take persistable URI permission", e);
                        }

                        if (imgProfilePhoto != null) {
                            imgProfilePhoto.setImageURI(uri);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error picking photo", e);
                }
            });

    @Override
    @SuppressWarnings("SetTextI18n")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_add_student);

            ImageView btnBack = findViewById(R.id.btnBack);
            TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
            imgProfilePhoto = findViewById(R.id.imgProfilePhoto);

            EditText etStudentFirstName = findViewById(R.id.etStudentFirstName);
            EditText etStudentMiddleName = findViewById(R.id.etStudentMiddleName);
            EditText etStudentLastName = findViewById(R.id.etStudentLastName);
            EditText etStudentExtension = findViewById(R.id.etStudentExtension);
            Spinner spnStudentGender = findViewById(R.id.spnStudentGender);

            RelativeLayout btnDateOfBirth = findViewById(R.id.btnDateOfBirth);
            TextView tvDateOfBirth = findViewById(R.id.tvDateOfBirth);
            TextView tvCalculatedAge = findViewById(R.id.tvCalculatedAge);

            Spinner spnGradeLevel = findViewById(R.id.spnGradeLevel);
            Spinner spnSection = findViewById(R.id.spnSection);

            EditText etParentFirstName = findViewById(R.id.etParentFirstName);
            EditText etParentMiddleName = findViewById(R.id.etParentMiddleName);
            EditText etParentLastName = findViewById(R.id.etParentLastName);
            EditText etParentExtension = findViewById(R.id.etParentExtension);
            EditText etParentAddress = findViewById(R.id.etParentAddress);
            EditText etParentContact = findViewById(R.id.etParentContact);

            if (etParentContact != null) {
                etParentContact.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11)});
            }

            Button btnSave = findViewById(R.id.btnSave);

            setupSpinners(spnGradeLevel, spnSection, spnStudentGender);

            boolean isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);
            if (isEditMode) {
                if (tvHeaderTitle != null) {
                    tvHeaderTitle.setText("Edit Student");
                }

                String fullEditName = getIntent().getStringExtra("STUDENT_NAME");
                if (fullEditName != null && !fullEditName.trim().isEmpty()) {
                    String[] nameParts = fullEditName.split(" ");
                    if (nameParts.length > 0 && etStudentFirstName != null) etStudentFirstName.setText(nameParts[0]);
                    if (nameParts.length > 1 && etStudentLastName != null) etStudentLastName.setText(nameParts[nameParts.length - 1]);
                }

                String editAge = getIntent().getStringExtra("STUDENT_AGE");
                if (editAge != null && tvCalculatedAge != null) tvCalculatedAge.setText(editAge);

                String editDob = getIntent().getStringExtra("STUDENT_DOB");
                if (editDob != null && tvDateOfBirth != null) tvDateOfBirth.setText(editDob);

                String editParent = getIntent().getStringExtra("STUDENT_PARENT");
                if (editParent != null && etParentFirstName != null) etParentFirstName.setText(editParent);

                String editAddress = getIntent().getStringExtra("STUDENT_ADDRESS");
                if (editAddress != null && etParentAddress != null) etParentAddress.setText(editAddress);

                String editContact = getIntent().getStringExtra("STUDENT_CONTACT");
                if (editContact != null && etParentContact != null) {
                    String cleanContact = editContact.replace("+63", "").trim();
                    etParentContact.setText(cleanContact);
                }

                String editPhotoUri = getIntent().getStringExtra("STUDENT_PHOTO_URI");
                if (editPhotoUri != null && !editPhotoUri.trim().isEmpty()) {
                    selectedImageUri = Uri.parse(editPhotoUri);
                    if (imgProfilePhoto != null) {
                        imgProfilePhoto.setImageURI(selectedImageUri);
                    }
                }
            } else {
                if (tvHeaderTitle != null) {
                    tvHeaderTitle.setText("Add Student");
                }
            }

            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }

            if (imgProfilePhoto != null) {
                imgProfilePhoto.setOnClickListener(v -> {
                    try {
                        photoPickerLauncher.launch("image/*");
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching photo picker", e);
                    }
                });
            }

            if (btnDateOfBirth != null) {
                btnDateOfBirth.setOnClickListener(v -> {
                    try {
                        Calendar cal = Calendar.getInstance();
                        int currentYear = cal.get(Calendar.YEAR);
                        int currentMonth = cal.get(Calendar.MONTH);
                        int currentDay = cal.get(Calendar.DAY_OF_MONTH);

                        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, day) -> {
                            try {
                                String formattedDob = String.format(Locale.getDefault(), "%02d/%02d/%d", month + 1, day, year);
                                if (tvDateOfBirth != null) {
                                    tvDateOfBirth.setText(formattedDob);
                                }

                                int age = currentYear - year;
                                if (currentMonth < month || (currentMonth == month && currentDay < day)) {
                                    age--;
                                }
                                if (tvCalculatedAge != null) {
                                    tvCalculatedAge.setText(String.valueOf(Math.max(0, age)));
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error setting date of birth", e);
                            }
                        }, currentYear - 5, currentMonth, currentDay);

                        datePicker.show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing date picker", e);
                    }
                });
            }

            if (btnSave != null) {
                btnSave.setOnClickListener(v -> {
                    try {
                        String firstName = "";
                        try { if (etStudentFirstName != null && etStudentFirstName.getText() != null) firstName = etStudentFirstName.getText().toString().trim(); } catch (Exception ignored) {}

                        String middleName = "";
                        try { if (etStudentMiddleName != null && etStudentMiddleName.getText() != null) middleName = etStudentMiddleName.getText().toString().trim(); } catch (Exception ignored) {}

                        String lastName = "";
                        try { if (etStudentLastName != null && etStudentLastName.getText() != null) lastName = etStudentLastName.getText().toString().trim(); } catch (Exception ignored) {}

                        String ext = "";
                        try { if (etStudentExtension != null && etStudentExtension.getText() != null) ext = etStudentExtension.getText().toString().trim(); } catch (Exception ignored) {}

                        String gender = "Male";
                        try {
                            if (spnStudentGender != null && spnStudentGender.getSelectedItem() != null) {
                                gender = spnStudentGender.getSelectedItem().toString();
                            }
                        } catch (Exception ignored) {}

                        String dob = "";
                        try {
                            if (tvDateOfBirth != null && tvDateOfBirth.getText() != null) {
                                dob = tvDateOfBirth.getText().toString().trim();
                            }
                        } catch (Exception ignored) {}

                        String age = "5";
                        try {
                            if (tvCalculatedAge != null && tvCalculatedAge.getText() != null) {
                                String ageText = tvCalculatedAge.getText().toString().trim();
                                if (!ageText.isEmpty() && !ageText.contains("Auto")) {
                                    age = ageText;
                                }
                            }
                        } catch (Exception ignored) {}

                        String grade = "Nursery";
                        try {
                            if (spnGradeLevel != null && spnGradeLevel.getSelectedItem() != null) {
                                grade = spnGradeLevel.getSelectedItem().toString();
                            }
                        } catch (Exception ignored) {}

                        String section = "Section A";
                        try {
                            if (spnSection != null && spnSection.getSelectedItem() != null) {
                                section = spnSection.getSelectedItem().toString();
                            }
                        } catch (Exception ignored) {}

                        String parentFirst = "";
                        try { if (etParentFirstName != null && etParentFirstName.getText() != null) parentFirst = etParentFirstName.getText().toString().trim(); } catch (Exception ignored) {}

                        String parentMiddle = "";
                        try { if (etParentMiddleName != null && etParentMiddleName.getText() != null) parentMiddle = etParentMiddleName.getText().toString().trim(); } catch (Exception ignored) {}

                        String parentLast = "";
                        try { if (etParentLastName != null && etParentLastName.getText() != null) parentLast = etParentLastName.getText().toString().trim(); } catch (Exception ignored) {}

                        String parentExt = "";
                        try { if (etParentExtension != null && etParentExtension.getText() != null) parentExt = etParentExtension.getText().toString().trim(); } catch (Exception ignored) {}

                        String address = "";
                        try { if (etParentAddress != null && etParentAddress.getText() != null) address = etParentAddress.getText().toString().trim(); } catch (Exception ignored) {}

                        String contactDigits = "";
                        try { if (etParentContact != null && etParentContact.getText() != null) contactDigits = etParentContact.getText().toString().trim(); } catch (Exception ignored) {}

                        if (firstName.isEmpty() || lastName.isEmpty()) {
                            Toast.makeText(this, "Please enter the student's first and last name.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String fullName = firstName + " " + lastName;
                        if (!middleName.isEmpty()) fullName += " " + middleName;
                        if (!ext.isEmpty()) fullName += " " + ext;

                        String gradeSection = grade + " - " + section;

                        String parentName = parentFirst;
                        if (!parentMiddle.isEmpty()) parentName += " " + parentMiddle;
                        if (!parentLast.isEmpty()) parentName += " " + parentLast;
                        if (!parentExt.isEmpty()) parentName += " " + parentExt;

                        String parentContact = contactDigits.isEmpty() ? "" : "+63 " + contactDigits;

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("STUDENT_NAME", fullName);
                        resultIntent.putExtra("STUDENT_CLASS", gradeSection);
                        resultIntent.putExtra("STUDENT_AGE", age);
                        resultIntent.putExtra("STUDENT_GENDER", gender);
                        resultIntent.putExtra("STUDENT_DOB", dob);
                        resultIntent.putExtra("STUDENT_PARENT", parentName);
                        resultIntent.putExtra("STUDENT_ADDRESS", address);
                        resultIntent.putExtra("STUDENT_CONTACT", parentContact);
                        resultIntent.putExtra("STUDENT_PHOTO_URI", selectedImageUri != null ? selectedImageUri.toString() : "");

                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, "Error in save click listener", e);
                        Toast.makeText(this, "Unable to save student. Please check inputs.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Critical error in onCreate", e);
        }
    }

    private void setupSpinners(Spinner spnGrade, Spinner spnSec, Spinner spnGender) {
        try {
            String[] grades = {"Nursery", "Pre-K", "Kindergarten"};
            try {
                String[] resGrades = getResources().getStringArray(R.array.grade_level_options);
                if (resGrades.length > 0) {
                    grades = resGrades;
                }
            } catch (Exception ignored) {}

            String[] sections = {"Section A", "Section B", "Section C"};
            try {
                String[] resSections = getResources().getStringArray(R.array.section_options);
                if (resSections.length > 0) {
                    sections = resSections;
                }
            } catch (Exception ignored) {}

            String[] genders = {"Male", "Female", "Other"};

            ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, grades);
            ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sections);
            ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders);

            if (spnGrade != null) spnGrade.setAdapter(gradeAdapter);
            if (spnSec != null) spnSec.setAdapter(sectionAdapter);
            if (spnGender != null) spnGender.setAdapter(genderAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up spinners", e);
        }
    }
}