package com.example.priangradingsystem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.priangradingsystem.data.DatabaseHelper;
import com.example.priangradingsystem.students.AddStudentActivity;

public class StudentDetailsActivity extends AppCompatActivity {

    private static final String TAG = "StudentDetailsActivity";

    private DatabaseHelper dbHelper;

    private int studentId = -1;
    private String studentName = "";
    private String studentClass = "";
    private String studentGender = "";
    private String studentAge = "";
    private String studentDob = "";
    private String studentParent = "";
    private String studentAddress = "";
    private String studentContact = "";
    private String studentPhotoUri = "";

    private String firstName = "";
    private String middleName = "";
    private String lastName = "";
    private String extension = "";
    private String parentFirstName = "";
    private String parentMiddleName = "";
    private String parentLastName = "";
    private String parentExtension = "";

    private ImageView studentImageView;
    private TextView genderTextView, ageTextView, dobTextView;
    private TextView tvStudentFullNameHeader, tvStudentSectionHeader;
    private TextView tvFirstNameVal, tvMiddleNameVal, tvLastNameVal, tvExtensionVal;
    private TextView tvParentFirstNameVal, tvParentMiddleNameVal, tvParentLastNameVal, tvParentExtensionVal;
    private TextView contactTextView;
    private TextView addressTextView;

