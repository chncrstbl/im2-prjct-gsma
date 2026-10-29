package com.example.priangradingsystem.grades;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.priangradingsystem.records.FilterActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("all")
public class SelectStudentGradeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "StudentPrefs";
    private static final String KEY_STUDENT_LIST = "master_student_list";

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

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnFilter = findViewById(R.id.btnFilter);
        EditText etSearch = findViewById(R.id.etSearch);
        RecyclerView rvStudents = findViewById(R.id.rvStudents);

        adapter = new StudentAdapter(displayedStudentList, item -> {
            if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                Intent intent = new Intent(SelectStudentGradeActivity.this, GradeEntryActivity.class);
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
        displayedStudentList.clear();

        List<String> sortedHeaders = new ArrayList<>();
        for (StudentItem item : masterStudentList) {
            if (item != null && item.getItemViewType() == StudentItem.TYPE_HEADER) {
                if (item.getHeaderTitle() != null && !sortedHeaders.contains(item.getHeaderTitle())) {
                    sortedHeaders.add(item.getHeaderTitle());
                }
            } else if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                String cat = item.getSection() != null && !item.getSection().isEmpty() ? item.getSection() : (item.getLevel() != null ? item.getLevel() : "Nursery");
                if (!sortedHeaders.contains(cat)) {
                    sortedHeaders.add(cat);
                }
            }
        }
        sortedHeaders.sort(String::compareToIgnoreCase);

        for (String headerTitle : sortedHeaders) {
            List<StudentItem> sectionStudents = new ArrayList<>();
            boolean sectionHeaderMatchesSearch = doesSectionHeaderMatchSearch(headerTitle);

            int headerIcon = android.R.drawable.ic_menu_agenda;
            for (StudentItem masterItem : masterStudentList) {
                if (masterItem != null && masterItem.getItemViewType() == StudentItem.TYPE_HEADER
                        && masterItem.getHeaderTitle() != null
                        && masterItem.getHeaderTitle().equalsIgnoreCase(headerTitle)) {
                    headerIcon = masterItem.getHeaderIconResId();
                    break;
                }
            }

            for (StudentItem item : masterStudentList) {
                if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                    String itemCat = item.getSection() != null && !item.getSection().isEmpty() ? item.getSection() : (item.getLevel() != null ? item.getLevel() : "Nursery");

                    if (itemCat.equalsIgnoreCase(headerTitle)) {
                        boolean matchesSearch = currentSearchQuery.isEmpty()
                                || sectionHeaderMatchesSearch
                                || (item.getName() != null && item.getName().toLowerCase().contains(currentSearchQuery));

                        boolean matchesLevel = currentLevelFilter.equalsIgnoreCase("All")
                                || (item.getLevel() != null && item.getLevel().equalsIgnoreCase(currentLevelFilter))
                                || (item.getSection() != null && item.getSection().toLowerCase().contains(currentLevelFilter.toLowerCase()));

                        boolean matchesPerf = currentPerformanceFilter.equalsIgnoreCase("All")
                                || (item.getPerformance() != null && item.getPerformance().equalsIgnoreCase(currentPerformanceFilter));

                        if (matchesSearch && matchesLevel && matchesPerf) {
                            sectionStudents.add(item);
                        }
                    }
                }
            }

            sectionStudents.sort(Comparator.comparing(StudentItem::getName, Comparator.nullsFirst(String::compareToIgnoreCase)));

            if (!sectionStudents.isEmpty()) {
                displayedStudentList.add(new StudentItem(StudentItem.TYPE_HEADER, headerTitle, headerIcon));
                displayedStudentList.addAll(sectionStudents);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private boolean doesSectionHeaderMatchSearch(String headerTitle) {
        if (currentSearchQuery.isEmpty()) {
            return false;
        }

        String headerLower = headerTitle.toLowerCase();
        if (currentSearchQuery.length() == 1) {
            return headerLower.endsWith(" " + currentSearchQuery)
                    || headerLower.endsWith("-" + currentSearchQuery)
                    || headerLower.startsWith(currentSearchQuery + " ");
        }

        return headerLower.contains(currentSearchQuery);
    }

    private void loadSavedStudents() {
        masterStudentList.clear();

        // 1. Query real students from Database
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            Cursor cursor = dbHelper.getAllStudents();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String fName = cursor.getString(cursor.getColumnIndexOrThrow("first_name"));
                    String lName = cursor.getString(cursor.getColumnIndexOrThrow("last_name"));
                    String age = cursor.getString(cursor.getColumnIndexOrThrow("age"));
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

                    String displayName = lName + ", " + fName;
                    String ageStr = (age != null && !age.trim().isEmpty()) ? (age.toLowerCase().startsWith("age") ? age : "Age " + age) : "";

                    StudentItem student = new StudentItem(StudentItem.TYPE_STUDENT, displayName, ageStr, level, "Good");
                    student.setSection(level);
                    student.setPhotoUri(photoUri);
                    masterStudentList.add(student);
                }
                cursor.close();
            }
        } catch (Exception ignored) {}

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
                            // Exclude mock "Person 1" to "Person 6"
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

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
        loadSavedStudents();
    }
}
