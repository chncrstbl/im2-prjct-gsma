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
import com.example.priangradingsystem.data.DatabaseHelper;

import java.util.Calendar;
import java.util.Locale;

@SuppressLint("SimpleDateFormat")
@SuppressWarnings({"FieldCanBeLocal", "unused", "ConstantConditions", "RedundantSuppression"})
public class AddStudentActivity extends AppCompatActivity {

    private static final String TAG = "AddStudentActivity";

    private EditText etStudentFirstName, etStudentMiddleName, etStudentLastName, etStudentExtension;
    private EditText etParentFirstName, etParentMiddleName, etParentLastName, etParentExtension;
    private EditText etParentAddress, etParentContact;
    private Spinner spnStudentGender, spnGradeLevel, spnSection;
    private TextView tvDateOfBirth, tvCalculatedAge, tvHeaderTitle;
    private RelativeLayout btnDateOfBirth;
    private ImageView imgStudentPhoto;
    private Button btnSave;

    private DatabaseHelper dbHelper;
    private boolean isEditMode = false;
    private int studentId = -1;
    private String photoUriString = "";

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

                            photoUriString = selectedImageUri.toString();
                            if (imgStudentPhoto != null) {
                                imgStudentPhoto.setImageURI(selectedImageUri);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error picking image", e);
                }
            });

    @Override
    @SuppressWarnings("SetTextI18n")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        try {
            dbHelper = new DatabaseHelper(this);

            ImageView btnBack = findViewById(R.id.btnBack);
            tvHeaderTitle = findViewById(R.id.tvHeaderTitle);

            imgStudentPhoto = findViewById(R.id.studentImageView);

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
            etParentExtension = findViewById(R.id.etParentExtension);

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

            if (imgStudentPhoto != null) {
                imgStudentPhoto.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("image/*");
                        pickImageLauncher.launch(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error opening image picker", e);
                        Toast.makeText(this, "Cannot open gallery", Toast.LENGTH_SHORT).show();
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

            isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);
            studentId = getIntent().getIntExtra("STUDENT_ID", -1);

            if (isEditMode) {
                if (tvHeaderTitle != null) {
                    tvHeaderTitle.setText("Edit Student");
                }

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
                    String cleanContact = editContact.replace("+63", "").trim();
                    etParentContact.setText(cleanContact);
                }

                String editClass = getIntent().getStringExtra("STUDENT_CLASS");
                if (editClass != null && spnGradeLevel != null) {
                    if (editClass.contains("-")) {
                        editClass = editClass.split("-")[0].trim();
                    }
                    android.widget.SpinnerAdapter rawAdapter = spnGradeLevel.getAdapter();
                    if (rawAdapter instanceof ArrayAdapter) {
                        @SuppressWarnings("unchecked")
                        ArrayAdapter<String> adapter = (ArrayAdapter<String>) rawAdapter;
                        int spinnerPosition = adapter.getPosition(editClass);
                        if (spinnerPosition >= 0) {
                            spnGradeLevel.setSelection(spinnerPosition);
                        }
                    }
                }

                String editPhoto = getIntent().getStringExtra("STUDENT_PHOTO_URI");
                if (editPhoto != null && !editPhoto.isEmpty()) {
                    photoUriString = editPhoto;
                    if (imgStudentPhoto != null) {
                        try {
                            imgStudentPhoto.setImageURI(Uri.parse(editPhoto));
                        } catch (Exception ignored) {}
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
            String[] grades = {"Nursery", "Toddler"};
            String[] genders = {"Male", "Female"};

            ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, grades);
            ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders);

            if (spnGradeLevel != null) spnGradeLevel.setAdapter(gradeAdapter);
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
            String age = tvCalculatedAge != null ? tvCalculatedAge.getText().toString().trim() : "";
            if (age.isEmpty() || age.contains("Auto")) {
                age = "5";
            }

            String gradeLevel = spnGradeLevel != null && spnGradeLevel.getSelectedItem() != null ?
                    spnGradeLevel.getSelectedItem().toString() : "Nursery";

            String parentFirstName = etParentFirstName != null ? etParentFirstName.getText().toString().trim() : "";
            String parentMiddleName = etParentMiddleName != null ? etParentMiddleName.getText().toString().trim() : "";
            String parentLastName = etParentLastName != null ? etParentLastName.getText().toString().trim() : "";
            String parentExtension = etParentExtension != null ? etParentExtension.getText().toString().trim() : "";

            String parentAddress = etParentAddress != null ? etParentAddress.getText().toString().trim() : "";
            String parentContactDigits = etParentContact != null ? etParentContact.getText().toString().trim() : "";

            // --- STRICT WARNING / VALIDATION (English) ---
            if (studentFirstName.isEmpty()) {
                if (etStudentFirstName != null) etStudentFirstName.setError("Required field");
                Toast.makeText(this, "Please enter the student's first name.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (studentLastName.isEmpty()) {
                if (etStudentLastName != null) etStudentLastName.setError("Required field");
                Toast.makeText(this, "Please enter the student's last name.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dob.isEmpty() || dob.equals("Date of Birth")) {
                Toast.makeText(this, "Please select the student's date of birth.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (parentFirstName.isEmpty()) {
                if (etParentFirstName != null) etParentFirstName.setError("Required field");
                Toast.makeText(this, "Please enter the parent/guardian's first name.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (parentLastName.isEmpty()) {
                if (etParentLastName != null) etParentLastName.setError("Required field");
                Toast.makeText(this, "Please enter the parent/guardian's last name.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (parentContactDigits.isEmpty()) {
                if (etParentContact != null) etParentContact.setError("Required field");
                Toast.makeText(this, "Please enter the parent/guardian's contact number.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (parentAddress.isEmpty()) {
                if (etParentAddress != null) etParentAddress.setError("Required field");
                Toast.makeText(this, "Please enter the parent/guardian's address.", Toast.LENGTH_SHORT).show();
                return;
            }
            // -----------------------------------------------------------

            String guardianFullName = parentFirstName
                    + (parentMiddleName.isEmpty() ? "" : " " + parentMiddleName)
                    + " " + parentLastName
                    + (parentExtension.isEmpty() ? "" : " " + parentExtension);

            String parentContact = parentContactDigits.isEmpty() ? "" : "+63 " + parentContactDigits;

            boolean success;
            if (isEditMode && studentId != -1) {
                success = dbHelper.updateStudent(studentId, studentFirstName, studentMiddleName, studentLastName, dob, studentGender, age, guardianFullName, parentContact, parentAddress, photoUriString, gradeLevel);
            } else {
                success = dbHelper.insertStudent(studentFirstName, studentMiddleName, studentLastName, dob, studentGender, age, guardianFullName, parentContact, parentAddress, photoUriString, gradeLevel);
            }

            if (success) {
                Toast.makeText(this, isEditMode ? "Student updated successfully!" : "Student added successfully!", Toast.LENGTH_SHORT).show();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("STUDENT_ID", studentId);
                resultIntent.putExtra("STUDENT_FIRST_NAME", studentFirstName);
                resultIntent.putExtra("STUDENT_MIDDLE_NAME", studentMiddleName);
                resultIntent.putExtra("STUDENT_LAST_NAME", studentLastName);
                resultIntent.putExtra("STUDENT_EXTENSION", studentExtension);
                resultIntent.putExtra("STUDENT_NAME", studentFirstName + " " + studentLastName);
                resultIntent.putExtra("STUDENT_CLASS", gradeLevel);
                resultIntent.putExtra("STUDENT_GENDER", studentGender);
                resultIntent.putExtra("STUDENT_AGE", age);
                resultIntent.putExtra("STUDENT_DOB", dob);

                resultIntent.putExtra("PARENT_FIRST_NAME", parentFirstName);
                resultIntent.putExtra("PARENT_MIDDLE_NAME", parentMiddleName);
                resultIntent.putExtra("PARENT_LAST_NAME", parentLastName);
                resultIntent.putExtra("PARENT_EXTENSION", parentExtension);

                resultIntent.putExtra("STUDENT_ADDRESS", parentAddress);
                resultIntent.putExtra("STUDENT_CONTACT", parentContact);
                resultIntent.putExtra("STUDENT_PHOTO_URI", photoUriString);

                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Database operation failed. Please try again.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in save click listener", e);
            Toast.makeText(this, "Unable to save student. Please check inputs.", Toast.LENGTH_SHORT).show();
        }
    }
}