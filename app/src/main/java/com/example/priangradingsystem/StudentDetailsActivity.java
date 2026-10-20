package com.example.priangradingsystem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

@SuppressWarnings({"SetTextI18n", "ConcatenationWithLocaleDependantString", "MethodLength"})
public class StudentDetailsActivity extends AppCompatActivity {

    private static final String TAG = "StudentDetailsActivity";

    private String studentName = "";
    private String studentClass = "";
    private String studentGender = "";
    private String studentAge = "";
    private String studentDob = "";
    private String studentParent = "";
    private String studentAddress = "";
    private String studentContact = "";
    private String studentPhotoUri = "";

    private ImageView studentImageView;
    private TextView nameTextView, subtitleTextView, genderTextView, ageTextView, dobTextView;
    private TextView parentTextView, addressTextView, contactTextView;
    private TextView studentInfoTitle, parentInfoTitle;
    private CardView studentInfoCard, parentInfoCard;

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

        studentName = data.getStringExtra("STUDENT_NAME");
        studentClass = data.getStringExtra("STUDENT_CLASS");
        studentAge = data.getStringExtra("STUDENT_AGE");
        studentGender = data.getStringExtra("STUDENT_GENDER");
        studentDob = data.getStringExtra("STUDENT_DOB");
        studentParent = data.getStringExtra("STUDENT_PARENT");
        studentAddress = data.getStringExtra("STUDENT_ADDRESS");
        studentContact = data.getStringExtra("STUDENT_CONTACT");
        studentPhotoUri = data.getStringExtra("STUDENT_PHOTO_URI");

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
        return updatedIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_details);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        Intent intent = getIntent();
        if (intent != null) {
            studentName = intent.getStringExtra("STUDENT_NAME");
            studentClass = intent.getStringExtra("STUDENT_CLASS");
            studentGender = intent.getStringExtra("STUDENT_GENDER");
            studentAge = intent.getStringExtra("STUDENT_AGE");
            studentDob = intent.getStringExtra("STUDENT_DOB");
            studentParent = intent.getStringExtra("STUDENT_PARENT");
            studentAddress = intent.getStringExtra("STUDENT_ADDRESS");
            studentContact = intent.getStringExtra("STUDENT_CONTACT");
            studentPhotoUri = intent.getStringExtra("STUDENT_PHOTO_URI");
        }

        studentImageView = findViewById(R.id.studentImageView);
        nameTextView = findViewById(R.id.studentNameTextView);
        subtitleTextView = findViewById(R.id.studentSubtitleTextView);

        genderTextView = findViewById(R.id.genderTextView);
        ageTextView = findViewById(R.id.ageTextView);
        dobTextView = findViewById(R.id.dobTextView);

        parentTextView = findViewById(R.id.parentTextView);
        addressTextView = findViewById(R.id.addressTextView);
        contactTextView = findViewById(R.id.contactTextView);

        studentInfoTitle = findViewById(R.id.studentInfoTitle);
        studentInfoCard = findViewById(R.id.studentInfoCard);
        parentInfoTitle = findViewById(R.id.parentInfoTitle);
        parentInfoCard = findViewById(R.id.parentInfoCard);

        Button editButton = findViewById(R.id.editButton);
        Button deleteButton = findViewById(R.id.deleteButton);

        populateUI();

        if (editButton != null) {
            editButton.setOnClickListener(v -> {
                try {
                    Intent editIntent = new Intent(StudentDetailsActivity.this, AddStudentActivity.class);
                    editIntent.putExtra("IS_EDIT_MODE", true);
                    editIntent.putExtra("STUDENT_NAME", studentName);
                    editIntent.putExtra("STUDENT_CLASS", studentClass);
                    editIntent.putExtra("STUDENT_GENDER", studentGender);
                    editIntent.putExtra("STUDENT_AGE", studentAge);
                    editIntent.putExtra("STUDENT_DOB", studentDob);
                    editIntent.putExtra("STUDENT_PARENT", studentParent);
                    editIntent.putExtra("STUDENT_ADDRESS", studentAddress);
                    editIntent.putExtra("STUDENT_CONTACT", studentContact);
                    editIntent.putExtra("STUDENT_PHOTO_URI", studentPhotoUri);
                    editStudentLauncher.launch(editIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching AddStudentActivity for edit", e);
                }
            });
        }

        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                Toast.makeText(this, "Student deleted", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("deleted_name", studentName);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
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

        if (nameTextView != null) {
            if (studentName != null && !studentName.trim().isEmpty()) {
                nameTextView.setText(studentName);
                nameTextView.setVisibility(View.VISIBLE);
            } else {
                nameTextView.setVisibility(View.GONE);
            }
        }

        if (subtitleTextView != null) {
            if (studentClass != null && !studentClass.trim().isEmpty()) {
                String cleanedClass = studentClass;
                if (cleanedClass.startsWith("Nursery - Nursery")) {
                    cleanedClass = cleanedClass.replace("Nursery - Nursery", "Nursery");
                } else if (cleanedClass.startsWith("Pre-K - Pre-K")) {
                    cleanedClass = cleanedClass.replace("Pre-K - Pre-K", "Pre-K");
                }
                subtitleTextView.setText(cleanedClass);
                subtitleTextView.setVisibility(View.VISIBLE);
            } else {
                subtitleTextView.setVisibility(View.GONE);
            }
        }

        boolean hasGender = studentGender != null && !studentGender.trim().isEmpty() && !studentGender.equalsIgnoreCase("Not specified") && !studentGender.equalsIgnoreCase("N/A");
        boolean hasAge = studentAge != null && !studentAge.trim().isEmpty() && !studentAge.equalsIgnoreCase("N/A");
        boolean hasDob = studentDob != null && !studentDob.trim().isEmpty() && !studentDob.equalsIgnoreCase("N/A");

        if (genderTextView != null) {
            if (hasGender) {
                genderTextView.setText("Gender: " + studentGender);
                genderTextView.setVisibility(View.VISIBLE);
            } else {
                genderTextView.setVisibility(View.GONE);
            }
        }

        if (ageTextView != null) {
            if (hasAge) {
                String displayAge = studentAge.startsWith("Age ") ? studentAge : "Age: " + studentAge;
                ageTextView.setText(displayAge);
                ageTextView.setVisibility(View.VISIBLE);
            } else {
                ageTextView.setVisibility(View.GONE);
            }
        }

        if (dobTextView != null) {
            if (hasDob) {
                dobTextView.setText("Date of Birth: " + studentDob);
                dobTextView.setVisibility(View.VISIBLE);
            } else {
                dobTextView.setVisibility(View.GONE);
            }
        }

        if (!hasGender && !hasAge && !hasDob) {
            if (studentInfoTitle != null) studentInfoTitle.setVisibility(View.GONE);
            if (studentInfoCard != null) studentInfoCard.setVisibility(View.GONE);
        } else {
            if (studentInfoTitle != null) studentInfoTitle.setVisibility(View.VISIBLE);
            if (studentInfoCard != null) studentInfoCard.setVisibility(View.VISIBLE);
        }

        boolean hasParent = studentParent != null && !studentParent.trim().isEmpty() && !studentParent.equalsIgnoreCase("N/A");
        boolean hasAddress = studentAddress != null && !studentAddress.trim().isEmpty() && !studentAddress.equalsIgnoreCase("N/A");
        boolean hasContact = studentContact != null && !studentContact.trim().isEmpty() && !studentContact.equalsIgnoreCase("N/A");

        if (parentTextView != null) {
            if (hasParent) {
                parentTextView.setText("Name: " + studentParent);
                parentTextView.setVisibility(View.VISIBLE);
            } else {
                parentTextView.setVisibility(View.GONE);
            }
        }

        if (addressTextView != null) {
            if (hasAddress) {
                addressTextView.setText("Address: " + studentAddress);
                addressTextView.setVisibility(View.VISIBLE);
            } else {
                addressTextView.setVisibility(View.GONE);
            }
        }

        if (contactTextView != null) {
            if (hasContact) {
                contactTextView.setText("Contact No: " + studentContact);
                contactTextView.setVisibility(View.VISIBLE);
            } else {
                contactTextView.setVisibility(View.GONE);
            }
        }

        if (!hasParent && !hasAddress && !hasContact) {
            if (parentInfoTitle != null) parentInfoTitle.setVisibility(View.GONE);
            if (parentInfoCard != null) parentInfoCard.setVisibility(View.GONE);
        } else {
            if (parentInfoTitle != null) parentInfoTitle.setVisibility(View.VISIBLE);
            if (parentInfoCard != null) parentInfoCard.setVisibility(View.VISIBLE);
        }
    }
}