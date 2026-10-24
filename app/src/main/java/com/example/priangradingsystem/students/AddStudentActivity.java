package com.example.priangradingsystem.students;

import android.annotation.SuppressLint;
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

import com.example.priangradingsystem.R;

import java.util.Calendar;
import java.util.Locale;

@SuppressLint("SimpleDateFormat")
@SuppressWarnings({"FieldCanBeLocal", "unused", "ConstantConditions", "RedundantSuppression"})
public class AddStudentActivity extends AppCompatActivity {

    private static final String TAG = "AddStudentActivity";

    private ImageView imgProfilePhoto;
    private Uri selectedImageUri = null;

    private EditText etStudentFirstName, etStudentMiddleName, etStudentLastName, etStudentExtension;
    private EditText etParentFirstName, etParentMiddleName, etParentLastName, etParentExtension;
    private EditText etParentAddress, etParentContact;
    private Spinner spnStudentGender, spnGradeLevel, spnSection;
    private TextView tvDateOfBirth, tvCalculatedAge, tvHeaderTitle;
    private RelativeLayout btnDateOfBirth;
    private Button btnSave;

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        selectedImageUri = uri;
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    } catch (Exception e) {
                        Log.w(TAG, "Could not take persistable URI permission", e);
                    }
                    if (imgProfilePhoto != null) {
                        imgProfilePhoto.setImageURI(uri);
                    }
                }
            });

    @Override
    @SuppressWarnings("SetTextI18n")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        try {
            ImageView btnBack = findViewById(R.id.btnBack);
            tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
            imgProfilePhoto = findViewById(R.id.imgProfilePhoto);

            etStudentFirstName = findViewById(R.id.etStudentFirstName);
            etStudentMiddleName = findViewById(R.id.etStudentMiddleName);
            etStudentLastName = findViewById(R.id.etStudentLastName);
            etStudentExtension = findViewById(R.id.etStudentExtension);
            spnStudentGender = findViewById(R.id.spnStudentGender);

            btnDateOfBirth = findViewById(R.id.btnDateOfBirth);
            tvDateOfBirth = findViewById(R.id.tvDateOfBirth);
            tvCalculatedAge = findViewById(R.id.tvCalculatedAge);

            spnGradeLevel = findViewById(R.id.spnGradeLevel);

            etParentFirstName = findViewById(R.id.etParentFirstName);
            etParentMiddleName = findViewById(R.id.etParentMiddleName);
            etParentLastName = findViewById(R.id.etParentLastName);
            etParentExtension = findViewById(R.id.etParentExtension); // Kung meron sa XML

            etParentAddress = findViewById(R.id.etParentAddress);
            etParentContact = findViewById(R.id.etParentContact);
            btnSave = findViewById(R.id.btnSave);

            if (etParentContact != null) {
                etParentContact.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11)});
            }

            setupSpinners();

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

            boolean isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);
            if (isEditMode) {
                if (tvHeaderTitle != null) {
                    tvHeaderTitle.setText("Edit Student");
                }

                // Kunin ang mga hiwalay na extras kung ipinasa
                String fName = getIntent().getStringExtra("STUDENT_FIRST_NAME");
                String mName = getIntent().getStringExtra("STUDENT_MIDDLE_NAME");
                String lName = getIntent().getStringExtra("STUDENT_LAST_NAME");
                String ext = getIntent().getStringExtra("STUDENT_EXTENSION");

                if (fName != null && etStudentFirstName != null) etStudentFirstName.setText(fName);
                if (mName != null && etStudentMiddleName != null) etStudentMiddleName.setText(mName);
                if (lName != null && etStudentLastName != null) etStudentLastName.setText(lName);
                if (ext != null && etStudentExtension != null) etStudentExtension.setText(ext);

                String pfName = getIntent().getStringExtra("PARENT_FIRST_NAME");
                String pmName = getIntent().getStringExtra("PARENT_MIDDLE_NAME");
                String plName = getIntent().getStringExtra("PARENT_LAST_NAME");
                String pExt = getIntent().getStringExtra("PARENT_EXTENSION");

                if (pfName != null && etParentFirstName != null) etParentFirstName.setText(pfName);
                if (pmName != null && etParentMiddleName != null) etParentMiddleName.setText(pmName);
                if (plName != null && etParentLastName != null) etParentLastName.setText(plName);
                if (pExt != null && etParentExtension != null) etParentExtension.setText(pExt);

                String editAge = getIntent().getStringExtra("STUDENT_AGE");
                if (editAge != null && tvCalculatedAge != null) tvCalculatedAge.setText(editAge);

                String editDob = getIntent().getStringExtra("STUDENT_DOB");
                if (editDob != null && tvDateOfBirth != null) tvDateOfBirth.setText(editDob);

                String editAddress = getIntent().getStringExtra("STUDENT_ADDRESS");
                if (editAddress != null && etParentAddress != null) etParentAddress.setText(editAddress);

                String editContact = getIntent().getStringExtra("STUDENT_CONTACT");
                if (editContact != null && etParentContact != null) {
                    String cleanContact = editContact.replace("+63", "").replace("🇵🇭", "").trim();
                    etParentContact.setText(cleanContact);
                }

                String editPhotoUri = getIntent().getStringExtra("STUDENT_PHOTO_URI");
                if (editPhotoUri != null && !editPhotoUri.trim().isEmpty()) {
                    selectedImageUri = Uri.parse(editPhotoUri);
                    if (imgProfilePhoto != null) {
                        imgProfilePhoto.setImageURI(selectedImageUri);
                    }
                }
            }

            if (btnSave != null) {
                btnSave.setOnClickListener(v -> saveStudentData());
            }

        } catch (Exception e) {
            Log.e(TAG, "Critical error in onCreate", e);
        }
    }

    private void setupSpinners() {
        try {
            String[] grades = {"Nursery", "Pre-K", "Kindergarten"};
            try {
                String[] resGrades = getResources().getStringArray(R.array.grade_level_options);
                if (resGrades.length > 0) grades = resGrades;
            } catch (Exception ignored) {}

            String[] sections = {"Section A", "Section B", "Section C"};
            try {
                String[] resSections = getResources().getStringArray(R.array.section_options);
                if (resSections.length > 0) sections = resSections;
            } catch (Exception ignored) {}

            String[] genders = {"Male", "Female", "Other"};

            ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, grades);
            ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sections);
            ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders);

            if (spnGradeLevel != null) spnGradeLevel.setAdapter(gradeAdapter);
            if (spnSection != null) spnSection.setAdapter(sectionAdapter);
            if (spnStudentGender != null) spnStudentGender.setAdapter(genderAdapter);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up spinners", e);
        }
    }

    private void saveStudentData() {
        try {
            String studentFirstName = etStudentFirstName != null ? etStudentFirstName.getText().toString().trim() : "";
            String studentMiddleName = etStudentMiddleName != null ? etStudentMiddleName.getText().toString().trim() : "";
            String studentLastName = etStudentLastName != null ? etStudentLastName.getText().toString().trim() : "";
            String studentExtension = etStudentExtension != null ? etStudentExtension.getText().toString().trim() : "";

            String studentGender = spnStudentGender != null && spnStudentGender.getSelectedItem() != null ?
                    spnStudentGender.getSelectedItem().toString() : "Male";

            String dob = tvDateOfBirth != null ? tvDateOfBirth.getText().toString().trim() : "";
            String age = tvCalculatedAge != null ? tvCalculatedAge.getText().toString().trim() : "5";
            if (age.isEmpty() || age.contains("Auto")) age = "5";

            String gradeLevel = spnGradeLevel != null && spnGradeLevel.getSelectedItem() != null ? spnGradeLevel.getSelectedItem().toString() : "";
            String section = spnSection != null && spnSection.getSelectedItem() != null ? spnSection.getSelectedItem().toString() : "";

            String parentFirstName = etParentFirstName != null ? etParentFirstName.getText().toString().trim() : "";
            String parentMiddleName = etParentMiddleName != null ? etParentMiddleName.getText().toString().trim() : "";
            String parentLastName = etParentLastName != null ? etParentLastName.getText().toString().trim() : "";
            String parentExtension = etParentExtension != null ? etParentExtension.getText().toString().trim() : "";

            String parentAddress = etParentAddress != null ? etParentAddress.getText().toString().trim() : "";
            String parentContactDigits = etParentContact != null ? etParentContact.getText().toString().trim() : "";

            if (studentFirstName.isEmpty() || studentLastName.isEmpty()) {
                Toast.makeText(this, "Please enter the student's first and last name.", Toast.LENGTH_SHORT).show();
                return;
            }

            String mergedFullName = studentFirstName
                    + (studentMiddleName.isEmpty() ? "" : " " + studentMiddleName)
                    + " " + studentLastName
                    + (studentExtension.isEmpty() ? "" : " " + studentExtension);

            String mergedGradeSection = gradeLevel + (section.isEmpty() ? "" : " - " + section);

            String mergedParentName = parentFirstName
                    + (parentMiddleName.isEmpty() ? "" : " " + parentMiddleName)
                    + " " + parentLastName
                    + (parentExtension.isEmpty() ? "" : " " + parentExtension);

            String parentContact = parentContactDigits.isEmpty() ? "" : "+63 " + parentContactDigits;

            Intent resultIntent = new Intent();
            resultIntent.putExtra("STUDENT_NAME", mergedFullName);
            resultIntent.putExtra("STUDENT_CLASS", mergedGradeSection);
            resultIntent.putExtra("STUDENT_AGE", age);
            resultIntent.putExtra("STUDENT_GENDER", studentGender);
            resultIntent.putExtra("STUDENT_DOB", dob);
            resultIntent.putExtra("STUDENT_PARENT", mergedParentName);
            resultIntent.putExtra("STUDENT_ADDRESS", parentAddress);
            resultIntent.putExtra("STUDENT_CONTACT", parentContact);
            resultIntent.putExtra("STUDENT_PHOTO_URI", selectedImageUri != null ? selectedImageUri.toString() : "");

            resultIntent.putExtra("STUDENT_FIRST_NAME", studentFirstName);
            resultIntent.putExtra("STUDENT_MIDDLE_NAME", studentMiddleName);
            resultIntent.putExtra("STUDENT_LAST_NAME", studentLastName);
            resultIntent.putExtra("STUDENT_EXTENSION", studentExtension);

            resultIntent.putExtra("PARENT_FIRST_NAME", parentFirstName);
            resultIntent.putExtra("PARENT_MIDDLE_NAME", parentMiddleName);
            resultIntent.putExtra("PARENT_LAST_NAME", parentLastName);
            resultIntent.putExtra("PARENT_EXTENSION", parentExtension);

            setResult(RESULT_OK, resultIntent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error in save click listener", e);
            Toast.makeText(this, "Unable to save student. Please check inputs.", Toast.LENGTH_SHORT).show();
        }
    }
}