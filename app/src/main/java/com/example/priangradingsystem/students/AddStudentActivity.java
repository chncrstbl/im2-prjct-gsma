package com.example.priangradingsystem.students;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

public class AddStudentActivity extends AppCompatActivity {

    private ImageView imgProfilePhoto;
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgProfilePhoto.setImageURI(uri);
                }
            });

    @Override
    @SuppressWarnings("SetTextI18n")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        ImageView btnBack = findViewById(R.id.btnBack);
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

        Button btnSave = findViewById(R.id.btnSave);

        setupSpinners(spnGradeLevel, spnSection, spnStudentGender);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (imgProfilePhoto != null) {
            imgProfilePhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        }

        if (btnDateOfBirth != null) {
            btnDateOfBirth.setOnClickListener(v -> {
                Calendar cal = Calendar.getInstance();
                int currentYear = cal.get(Calendar.YEAR);
                int currentMonth = cal.get(Calendar.MONTH);
                int currentDay = cal.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, day) -> {
                    String formattedDob = String.format(Locale.getDefault(), "%02d/%02d/%d", month + 1, day, year);
                    if (tvDateOfBirth != null) {
                        tvDateOfBirth.setText(formattedDob);
                    }

                    int calculatedAge = currentYear - year - ((currentMonth < month || (currentMonth == month && currentDay < day)) ? 1 : 0);
                    if (tvCalculatedAge != null) {
                        tvCalculatedAge.setText("" + Math.max(0, calculatedAge));
                    }
                }, currentYear - 5, currentMonth, currentDay);

                datePicker.show();
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String studentFirstName = etStudentFirstName != null ? etStudentFirstName.getText().toString().trim() : "";
                String studentMiddleName = etStudentMiddleName != null ? etStudentMiddleName.getText().toString().trim() : "";
                String studentLastName = etStudentLastName != null ? etStudentLastName.getText().toString().trim() : "";
                String studentExt = etStudentExtension != null ? etStudentExtension.getText().toString().trim() : "";
                String studentGender = spnStudentGender != null && spnStudentGender.getSelectedItem() != null ? spnStudentGender.getSelectedItem().toString() : "";

                String dob = tvDateOfBirth != null ? tvDateOfBirth.getText().toString().trim() : "";
                String age = tvCalculatedAge != null ? tvCalculatedAge.getText().toString().trim() : "";

                String gradeLevel = spnGradeLevel != null && spnGradeLevel.getSelectedItem() != null ? spnGradeLevel.getSelectedItem().toString() : "";
                String section = spnSection != null && spnSection.getSelectedItem() != null ? spnSection.getSelectedItem().toString() : "";

                String parentFirstName = etParentFirstName != null ? etParentFirstName.getText().toString().trim() : "";
                String parentMiddleName = etParentMiddleName != null ? etParentMiddleName.getText().toString().trim() : "";
                String parentLastName = etParentLastName != null ? etParentLastName.getText().toString().trim() : "";
                String parentExt = etParentExtension != null ? etParentExtension.getText().toString().trim() : "";
                String parentAddress = etParentAddress != null ? etParentAddress.getText().toString().trim() : "";
                String parentContactDigits = etParentContact != null ? etParentContact.getText().toString().trim() : "";

                if (studentFirstName.isEmpty() || studentLastName.isEmpty()) {
                    Toast.makeText(this, "Please enter the student's first and last name.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (parentFirstName.isEmpty() || parentLastName.isEmpty() || parentAddress.isEmpty() || parentContactDigits.isEmpty()) {
                    Toast.makeText(this, "Parent/Guardian name, address, and contact number are required to save.", Toast.LENGTH_LONG).show();
                    return;
                }

                String mergedFullName = studentFirstName
                        + (studentMiddleName.isEmpty() ? "" : " " + studentMiddleName)
                        + " " + studentLastName
                        + (studentExt.isEmpty() || studentExt.equalsIgnoreCase("None") ? "" : " " + studentExt);

                String mergedGradeSection = gradeLevel + (section.isEmpty() ? "" : " - " + section);

                String mergedParentName = parentFirstName
                        + (parentMiddleName.isEmpty() ? "" : " " + parentMiddleName)
                        + " " + parentLastName
                        + (parentExt.isEmpty() || parentExt.equalsIgnoreCase("None") ? "" : " " + parentExt);

                String parentContact = "🇵🇭 +63 " + parentContactDigits;

                String summary = "Saved " + mergedFullName
                        + " | Gender: " + studentGender
                        + " | Grade: " + mergedGradeSection
                        + " | Age: " + age
                        + " | Parent: " + mergedParentName
                        + " | Address: " + parentAddress
                        + " | Contact: " + parentContact;

                Toast.makeText(this, summary, Toast.LENGTH_LONG).show();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("STUDENT_NAME", mergedFullName);
                resultIntent.putExtra("STUDENT_CLASS", mergedGradeSection);
                resultIntent.putExtra("STUDENT_AGE", age);
                resultIntent.putExtra("STUDENT_DOB", dob);
                if (selectedImageUri != null) {
                    resultIntent.putExtra("STUDENT_PHOTO_URI", selectedImageUri.toString());
                }

                setResult(RESULT_OK, resultIntent);
                finish();
            });
        }
    }

    private void setupSpinners(Spinner spnGrade, Spinner spnSec, Spinner spnGender) {
        String[] grades = getResources().getStringArray(R.array.grade_level_options);
        String[] sections = getResources().getStringArray(R.array.section_options);
        String[] genders = {"Male", "Female"};

        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, grades);
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sections);
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders);

        if (spnGrade != null) spnGrade.setAdapter(gradeAdapter);
        if (spnSec != null) spnSec.setAdapter(sectionAdapter);
        if (spnGender != null) spnGender.setAdapter(genderAdapter);
    }
}
