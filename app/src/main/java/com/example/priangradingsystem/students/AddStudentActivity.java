package com.example.priangradingsystem.students;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.priangradingsystem.R;
import com.example.priangradingsystem.data.AgeCalculator;
import com.example.priangradingsystem.data.DatabaseHelper;
import com.example.priangradingsystem.ui.SchoolYearBar;

import java.util.Calendar;
import java.util.Locale;

@SuppressLint("SimpleDateFormat")
@SuppressWarnings({"FieldCanBeLocal", "unused", "ConstantConditions", "RedundantSuppression"})
public class AddStudentActivity extends AppCompatActivity {

    private static final String TAG = "AddStudentActivity";

    private EditText etStudentFirstName, etStudentMiddleName, etStudentLastName, etStudentExtension;
    private EditText etParentFirstName, etParentMiddleName, etParentLastName, etParentExtension;
    private EditText etParentAddress, etParentContact;
    private RelativeLayout layoutGenderDropdown, layoutGradeLevelDropdown;
    private TextView tvSelectedGender, tvSelectedGradeLevel;
    private TextView tvDateOfBirth, tvCalculatedAge, tvHeaderTitle;
    private RelativeLayout btnDateOfBirth;
    private ImageView imgStudentPhoto;
    private Button btnSave;

    private DatabaseHelper dbHelper;
    private boolean isEditMode = false;
    private int studentId = -1;
    private long editYearId = -1;
    private SchoolYearBar schoolYearBar;
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
            isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);
            editYearId = getIntent().getLongExtra("SCHOOL_YEAR_ID", -1L);
            if (editYearId > 0 && dbHelper.getSchoolYear(editYearId) != null) dbHelper.selectSchoolYear(editYearId);
            // Adding: the year can be switched here. Editing: shown only, the student belongs to that year.
            schoolYearBar = SchoolYearBar.attach(this, dbHelper, !isEditMode, false, null);

            ImageView btnBack = findViewById(R.id.btnBack);
            tvHeaderTitle = findViewById(R.id.tvHeaderTitle);

            imgStudentPhoto = findViewById(R.id.studentImageView);

            etStudentFirstName = findViewById(R.id.etStudentFirstName);
            etStudentMiddleName = findViewById(R.id.etStudentMiddleName);
            etStudentLastName = findViewById(R.id.etStudentLastName);
            etStudentExtension = findViewById(R.id.etStudentExtension);
            layoutGenderDropdown = findViewById(R.id.layoutGenderDropdown);
            tvSelectedGender = findViewById(R.id.tvSelectedGender);

            btnDateOfBirth = findViewById(R.id.btnDateOfBirth);
            tvDateOfBirth = findViewById(R.id.tvDateOfBirth);
            tvCalculatedAge = findViewById(R.id.tvCalculatedAge);

            layoutGradeLevelDropdown = findViewById(R.id.layoutGradeLevelDropdown);
            tvSelectedGradeLevel = findViewById(R.id.tvSelectedGradeLevel);


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

                                if (tvCalculatedAge != null) {
                                    int age = AgeCalculator.ageToday(formattedDob);
                                    tvCalculatedAge.setText(age >= 0 ? String.valueOf(age) : "-");
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

                String editDob = getIntent().getStringExtra("STUDENT_DOB");
                if (editDob != null && tvDateOfBirth != null) tvDateOfBirth.setText(editDob);
                if (tvCalculatedAge != null) {
                    int editAge = AgeCalculator.ageToday(editDob);
                    tvCalculatedAge.setText(editAge >= 0 ? String.valueOf(editAge) : "-");
                }

                String editAddress = getIntent().getStringExtra("STUDENT_ADDRESS");
                if (editAddress != null && etParentAddress != null) etParentAddress.setText(editAddress);

                String editContact = getIntent().getStringExtra("STUDENT_CONTACT");
                if (editContact != null && etParentContact != null) {
                    String cleanContact = editContact.replace("+63", "").trim();
                    etParentContact.setText(cleanContact);
                }

                String editGender = getIntent().getStringExtra("STUDENT_GENDER");
                if (editGender != null && tvSelectedGender != null) {
                    tvSelectedGender.setText(editGender);
                }

                String editClass = getIntent().getStringExtra("STUDENT_CLASS");
                if (editClass != null && tvSelectedGradeLevel != null) {
                    if (editClass.contains("-")) {
                        editClass = editClass.split("-")[0].trim();
                    }
                    tvSelectedGradeLevel.setText(editClass);
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

            if (layoutGenderDropdown != null && tvSelectedGender != null) {
                layoutGenderDropdown.setOnClickListener(v -> {
                    showCustomDropdown(v, tvSelectedGender, genders, selectedGender -> {
                        tvSelectedGender.setText(selectedGender);
                    });
                });
            }

            if (layoutGradeLevelDropdown != null && tvSelectedGradeLevel != null) {
                layoutGradeLevelDropdown.setOnClickListener(v -> {
                    showCustomDropdown(v, tvSelectedGradeLevel, grades, selectedGrade -> {
                        tvSelectedGradeLevel.setText(selectedGrade);
                    });
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up spinners", e);
        }
    }

    interface OnItemSelectedListener {
        void onItemSelected(String item);
    }

    private void showCustomDropdown(View anchor, TextView targetTextView, String[] items, OnItemSelectedListener listener) {
        String currentSelected = targetTextView != null ? targetTextView.getText().toString() : "";

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                items
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(android.graphics.Color.BLACK);
                view.setTextSize(16);
                view.setPadding(32, 24, 32, 24);

                String itemText = getItem(position);
                if (itemText != null && itemText.equals(currentSelected)) {
                    view.setBackgroundResource(R.drawable.dropdown_selected);
                } else {
                    view.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                }
                return view;
            }
        };

        android.widget.ListPopupWindow popupWindow = new android.widget.ListPopupWindow(this);
        popupWindow.setAdapter(adapter);
        popupWindow.setAnchorView(anchor);
        popupWindow.setBackgroundDrawable(androidx.core.content.res.ResourcesCompat.getDrawable(getResources(), R.drawable.dropdown_box, null));
        popupWindow.setWidth(anchor.getWidth());
        popupWindow.setHeight(android.widget.ListPopupWindow.WRAP_CONTENT);
        popupWindow.setModal(true);

        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(currentSelected)) {
                popupWindow.setSelection(i);
                break;
            }
        }

        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            String selected = items[position];
            listener.onItemSelected(selected);
            popupWindow.dismiss();
        });
        popupWindow.show();
    }

    private void saveStudentData() {
        try {
            String studentFirstName = etStudentFirstName != null ? etStudentFirstName.getText().toString().trim() : "";
            String studentMiddleName = etStudentMiddleName != null ? etStudentMiddleName.getText().toString().trim() : "";
            String studentLastName = etStudentLastName != null ? etStudentLastName.getText().toString().trim() : "";
            String studentExtension = etStudentExtension != null ? etStudentExtension.getText().toString().trim() : "";
            String studentGender = tvSelectedGender != null ? tvSelectedGender.getText().toString().trim() : "Male";

            String dob = tvDateOfBirth != null ? tvDateOfBirth.getText().toString().trim() : "";
            int age = AgeCalculator.ageToday(dob);

            String gradeLevel = tvSelectedGradeLevel != null ? tvSelectedGradeLevel.getText().toString().trim() : "Nursery";

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

            if (age < 0) {
                Toast.makeText(this, "Please select a valid date of birth (not in the future).", Toast.LENGTH_SHORT).show();
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

            String parentContact = "+63 " + parentContactDigits;

            Form f = new Form();
            f.first = studentFirstName; f.middle = studentMiddleName; f.last = studentLastName; f.ext = studentExtension;
            f.dob = dob; f.sex = studentGender; f.age = String.valueOf(age);
            f.gFirst = parentFirstName; f.gMiddle = parentMiddleName; f.gLast = parentLastName; f.gExt = parentExtension;
            f.contact = parentContact; f.address = parentAddress; f.level = gradeLevel;

            if (isEditMode && studentId != -1) {
                long yearId = editYearId > 0 ? editYearId : dbHelper.getSelectedSchoolYearId();
                boolean ok = dbHelper.updateStudent(studentId, f.first, f.middle, f.last, f.ext, f.dob, f.sex,
                        f.gFirst, f.gMiddle, f.gLast, f.gExt, f.contact, f.address, photoUriString, f.level, yearId);
                finishSave(ok, studentId, f);
                return;
            }

            // Adding. Never create a second copy of someone who is already in the records.
            final long yearId = dbHelper.getSelectedSchoolYearId();
            final String yearLabel = dbHelper.getSelectedSchoolYear().label;
            long existing = dbHelper.findStudentByIdentity(f.first, f.last, f.dob);
            if (existing > 0 && dbHelper.isEnrolled(existing, yearId)) {
                Toast.makeText(this, f.first + " " + f.last + " is already enrolled in " + yearLabel + ".",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (existing > 0) {
                final long existingId = existing;
                new AlertDialog.Builder(this)
                        .setTitle("Student already in your records")
                        .setMessage(f.first + " " + f.last + " (same birthdate) is already saved from another school year. "
                                + "Enroll the same student in " + yearLabel + " instead of creating a second copy?")
                        .setPositiveButton("Enroll existing student", (d, w) -> {
                            boolean ok = dbHelper.updateStudent(existingId, f.first, f.middle, f.last, f.ext, f.dob,
                                    f.sex, f.gFirst, f.gMiddle, f.gLast, f.gExt, f.contact, f.address, photoUriString,
                                    f.level, yearId);
                            finishSave(ok, existingId, f);
                        })
                        .setNegativeButton("Add as new student", (d, w) -> addNew(f, yearId))
                        .setNeutralButton("Cancel", null)
                        .show();
                return;
            }
            addNew(f, yearId);

        } catch (Exception e) {
            Log.e(TAG, "Error in save click listener", e);
            Toast.makeText(this, "Unable to save student. Please check inputs.", Toast.LENGTH_SHORT).show();
        }
    }

    private static final class Form {
        String first, middle, last, ext, dob, sex, age, gFirst, gMiddle, gLast, gExt, contact, address, level;
    }

    private void addNew(Form f, long yearId) {
        long id = dbHelper.insertStudent(f.first, f.middle, f.last, f.ext, f.dob, f.sex, f.gFirst, f.gMiddle, f.gLast,
                f.gExt, f.contact, f.address, photoUriString, f.level, yearId);
        finishSave(id > 0, id, f);
    }

    private void finishSave(boolean success, long id, Form f) {
        if (!success) {
            String err = dbHelper.getLastError();
            Log.e(TAG, "Save failed: " + err);
            Toast.makeText(this, "Database operation failed. Please try again."
                    + (err.isEmpty() ? "" : "\n(" + err + ")"), Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, isEditMode ? "Student updated successfully!" : "Student added to school year "
                + dbHelper.getSelectedSchoolYear().label + "!", Toast.LENGTH_SHORT).show();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("STUDENT_ID", (int) id);
        resultIntent.putExtra("STUDENT_FIRST_NAME", f.first);
        resultIntent.putExtra("STUDENT_MIDDLE_NAME", f.middle);
        resultIntent.putExtra("STUDENT_LAST_NAME", f.last);
        resultIntent.putExtra("STUDENT_EXTENSION", f.ext);
        resultIntent.putExtra("STUDENT_NAME", f.first + " " + f.last);
        resultIntent.putExtra("STUDENT_CLASS", f.level);
        resultIntent.putExtra("STUDENT_GENDER", f.sex);
        resultIntent.putExtra("STUDENT_AGE", f.age);
        resultIntent.putExtra("STUDENT_DOB", f.dob);
        resultIntent.putExtra("SCHOOL_YEAR_ID", dbHelper.getSelectedSchoolYearId());

        resultIntent.putExtra("PARENT_FIRST_NAME", f.gFirst);
        resultIntent.putExtra("PARENT_MIDDLE_NAME", f.gMiddle);
        resultIntent.putExtra("PARENT_LAST_NAME", f.gLast);
        resultIntent.putExtra("PARENT_EXTENSION", f.gExt);
        resultIntent.putExtra("STUDENT_PARENT", com.example.priangradingsystem.data.NameSplitter.join(f.gFirst, f.gMiddle, f.gLast, f.gExt));

        resultIntent.putExtra("STUDENT_ADDRESS", f.address);
        resultIntent.putExtra("STUDENT_CONTACT", f.contact);
        resultIntent.putExtra("STUDENT_PHOTO_URI", photoUriString);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (schoolYearBar != null) schoolYearBar.refresh();
    }
}
