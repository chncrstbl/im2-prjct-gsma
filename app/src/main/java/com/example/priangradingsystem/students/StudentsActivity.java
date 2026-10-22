package com.example.priangradingsystem.students;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.priangradingsystem.R;
import com.example.priangradingsystem.StudentDetailsActivity;
import com.example.priangradingsystem.model.StudentItem;
import com.example.priangradingsystem.adapter.StudentAdapter;
import com.example.priangradingsystem.records.FilterActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressLint("ObsoleteSdkInt")
@SuppressWarnings({"FieldCanBeLocal", "unused", "ConstantConditions", "OverlyCoupledClass", "deprecation", "RedundantSuppression"})
public class StudentsActivity extends AppCompatActivity {

    private static final String TAG = "StudentsActivity";
    private static final String PREFS_NAME = "StudentPrefs";
    private static final String KEY_STUDENT_LIST = "master_student_list";

    private StudentAdapter adapter;
    private final List<StudentItem> masterStudentList = new ArrayList<>();
    private final List<StudentItem> displayedStudentList = new ArrayList<>();

    private String currentSearchQuery = "";
    private String currentLevelFilter = "All";
    private String currentPerformanceFilter = "All";

    private final ActivityResultLauncher<Intent> addStudentLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                try {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();

                        String name = data.getStringExtra("STUDENT_NAME");
                        if (name == null || name.trim().isEmpty()) name = "Unnamed Student";

                        String gradeSection = data.getStringExtra("STUDENT_CLASS");
                        if (gradeSection == null || gradeSection.trim().isEmpty()) gradeSection = "Nursery - Section A";

                        String age = data.getStringExtra("STUDENT_AGE");
                        if (age == null || age.trim().isEmpty()) age = "5";

                        String photoUri = data.getStringExtra("STUDENT_PHOTO_URI");
                        if (photoUri == null) photoUri = "";

                        String gender = data.getStringExtra("STUDENT_GENDER");
                        if (gender == null || gender.trim().isEmpty()) gender = "Male";

                        String dob = data.getStringExtra("STUDENT_DOB");
                        if (dob == null) dob = "";

                        String parentName = data.getStringExtra("STUDENT_PARENT");
                        if (parentName == null) parentName = "";

                        String address = data.getStringExtra("STUDENT_ADDRESS");
                        if (address == null) address = "";

                        String contact = data.getStringExtra("STUDENT_CONTACT");
                        if (contact == null) contact = "";

                        addNewStudentWithSection(name, gradeSection, age, photoUri, gender, dob, parentName, address, contact);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in addStudentLauncher", e);
                }
            });

    private final ActivityResultLauncher<Intent> studentDetailsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                try {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String deletedName = data.getStringExtra("deleted_name");
                        String updatedNameOld = data.getStringExtra("updated_name_old");

                        if (deletedName != null) {
                            masterStudentList.removeIf(item ->
                                    item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT &&
                                            deletedName.equalsIgnoreCase(item.getName())
                            );
                        } else if (updatedNameOld != null) {
                            for (StudentItem item : masterStudentList) {
                                if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT &&
                                        updatedNameOld.equalsIgnoreCase(item.getName())) {

                                    item.setName(data.getStringExtra("updated_name"));
                                    item.setAge(data.getStringExtra("updated_age"));
                                    item.setGender(data.getStringExtra("updated_gender"));
                                    item.setDob(data.getStringExtra("updated_dob"));
                                    item.setParentName(data.getStringExtra("updated_parent"));
                                    item.setAddress(data.getStringExtra("updated_address"));
                                    item.setContact(data.getStringExtra("updated_contact"));
                                    item.setPhotoUri(data.getStringExtra("updated_photo"));

                                    String gradeSection = data.getStringExtra("updated_class");
                                    if (gradeSection != null && gradeSection.contains("-")) {
                                        item.setLevel(gradeSection.split("-")[0].trim());
                                        item.setSection(gradeSection.trim());
                                    } else if (gradeSection != null) {
                                        item.setSection(gradeSection.trim());
                                    }
                                    break;
                                }
                            }
                        }
                        saveStudentsPermanently();
                        applyCombinedFilters();
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

            loadSavedStudents();

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
                btnFilter.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(StudentsActivity.this, FilterActivity.class);
                        intent.putExtra("CURRENT_LEVEL_FILTER", currentLevelFilter);
                        intent.putExtra("CURRENT_PERFORMANCE_FILTER", currentPerformanceFilter);
                        filterLauncher.launch(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching FilterActivity", e);
                    }
                });
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
            intent.putExtra("STUDENT_NAME", studentItem.getName() != null ? studentItem.getName() : "");

            String sectionStr = studentItem.getSection() != null && !studentItem.getSection().trim().isEmpty()
                    ? studentItem.getSection().trim()
                    : (studentItem.getLevel() != null ? studentItem.getLevel().trim() : "Nursery - Section A");

            intent.putExtra("STUDENT_CLASS", sectionStr);
            intent.putExtra("STUDENT_GENDER", studentItem.getGender() != null ? studentItem.getGender() : "Male");
            intent.putExtra("STUDENT_AGE", studentItem.getAge() != null ? studentItem.getAge() : "");
            intent.putExtra("STUDENT_DOB", studentItem.getDob() != null ? studentItem.getDob() : "");
            intent.putExtra("STUDENT_PARENT", studentItem.getParentName() != null ? studentItem.getParentName() : "");
            intent.putExtra("STUDENT_ADDRESS", studentItem.getAddress() != null ? studentItem.getAddress() : "");
            intent.putExtra("STUDENT_CONTACT", studentItem.getContact() != null ? studentItem.getContact() : "");
            intent.putExtra("STUDENT_PHOTO_URI", studentItem.getPhotoUri() != null ? studentItem.getPhotoUri() : "");

            studentDetailsLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening student details", e);
        }
    }

    private void addNewStudentWithSection(String name, String gradeSection, String age, String photoUri,
                                          String gender, String dob, String parentName, String address, String contact) {
        try {
            if (gradeSection == null || gradeSection.trim().isEmpty()) {
                gradeSection = "Nursery - Section A";
            }
            gradeSection = gradeSection.trim();
            String levelOnly = gradeSection.split("-")[0].trim();

            StudentItem newStudent = new StudentItem(StudentItem.TYPE_STUDENT, name, age, levelOnly, "Good");
            newStudent.setSection(gradeSection);
            newStudent.setGender(gender != null && !gender.trim().isEmpty() ? gender : "Male");

            if (photoUri != null && !photoUri.isEmpty()) newStudent.setPhotoUri(photoUri);
            if (dob != null) newStudent.setDob(dob);
            if (parentName != null) newStudent.setParentName(parentName);
            if (address != null) newStudent.setAddress(address);
            if (contact != null) newStudent.setContact(contact);

            masterStudentList.add(newStudent);
            saveStudentsPermanently();
            applyCombinedFilters();
        } catch (Exception e) {
            Log.e(TAG, "Error adding new student", e);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applyCombinedFilters() {
        try {
            displayedStudentList.clear();

            List<String> sectionTitles = new ArrayList<>();
            for (StudentItem item : masterStudentList) {
                if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                    if (item.getSection() == null || item.getSection().trim().isEmpty()) {
                        item.setSection("Nursery - Section A");
                    }
                    String sec = item.getSection().trim();
                    if (!sectionTitles.contains(sec)) {
                        sectionTitles.add(sec);
                    }
                }
            }
            sectionTitles.sort(String::compareToIgnoreCase);

            for (String sectionTitle : sectionTitles) {
                List<StudentItem> maleStudents = new ArrayList<>();
                List<StudentItem> femaleStudents = new ArrayList<>();
                List<StudentItem> otherStudents = new ArrayList<>();

                boolean sectionMatchesSearch = currentSearchQuery.isEmpty() || sectionTitle.toLowerCase().contains(currentSearchQuery);

                for (StudentItem item : masterStudentList) {
                    if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                        String itemSec = item.getSection() != null ? item.getSection().trim() : "Nursery - Section A";

                        if (itemSec.equalsIgnoreCase(sectionTitle)) {
                            boolean matchesSearch = checkSearchMatch(item, sectionMatchesSearch);

                            boolean matchesLevel = currentLevelFilter.equalsIgnoreCase("All")
                                    || (item.getLevel() != null && item.getLevel().equalsIgnoreCase(currentLevelFilter))
                                    || itemSec.toLowerCase().contains(currentLevelFilter.toLowerCase());

                            boolean matchesPerf = currentPerformanceFilter.equalsIgnoreCase("All")
                                    || (item.getPerformance() != null && item.getPerformance().equalsIgnoreCase(currentPerformanceFilter));

                            if (matchesSearch && matchesLevel && matchesPerf) {
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
                    int iconResId = sectionTitle.toLowerCase().contains("pre-k") ? android.R.drawable.ic_menu_compass : android.R.drawable.ic_menu_agenda;
                    displayedStudentList.add(new StudentItem(StudentItem.TYPE_HEADER, sectionTitle, iconResId));

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

    private boolean checkSearchMatch(StudentItem item, boolean sectionMatchesSearch) {
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
                || sectionMatchesSearch
                || (item.getName() != null && item.getName().toLowerCase().contains(query));
    }

    private void saveStudentsPermanently() {
        try {
            List<StudentItem> studentOnlyList = new ArrayList<>();
            for (StudentItem item : masterStudentList) {
                if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                    studentOnlyList.add(item);
                }
            }

            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            Gson gson = new Gson();
            String json = gson.toJson(studentOnlyList);
            editor.putString(KEY_STUDENT_LIST, json);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving students permanently", e);
        }
    }

    private void loadSavedStudents() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String json = sharedPreferences.getString(KEY_STUDENT_LIST, null);

            masterStudentList.clear();

            if (json != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<StudentItem>>() {}.getType();
                List<StudentItem> savedList = gson.fromJson(json, type);

                if (savedList != null) {
                    for (StudentItem item : savedList) {
                        if (item != null && item.getName() != null && !item.getName().trim().isEmpty()) {
                            if (item.getSection() == null || item.getSection().trim().isEmpty()) {
                                item.setSection("Nursery - Section A");
                            }
                            if (item.getGender() == null || item.getGender().trim().isEmpty()) {
                                item.setGender("Male");
                            }
                            masterStudentList.add(item);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading saved students safely", e);
        }
        applyCombinedFilters();
    }
}