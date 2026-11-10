package com.example.priangradingsystem.grades;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.adapter.StudentAdapter;
import com.example.priangradingsystem.data.DatabaseHelper;
import com.example.priangradingsystem.model.StudentItem;
import com.example.priangradingsystem.ui.SchoolYearBar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("all")
public class SelectStudentGradeActivity extends AppCompatActivity {

    private static final String TAG = "SelectStudentGradeActivity";
    private static final String PREFS_NAME = "StudentPrefs";
    private static final String KEY_STUDENT_LIST = "master_student_list";

    private DatabaseHelper dbHelper;
    private StudentAdapter adapter;
    private final List<StudentItem> masterStudentList = new ArrayList<>();
    private final List<StudentItem> displayedStudentList = new ArrayList<>();

    private String currentSearchQuery = "";
    private String currentLevelFilter = "All";
    private String currentPerformanceFilter = "All";

    private final ActivityResultLauncher<Intent> filterLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    currentLevelFilter = data.getStringExtra("LEVEL_FILTER") != null ? data.getStringExtra("LEVEL_FILTER") : "All";
                    currentPerformanceFilter = data.getStringExtra("PERFORMANCE_FILTER") != null ? data.getStringExtra("PERFORMANCE_FILTER") : "All";

                    applyCombinedFilters();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_select_student_grade);
        dbHelper = new DatabaseHelper(this);
        SchoolYearBar.attach(this, dbHelper, true, false, this::loadSavedStudents);

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnFilter = findViewById(R.id.btnFilter);
        EditText etSearch = findViewById(R.id.etSearch);
        RecyclerView rvStudents = findViewById(R.id.rvStudents);

        adapter = new StudentAdapter(displayedStudentList, item -> {
            if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                Intent intent = new Intent(SelectStudentGradeActivity.this, GradeEntryActivity.class);
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

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

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
                            boolean matchesSearch = currentSearchQuery.isEmpty()
                                    || (item.getName() != null && item.getName().toLowerCase().contains(currentSearchQuery))
                                    || levelMatchesSearch;

                            boolean matchesLevel = currentLevelFilter.equalsIgnoreCase("All")
                                    || itemLvl.equalsIgnoreCase(currentLevelFilter)
                                    || (currentLevelFilter.equalsIgnoreCase("Toddler") && itemLvl.equalsIgnoreCase("Pre-K"))
                                    || (currentLevelFilter.equalsIgnoreCase("Pre-K") && itemLvl.equalsIgnoreCase("Toddler"));

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
                    int iconResId = (levelTitle.equalsIgnoreCase("Toddler") || levelTitle.equalsIgnoreCase("Pre-K"))
                            ? android.R.drawable.ic_menu_compass
                            : android.R.drawable.ic_menu_agenda;

                    displayedStudentList.add(new StudentItem(StudentItem.TYPE_HEADER, levelTitle, iconResId));

                    if (!femaleStudents.isEmpty()) {
                        displayedStudentList.add(new StudentItem(StudentItem.TYPE_GENDER_HEADER, "Female", 0));
                        displayedStudentList.addAll(femaleStudents);
                    }

                    if (!maleStudents.isEmpty()) {
                        displayedStudentList.add(new StudentItem(StudentItem.TYPE_GENDER_HEADER, "Male", 0));
                        displayedStudentList.addAll(maleStudents);
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

    private void loadSavedStudents() {
        masterStudentList.clear();

        // 1. Query real students from Database
        try {
            Cursor cursor = dbHelper.getAllStudents();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("student_id"));
                    String fName = cursor.getString(cursor.getColumnIndexOrThrow("first_name"));
                    String mName = cursor.getString(cursor.getColumnIndexOrThrow("middle_name"));
                    String lName = cursor.getString(cursor.getColumnIndexOrThrow("last_name"));
                    String age = cursor.getString(cursor.getColumnIndexOrThrow("age"));
                    String gender = cursor.getString(cursor.getColumnIndexOrThrow("sex"));
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

                    String displayName = fName + (mName != null && !mName.trim().isEmpty() ? " " + mName : "") + " " + lName;
                    displayName = displayName.trim();
                    if (displayName.isEmpty()) {
                        displayName = lName + ", " + fName;
                    }
                    String ageStr = (age != null && !age.trim().isEmpty()) ? (age.toLowerCase().startsWith("age") ? age : "Age " + age) : "";

                    StudentItem student = new StudentItem(StudentItem.TYPE_STUDENT, displayName, ageStr, level, "Good");
                    student.setId(id);
                    student.setSection(level);
                    student.setGender(gender);
                    student.setPhotoUri(photoUri);
                    masterStudentList.add(student);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading database students", e);
        }

        applyCombinedFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
        loadSavedStudents();
    }
}