    private final ActivityResultLauncher<Intent> editStudentLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                try {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleEditResult(result.getData());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in editStudentLauncher", e);
                }
            });

    private void handleEditResult(Intent data) {
        String oldName = studentName;

        studentId = data.getIntExtra("STUDENT_ID", studentId);
        studentName = data.getStringExtra("STUDENT_NAME");

        String rawClass = data.getStringExtra("STUDENT_CLASS");
        if (rawClass != null && rawClass.contains("-")) {
            rawClass = rawClass.split("-")[0].trim();
        }
        studentClass = rawClass != null ? rawClass : "Nursery";

        studentAge = data.getStringExtra("STUDENT_AGE");
        studentGender = data.getStringExtra("STUDENT_GENDER");
        studentDob = data.getStringExtra("STUDENT_DOB");
        studentParent = data.getStringExtra("STUDENT_PARENT");
        studentAddress = data.getStringExtra("STUDENT_ADDRESS");
        studentContact = data.getStringExtra("STUDENT_CONTACT");
        studentPhotoUri = data.getStringExtra("STUDENT_PHOTO_URI");

        firstName = data.getStringExtra("STUDENT_FIRST_NAME");
        middleName = data.getStringExtra("STUDENT_MIDDLE_NAME");
        lastName = data.getStringExtra("STUDENT_LAST_NAME");
        extension = data.getStringExtra("STUDENT_EXTENSION");

        parentFirstName = data.getStringExtra("PARENT_FIRST_NAME");
        parentMiddleName = data.getStringExtra("PARENT_MIDDLE_NAME");
        parentLastName = data.getStringExtra("PARENT_LAST_NAME");
        parentExtension = data.getStringExtra("PARENT_EXTENSION");

        parseParentNameIfNeeded();
        populateUI();

        Intent updatedIntent = createUpdatedIntent(oldName);
        setResult(RESULT_OK, updatedIntent);
    }

    private Intent createUpdatedIntent(String oldName) {
        Intent updatedIntent = new Intent();
        updatedIntent.putExtra("updated_name_old", oldName);
        updatedIntent.putExtra("updated_name", studentName);
        updatedIntent.putExtra("updated_class", studentClass);
        updatedIntent.putExtra("updated_age", studentAge);
        updatedIntent.putExtra("updated_gender", studentGender);
        updatedIntent.putExtra("updated_dob", studentDob);
        updatedIntent.putExtra("updated_parent", studentParent);
        updatedIntent.putExtra("updated_address", studentAddress);
        updatedIntent.putExtra("updated_contact", studentContact);
        updatedIntent.putExtra("updated_photo", studentPhotoUri);

        updatedIntent.putExtra("STUDENT_FIRST_NAME", firstName);
        updatedIntent.putExtra("STUDENT_MIDDLE_NAME", middleName);
        updatedIntent.putExtra("STUDENT_LAST_NAME", lastName);
        updatedIntent.putExtra("STUDENT_EXTENSION", extension);

        updatedIntent.putExtra("PARENT_FIRST_NAME", parentFirstName);
        updatedIntent.putExtra("PARENT_MIDDLE_NAME", parentMiddleName);
        updatedIntent.putExtra("PARENT_LAST_NAME", parentLastName);
        updatedIntent.putExtra("PARENT_EXTENSION", parentExtension);
        return updatedIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_details);

        dbHelper = new DatabaseHelper(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        Intent intent = getIntent();
        if (intent != null) {
            studentId = intent.getIntExtra("STUDENT_ID", -1);
            studentName = intent.getStringExtra("STUDENT_NAME");

            String rawClass = intent.getStringExtra("STUDENT_CLASS");
            if (rawClass != null && rawClass.contains("-")) {
                rawClass = rawClass.split("-")[0].trim();
            }
            studentClass = rawClass != null ? rawClass : "Nursery";

            studentGender = intent.getStringExtra("STUDENT_GENDER");
            studentAge = intent.getStringExtra("STUDENT_AGE");
            studentDob = intent.getStringExtra("STUDENT_DOB");
            studentParent = intent.getStringExtra("STUDENT_PARENT");
            studentAddress = intent.getStringExtra("STUDENT_ADDRESS");
            studentContact = intent.getStringExtra("STUDENT_CONTACT");
            studentPhotoUri = intent.getStringExtra("STUDENT_PHOTO_URI");

            firstName = intent.getStringExtra("STUDENT_FIRST_NAME");
            middleName = intent.getStringExtra("STUDENT_MIDDLE_NAME");
            lastName = intent.getStringExtra("STUDENT_LAST_NAME");
            extension = intent.getStringExtra("STUDENT_EXTENSION");

            parentFirstName = intent.getStringExtra("PARENT_FIRST_NAME");
            parentMiddleName = intent.getStringExtra("PARENT_MIDDLE_NAME");
            parentLastName = intent.getStringExtra("PARENT_LAST_NAME");
            parentExtension = intent.getStringExtra("PARENT_EXTENSION");
        }

        parseParentNameIfNeeded();

        studentImageView = findViewById(R.id.studentImageView);
        genderTextView = findViewById(R.id.genderTextView);
        ageTextView = findViewById(R.id.ageTextView);
        dobTextView = findViewById(R.id.dobTextView);

        tvStudentFullNameHeader = findViewById(R.id.tvStudentFullNameHeader);
        tvStudentSectionHeader = findViewById(R.id.tvStudentSectionHeader);

        tvFirstNameVal = findViewById(R.id.tvFirstNameVal);
        tvMiddleNameVal = findViewById(R.id.tvMiddleNameVal);
        tvLastNameVal = findViewById(R.id.tvLastNameVal);
        tvExtensionVal = findViewById(R.id.tvExtensionVal);

        tvParentFirstNameVal = findViewById(R.id.tvParentFirstNameVal);
        tvParentMiddleNameVal = findViewById(R.id.tvParentMiddleNameVal);
        tvParentLastNameVal = findViewById(R.id.tvParentLastNameVal);
        tvParentExtensionVal = findViewById(R.id.tvParentExtensionVal);
        contactTextView = findViewById(R.id.contactTextView);
        addressTextView = findViewById(R.id.addressTextView);

        Button editButton = findViewById(R.id.editButton);
        Button deleteButton = findViewById(R.id.deleteButton);

        populateUI();

        if (editButton != null) {
            editButton.setOnClickListener(v -> {
                try {
                    Intent editIntent = new Intent(StudentDetailsActivity.this, AddStudentActivity.class);
                    editIntent.putExtra("IS_EDIT_MODE", true);
                    editIntent.putExtra("STUDENT_ID", studentId);
                    editIntent.putExtra("STUDENT_NAME", studentName);
                    editIntent.putExtra("STUDENT_CLASS", studentClass);
                    editIntent.putExtra("STUDENT_GENDER", studentGender);
                    editIntent.putExtra("STUDENT_AGE", studentAge);
                    editIntent.putExtra("STUDENT_DOB", studentDob);
                    editIntent.putExtra("STUDENT_PARENT", studentParent);
                    editIntent.putExtra("STUDENT_ADDRESS", studentAddress);
                    editIntent.putExtra("STUDENT_CONTACT", studentContact);
                    editIntent.putExtra("STUDENT_PHOTO_URI", studentPhotoUri);

                    editIntent.putExtra("STUDENT_FIRST_NAME", firstName);
                    editIntent.putExtra("STUDENT_MIDDLE_NAME", middleName);
                    editIntent.putExtra("STUDENT_LAST_NAME", lastName);
                    editIntent.putExtra("STUDENT_EXTENSION", extension);

                    editIntent.putExtra("PARENT_FIRST_NAME", parentFirstName);
                    editIntent.putExtra("PARENT_MIDDLE_NAME", parentMiddleName);
                    editIntent.putExtra("PARENT_LAST_NAME", parentLastName);
                    editIntent.putExtra("PARENT_EXTENSION", parentExtension);

                    editStudentLauncher.launch(editIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching AddStudentActivity for edit", e);
                }
            });
        }

        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                try {
                    if (studentId != -1) {
                        boolean deleted = dbHelper.deleteStudent(studentId);
                        if (deleted) {
                            Toast.makeText(this, "Student deleted successfully", Toast.LENGTH_SHORT).show();
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("deleted_name", studentName);
                            resultIntent.putExtra("STUDENT_ID", studentId);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } else {
                            Toast.makeText(this, "Failed to delete student from database", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Invalid student ID", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting student", e);
                    Toast.makeText(this, "Error deleting student", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void parseParentNameIfNeeded() {
        if ((parentFirstName == null || parentFirstName.trim().isEmpty()) &&
                studentParent != null && !studentParent.trim().isEmpty()) {
            String[] parts = studentParent.trim().split("\\s+");
            if (parts.length == 1) {
                parentFirstName = parts[0];
            } else if (parts.length == 2) {
                parentFirstName = parts[0];
                parentLastName = parts[1];
            } else if (parts.length >= 3) {
                parentFirstName = parts[0];
                parentMiddleName = parts[1];
                parentLastName = parts[parts.length - 1];
            }
        }
    }

    private void populateUI() {
        if (studentImageView != null) {
            if (studentPhotoUri != null && !studentPhotoUri.trim().isEmpty()) {
                try {
                    Uri uri = Uri.parse(studentPhotoUri);
                    studentImageView.setImageURI(uri);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load photo URI: " + studentPhotoUri, e);
                    studentImageView.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                studentImageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        String fullDisplay = (firstName != null ? firstName : "") +
                (middleName != null && !middleName.isEmpty() ? " " + middleName : "") +
                (lastName != null ? " " + lastName : "") +
                (extension != null && !extension.isEmpty() ? " " + extension : "");
        if (fullDisplay.trim().isEmpty()) fullDisplay = studentName;

        if (tvStudentFullNameHeader != null) {
            tvStudentFullNameHeader.setText(!fullDisplay.trim().isEmpty() ? fullDisplay : "Student Name");
        }

        if (tvStudentSectionHeader != null) {
            String cleanLvl = studentClass != null ? studentClass : "";
            if (cleanLvl.contains("-")) {
                cleanLvl = cleanLvl.split("-")[0].trim();
            }
            tvStudentSectionHeader.setText(!cleanLvl.isEmpty() ? cleanLvl : "Level");
        }

        if (dobTextView != null) dobTextView.setText(studentDob != null ? studentDob : "-");
        if (ageTextView != null) ageTextView.setText(studentAge != null ? studentAge : "-");
        if (genderTextView != null) genderTextView.setText(studentGender != null ? studentGender : "-");

        if (tvFirstNameVal != null) tvFirstNameVal.setText(firstName != null && !firstName.isEmpty() ? firstName : "-");
        if (tvMiddleNameVal != null) tvMiddleNameVal.setText(middleName != null && !middleName.isEmpty() ? middleName : "-");
        if (tvLastNameVal != null) tvLastNameVal.setText(lastName != null && !lastName.isEmpty() ? lastName : "-");
        if (tvExtensionVal != null) tvExtensionVal.setText(extension != null && !extension.isEmpty() ? extension : "N/A");

        if (tvParentFirstNameVal != null) tvParentFirstNameVal.setText(parentFirstName != null && !parentFirstName.isEmpty() ? parentFirstName : "-");
        if (tvParentMiddleNameVal != null) tvParentMiddleNameVal.setText(parentMiddleName != null && !parentMiddleName.isEmpty() ? parentMiddleName : "-");
        if (tvParentLastNameVal != null) tvParentLastNameVal.setText(parentLastName != null && !parentLastName.isEmpty() ? parentLastName : "-");
        if (tvParentExtensionVal != null) tvParentExtensionVal.setText(parentExtension != null && !parentExtension.isEmpty() ? parentExtension : "N/A");

        if (contactTextView != null) contactTextView.setText(studentContact != null && !studentContact.isEmpty() ? studentContact : "-");
        if (addressTextView != null) addressTextView.setText(studentAddress != null && !studentAddress.isEmpty() ? studentAddress : "-");
    }
}