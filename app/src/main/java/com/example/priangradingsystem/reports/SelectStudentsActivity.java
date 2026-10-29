package com.example.priangradingsystem.reports;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.adapter.StudentAdapter;
import com.example.priangradingsystem.data.DatabaseHelper;
import com.example.priangradingsystem.model.StudentItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SelectStudentsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "StudentPrefs";
    private static final String KEY_STUDENT_LIST = "master_student_list";

    private DatabaseHelper dbHelper;
    private StudentAdapter adapter;
    private final List<StudentItem> masterStudentList = new ArrayList<>();
    private final List<StudentItem> displayedStudentList = new ArrayList<>();

    private String currentSearchQuery = "";
    private String currentLevelFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_select_students);

        dbHelper = new DatabaseHelper(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnFilter = findViewById(R.id.btnFilter);
        EditText etSearch = findViewById(R.id.etSearch);
        RecyclerView rvStudents = findViewById(R.id.rvStudents);

        adapter = new StudentAdapter(displayedStudentList, item -> {
            if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                Intent intent = new Intent(SelectStudentsActivity.this, ReportsActivity.class);
                intent.putExtra("STUDENT_ID", (long) item.getId());
                intent.putExtra("STUDENT_NAME", item.getName());
                String level = item.getLevel() != null && !item.getLevel().isEmpty() ? item.getLevel() : item.getSection();
                intent.putExtra("STUDENT_LEVEL", level);
                if (item.getPhotoUri() != null) {
                    intent.putExtra("STUDENT_PHOTO_URI", item.getPhotoUri());
                }
                startActivity(intent);
            }
        });

        if (rvStudents != null) {
            rvStudents.setLayoutManager(new LinearLayoutManager(this));
            rvStudents.setAdapter(adapter);
        }

        loadSavedStudents();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
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
                    currentSearchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                    applyCombinedFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void showFilterModal() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_students, null);
        dialog.setContentView(dialogView);

        RadioGroup rgLevelFilter = dialogView.findViewById(R.id.rgLevelFilter);
        RadioButton rbLevelAll = dialogView.findViewById(R.id.rbLevelAll);
        RadioButton rbNursery = dialogView.findViewById(R.id.rbNursery);
        RadioButton rbToddler = dialogView.findViewById(R.id.rbToddler);

        if ("Nursery".equalsIgnoreCase(currentLevelFilter)) {
            rbNursery.setChecked(true);
        } else if ("Toddler".equalsIgnoreCase(currentLevelFilter) || "Pre-K".equalsIgnoreCase(currentLevelFilter)) {
            rbToddler.setChecked(true);
        } else {
            rbLevelAll.setChecked(true);
        }

        Button btnApplyFilter = dialogView.findViewById(R.id.btnApplyFilter);
        Button btnResetFilter = dialogView.findViewById(R.id.btnResetFilter);

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

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
        loadSavedStudents();
    }

    private void loadSavedStudents() {
        masterStudentList.clear();

        // 1. Query real students from SQLite Database
        try {
            if (dbHelper == null) {
                dbHelper = new DatabaseHelper(this);
            }
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

                    String displayName = ((fName != null ? fName : "") + " " + (lName != null ? lName : "")).trim();
                    if (displayName.isEmpty()) {
                        displayName = "Unnamed Student";
                    }
                    String ageStr = (age != null && !age.trim().isEmpty()) ? (age.toLowerCase().startsWith("age") ? age : "Age " + age) : "";

                    StudentItem student = new StudentItem(StudentItem.TYPE_STUDENT, displayName, ageStr, level, "Good");
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
                    student.setSection(level);

                    masterStudentList.add(student);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("SelectStudentsActivity", "Error loading students from database", e);
        }

        // 2. Query saved students from SharedPreferences
        try {
            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String json = sharedPreferences.getString(KEY_STUDENT_LIST, null);

            if (json != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<StudentItem>>() {}.getType();
                List<StudentItem> savedList = gson.fromJson(json, type);

                if (savedList != null) {
                    for (StudentItem item : savedList) {
                        if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                            if (item.getName() != null && item.getName().toLowerCase().startsWith("person ")) {
                                continue;
                            }

                            boolean exists = false;
                            for (StudentItem existing : masterStudentList) {
                                if (existing.getName() != null && existing.getName().equalsIgnoreCase(item.getName())) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                masterStudentList.add(item);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        applyCombinedFilters();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applyCombinedFilters() {
        displayedStudentList.clear();

        List<String> sectionTitles = new ArrayList<>();
        for (StudentItem item : masterStudentList) {
            if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                String sec = item.getLevel() != null && !item.getLevel().trim().isEmpty()
                        ? item.getLevel().trim()
                        : (item.getSection() != null ? item.getSection().trim() : "Nursery");

                if (sec.contains("-")) {
                    sec = sec.split("-")[0].trim();
                }

                if (!sectionTitles.contains(sec)) {
                    sectionTitles.add(sec);
                }
            }
        }

        if (!sectionTitles.contains("Nursery")) sectionTitles.add("Nursery");
        if (!sectionTitles.contains("Toddler") && !sectionTitles.contains("Pre-K")) sectionTitles.add("Toddler");

        sectionTitles.sort(String::compareToIgnoreCase);

        for (String sectionTitle : sectionTitles) {
            List<StudentItem> sectionStudents = new ArrayList<>();

            for (StudentItem item : masterStudentList) {
                if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                    String itemSec = item.getLevel() != null && !item.getLevel().trim().isEmpty()
                            ? item.getLevel().trim()
                            : (item.getSection() != null ? item.getSection().trim() : "Nursery");

                    if (itemSec.contains("-")) {
                        itemSec = itemSec.split("-")[0].trim();
                    }

                    if (itemSec.equalsIgnoreCase(sectionTitle)) {
                        boolean matchesSearch = currentSearchQuery.isEmpty()
                                || (item.getName() != null && item.getName().toLowerCase().contains(currentSearchQuery))
                                || sectionTitle.toLowerCase().contains(currentSearchQuery);

                        boolean matchesLevel = currentLevelFilter.equalsIgnoreCase("All")
                                || itemSec.equalsIgnoreCase(currentLevelFilter)
                                || (currentLevelFilter.equalsIgnoreCase("Toddler") && itemSec.equalsIgnoreCase("Pre-K"))
                                || (currentLevelFilter.equalsIgnoreCase("Pre-K") && itemSec.equalsIgnoreCase("Toddler"));

                        if (matchesSearch && matchesLevel) {
                            sectionStudents.add(item);
                        }
                    }
                }
            }

            sectionStudents.sort(Comparator.comparing(StudentItem::getName, String.CASE_INSENSITIVE_ORDER));

            if (!sectionStudents.isEmpty()) {
                int iconResId = (sectionTitle.equalsIgnoreCase("Toddler") || sectionTitle.equalsIgnoreCase("Pre-K"))
                        ? android.R.drawable.ic_menu_compass
                        : android.R.drawable.ic_menu_agenda;

                displayedStudentList.add(new StudentItem(StudentItem.TYPE_HEADER, sectionTitle, iconResId));
                displayedStudentList.addAll(sectionStudents);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
