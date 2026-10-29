package com.example.priangradingsystem.students;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.priangradingsystem.R;
import com.example.priangradingsystem.StudentDetailsActivity;
import com.example.priangradingsystem.data.DatabaseHelper;
import com.example.priangradingsystem.model.StudentItem;
import com.example.priangradingsystem.adapter.StudentAdapter;
import com.example.priangradingsystem.records.FilterActivity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressLint("ObsoleteSdkInt")
@SuppressWarnings({"FieldCanBeLocal", "unused", "ConstantConditions", "OverlyCoupledClass", "deprecation", "RedundantSuppression"})
public class StudentsActivity extends AppCompatActivity {

    private static final String TAG = "StudentsActivity";

    private DatabaseHelper dbHelper;
    private StudentAdapter adapter;
    private final List<StudentItem> masterStudentList = new ArrayList<>();
    private final List<StudentItem> displayedStudentList = new ArrayList<>();

    private String currentSearchQuery = "";
    private String currentLevelFilter = "All";
    private String currentPerformanceFilter = "All";

    private final ActivityResultLauncher<Intent> addStudentLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                try {
                    if (result.getResultCode() == RESULT_OK) {
                        loadStudentsFromDatabase();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in addStudentLauncher", e);
                }
            });

    private final ActivityResultLauncher<Intent> studentDetailsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                try {
                    if (result.getResultCode() == RESULT_OK) {
                        loadStudentsFromDatabase();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in studentDetailsLauncher", e);
                }
            });

    private final ActivityResultLauncher<Intent> filterLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                try {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        currentLevelFilter = data.getStringExtra("LEVEL_FILTER") != null ? data.getStringExtra("LEVEL_FILTER") : "All";
                        currentPerformanceFilter = data.getStringExtra("PERFORMANCE_FILTER") != null ? data.getStringExtra("PERFORMANCE_FILTER") : "All";
                        applyCombinedFilters();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in filterLauncher", e);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(Color.parseColor("#FFC107"));
            }

            setContentView(R.layout.activity_student);
            dbHelper = new DatabaseHelper(this);

            ImageView btnBack = findViewById(R.id.btnBack);
            ImageView btnAdd = findViewById(R.id.btnAdd);
            ImageView btnFilter = findViewById(R.id.btnFilter);
            EditText etSearch = findViewById(R.id.etSearch);
            RecyclerView rvStudents = findViewById(R.id.rvStudents);

            adapter = new StudentAdapter(displayedStudentList, this::openStudentDetails);

            if (rvStudents != null) {
                rvStudents.setLayoutManager(new LinearLayoutManager(this));
                rvStudents.setAdapter(adapter);
            }

            loadStudentsFromDatabase();

            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }

            if (btnAdd != null) {
                btnAdd.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(StudentsActivity.this, AddStudentActivity.class);
                        addStudentLauncher.launch(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching AddStudentActivity", e);
                    }
                });
            }

            if (btnFilter != null) {
                btnFilter.setOnClickListener(v -> showFilterModal());
            }

            if (etSearch != null) {
                etSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        try {
                            currentSearchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                            applyCombinedFilters();
                        } catch (Exception e) {
                            Log.e(TAG, "Error in search text watcher", e);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Critical error in StudentsActivity onCreate", e);
        }
    }

    private void openStudentDetails(StudentItem studentItem) {
        try {
            if (studentItem == null || studentItem.getItemViewType() != StudentItem.TYPE_STUDENT) return;

            Intent intent = new Intent(StudentsActivity.this, StudentDetailsActivity.class);
            intent.putExtra("STUDENT_ID", studentItem.getId());
            intent.putExtra("STUDENT_NAME", studentItem.getName() != null ? studentItem.getName() : "");
            intent.putExtra("STUDENT_FIRST_NAME", studentItem.getFirstName());
            intent.putExtra("STUDENT_MIDDLE_NAME", studentItem.getMiddleName());
            intent.putExtra("STUDENT_LAST_NAME", studentItem.getLastName());
            intent.putExtra("STUDENT_EXTENSION", studentItem.getExtension());

            String cleanClass = studentItem.getLevel() != null ? studentItem.getLevel() : "";
            if (cleanClass.contains("-")) {
                cleanClass = cleanClass.split("-")[0].trim();
            }
            intent.putExtra("STUDENT_CLASS", cleanClass);

            intent.putExtra("STUDENT_GENDER", studentItem.getGender() != null ? studentItem.getGender() : "Male");
            intent.putExtra("STUDENT_AGE", studentItem.getAge() != null ? studentItem.getAge() : "");
            intent.putExtra("STUDENT_DOB", studentItem.getDob() != null ? studentItem.getDob() : "");

            String parentFullName = studentItem.getParentName() != null ? studentItem.getParentName() : "";
            intent.putExtra("STUDENT_PARENT", parentFullName);

            String pFirst = "";
            String pMiddle = "";
            String pLast = "";
            if (!parentFullName.isEmpty()) {
                String[] parts = parentFullName.trim().split("\\s+");
                if (parts.length == 1) {
                    pFirst = parts[0];
                } else if (parts.length == 2) {
                    pFirst = parts[0];
                    pLast = parts[1];
                } else if (parts.length >= 3) {
                    pFirst = parts[0];
                    pMiddle = parts[1];
                    pLast = parts[parts.length - 1];
                }
            }

            intent.putExtra("PARENT_FIRST_NAME", pFirst);
            intent.putExtra("PARENT_MIDDLE_NAME", pMiddle);
            intent.putExtra("PARENT_LAST_NAME", pLast);
            intent.putExtra("PARENT_EXTENSION", "");

            intent.putExtra("STUDENT_CONTACT", studentItem.getContact() != null ? studentItem.getContact() : "");
            intent.putExtra("STUDENT_ADDRESS", studentItem.getAddress() != null ? studentItem.getAddress() : "");
            intent.putExtra("STUDENT_PHOTO_URI", studentItem.getPhotoUri() != null ? studentItem.getPhotoUri() : "");

            studentDetailsLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening student details", e);
        }
    }

    private void loadStudentsFromDatabase() {
        try {
            masterStudentList.clear();
            Cursor cursor = dbHelper.getAllStudents();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("student_id"));
                    String fName = cursor.getString(cursor.getColumnIndexOrThrow("first_name"));
                    String mName = cursor.getString(cursor.getColumnIndexOrThrow("middle_name"));
                    String lName = cursor.getString(cursor.getColumnIndexOrThrow("last_name"));
                    String dob = cursor.getString(cursor.getColumnIndexOrThrow("birthdate"));
                    String gender = cursor.getString(cursor.getColumnIndexOrThrow("sex"));
                    String age = cursor.getString(cursor.getColumnIndexOrThrow("age"));
                    String guardian = cursor.getString(cursor.getColumnIndexOrThrow("guardian_name"));
                    String contact = cursor.getString(cursor.getColumnIndexOrThrow("guardian_contact"));
                    String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    String photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri"));

                    String level = "Nursery";
                    try {
                        int levelColIdx = cursor.getColumnIndex("level");
                        if (levelColIdx != -1) {
                            String dbLevel = cursor.getString(levelColIdx);
                            if (dbLevel != null && !dbLevel.trim().isEmpty()) {
                                level = dbLevel.trim();
                                if (level.contains("-")) {
                                    level = level.split("-")[0].trim();
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    StudentItem student = new StudentItem(StudentItem.TYPE_STUDENT, fName + " " + lName, age, level, "Good");
                    student.setId(id);
                    student.setFirstName(fName);
                    student.setMiddleName(mName);
                    student.setLastName(lName);
                    student.setGender(gender);
                    student.setDob(dob);
                    student.setParentName(guardian);
                    student.setContact(contact);
                    student.setAddress(address);
                    student.setPhotoUri(photoUri);
                    student.setLevel(level);

                    masterStudentList.add(student);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading students from database", e);
        }
        applyCombinedFilters();
    }

    private void showFilterModal() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_students, null);
        dialog.setContentView(dialogView);

        android.widget.RadioGroup rgLevelFilter = dialogView.findViewById(R.id.rgLevelFilter);
        android.widget.RadioButton rbLevelAll = dialogView.findViewById(R.id.rbLevelAll);
        android.widget.RadioButton rbNursery = dialogView.findViewById(R.id.rbNursery);
        android.widget.RadioButton rbToddler = dialogView.findViewById(R.id.rbToddler);

        if ("Nursery".equalsIgnoreCase(currentLevelFilter)) {
            rbNursery.setChecked(true);
        } else if ("Toddler".equalsIgnoreCase(currentLevelFilter) || "Pre-K".equalsIgnoreCase(currentLevelFilter)) {
            rbToddler.setChecked(true);
        } else {
            rbLevelAll.setChecked(true);
        }

        android.widget.Button btnApplyFilter = dialogView.findViewById(R.id.btnApplyFilter);
        android.widget.Button btnResetFilter = dialogView.findViewById(R.id.btnResetFilter);

        if (btnApplyFilter != null) {
            btnApplyFilter.setOnClickListener(v -> {
                int checkedId = rgLevelFilter.getCheckedRadioButtonId();
                if (checkedId == R.id.rbNursery) {
                    currentLevelFilter = "Nursery";
                } else if (checkedId == R.id.rbToddler) {
                    currentLevelFilter = "Toddler";
                } else {
                    currentLevelFilter = "All";
                }
                applyCombinedFilters();
                dialog.dismiss();
            });
        }

        if (btnResetFilter != null) {
            btnResetFilter.setOnClickListener(v -> {
                currentLevelFilter = "All";
                applyCombinedFilters();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applyCombinedFilters() {
        try {
            displayedStudentList.clear();

            List<String> levelTitles = new ArrayList<>();
            for (StudentItem item : masterStudentList) {
                if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                    String lvl = (item.getLevel() != null && !item.getLevel().trim().isEmpty()) ? item.getLevel().trim() : "Nursery";
                    if (lvl.contains("-")) {
                        lvl = lvl.split("-")[0].trim();
                    }
                    if (!levelTitles.contains(lvl)) {
                        levelTitles.add(lvl);
                    }
                }
            }
            levelTitles.sort(String::compareToIgnoreCase);

            if (levelTitles.isEmpty()) {
                levelTitles.add("Nursery");
            }

            for (String levelTitle : levelTitles) {
                List<StudentItem> maleStudents = new ArrayList<>();
                List<StudentItem> femaleStudents = new ArrayList<>();
                List<StudentItem> otherStudents = new ArrayList<>();

                boolean levelMatchesSearch = currentSearchQuery.isEmpty() || levelTitle.toLowerCase().contains(currentSearchQuery);

                for (StudentItem item : masterStudentList) {
                    if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                        String itemLvl = (item.getLevel() != null && !item.getLevel().trim().isEmpty()) ? item.getLevel().trim() : "Nursery";
                        if (itemLvl.contains("-")) {
                            itemLvl = itemLvl.split("-")[0].trim();
                        }

                        if (itemLvl.equalsIgnoreCase(levelTitle)) {
                            boolean matchesSearch = checkSearchMatch(item, levelMatchesSearch);

                            boolean matchesLevelFilter = currentLevelFilter.equalsIgnoreCase("All")
                                    || itemLvl.equalsIgnoreCase(currentLevelFilter);

                            boolean matchesPerf = currentPerformanceFilter.equalsIgnoreCase("All")
                                    || (item.getPerformance() != null && item.getPerformance().equalsIgnoreCase(currentPerformanceFilter));

                            if (matchesSearch && matchesLevelFilter && matchesPerf) {
                                String g = item.getGender() != null ? item.getGender().trim().toLowerCase() : "male";

                                if (g.contains("fem") || g.contains("girl") || g.equals("female")) {
                                    femaleStudents.add(item);
                                } else if (g.contains("oth") || g.equals("other")) {
                                    otherStudents.add(item);
                                } else {
                                    maleStudents.add(item);
                                }
                            }
                        }
                    }
                }

                maleStudents.sort(Comparator.comparing(StudentItem::getName, Comparator.nullsFirst(String::compareToIgnoreCase)));
                femaleStudents.sort(Comparator.comparing(StudentItem::getName, Comparator.nullsFirst(String::compareToIgnoreCase)));
                otherStudents.sort(Comparator.comparing(StudentItem::getName, Comparator.nullsFirst(String::compareToIgnoreCase)));

                if (!maleStudents.isEmpty() || !femaleStudents.isEmpty() || !otherStudents.isEmpty()) {
                    int iconResId = android.R.drawable.ic_menu_agenda;
                    displayedStudentList.add(new StudentItem(StudentItem.TYPE_HEADER, levelTitle, iconResId));

                    if (!maleStudents.isEmpty()) {
                        displayedStudentList.add(new StudentItem(StudentItem.TYPE_GENDER_HEADER, "Male", 0));
                        displayedStudentList.addAll(maleStudents);
                    }

                    if (!femaleStudents.isEmpty()) {
                        displayedStudentList.add(new StudentItem(StudentItem.TYPE_GENDER_HEADER, "Female", 0));
                        displayedStudentList.addAll(femaleStudents);
                    }

                    if (!otherStudents.isEmpty()) {
                        displayedStudentList.add(new StudentItem(StudentItem.TYPE_GENDER_HEADER, "Other", 0));
                        displayedStudentList.addAll(otherStudents);
                    }
                }
            }

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying combined filters", e);
        }
    }

    private boolean checkSearchMatch(StudentItem item, boolean levelMatchesSearch) {
        String query = currentSearchQuery.trim();

        if (query.equals("male")) {
            return item.getGender() != null && item.getGender().trim().equalsIgnoreCase("Male");
        }

        if (query.equals("female")) {
            return item.getGender() != null &&
                    (item.getGender().trim().equalsIgnoreCase("Female") ||
                            item.getGender().trim().toLowerCase().contains("fem"));
        }

        return query.isEmpty()
                || levelMatchesSearch
                || (item.getName() != null && item.getName().toLowerCase().contains(query));
    }
}