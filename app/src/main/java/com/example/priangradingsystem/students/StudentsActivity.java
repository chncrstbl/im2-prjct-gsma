package com.example.priangradingsystem.students;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.adapter.StudentAdapter;
import com.example.priangradingsystem.model.StudentItem;
import com.example.priangradingsystem.records.FilterActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("all")
public class StudentsActivity extends AppCompatActivity {

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
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String name = data.getStringExtra("STUDENT_NAME");
                    String gradeSection = data.getStringExtra("STUDENT_CLASS");
                    String age = data.getStringExtra("STUDENT_AGE");
                    String photoUri = data.getStringExtra("STUDENT_PHOTO_URI");

                    addNewStudentWithSection(name, gradeSection, age, photoUri);
                }
            });

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
        setContentView(R.layout.activity_student);

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnAdd = findViewById(R.id.btnAdd);
        ImageView btnFilter = findViewById(R.id.btnFilter);
        EditText etSearch = findViewById(R.id.etSearch);
        RecyclerView rvStudents = findViewById(R.id.rvStudents);

        adapter = new StudentAdapter(displayedStudentList);
        if (rvStudents != null) {
            rvStudents.setLayoutManager(new LinearLayoutManager(this));
            rvStudents.setAdapter(adapter);
        }

        loadSavedStudents();

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                Intent intent = new Intent(StudentsActivity.this, AddStudentActivity.class);
                addStudentLauncher.launch(intent);
            });
        }

        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> {
                Intent intent = new Intent(StudentsActivity.this, FilterActivity.class);
                intent.putExtra("CURRENT_LEVEL_FILTER", currentLevelFilter);
                intent.putExtra("CURRENT_PERFORMANCE_FILTER", currentPerformanceFilter);
                filterLauncher.launch(intent);
            });
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString().trim().toLowerCase();
                    applyCombinedFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void addNewStudentWithSection(String name, String gradeSection, String age, String photoUri) {
        String targetSection = (gradeSection == null || gradeSection.trim().isEmpty())
                ? "General - Unassigned"
                : gradeSection;

        if (!hasHeader(targetSection)) {
            int iconResId = android.R.drawable.ic_menu_agenda;
            String lower = targetSection.toLowerCase();
            if (lower.contains("nursery")) {
                iconResId = android.R.drawable.ic_menu_edit;
            } else if (lower.contains("pre-k")) {
                iconResId = android.R.drawable.ic_menu_compass;
            }
            masterStudentList.add(new StudentItem(StudentItem.TYPE_HEADER, targetSection, iconResId));
        }

        String levelOnly = targetSection.split("-")[0].trim();

        StudentItem newStudent = new StudentItem(StudentItem.TYPE_STUDENT, name, "Age " + age, levelOnly, "Good");
        newStudent.setSection(targetSection);
        if (photoUri != null) {
            newStudent.setPhotoUri(photoUri);
        }

        masterStudentList.add(newStudent);
        saveStudentsPermanently();
        applyCombinedFilters();
    }

    private boolean hasHeader(String sectionTitle) {
        for (StudentItem item : masterStudentList) {
            if (item.getItemViewType() == StudentItem.TYPE_HEADER && item.getHeaderTitle().equalsIgnoreCase(sectionTitle)) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applyCombinedFilters() {
        displayedStudentList.clear();

        List<String> sortedHeaders = new ArrayList<>();
        for (StudentItem item : masterStudentList) {
            switch (item.getItemViewType()) {
                case StudentItem.TYPE_HEADER:
                    if (!sortedHeaders.contains(item.getHeaderTitle())) {
                        sortedHeaders.add(item.getHeaderTitle());
                    }
                    break;
                default:
                    break;
            }
        }
        sortedHeaders.sort(String::compareToIgnoreCase);

        for (String headerTitle : sortedHeaders) {
            List<StudentItem> sectionStudents = new ArrayList<>();
            boolean sectionHeaderMatchesSearch = doesSectionHeaderMatchSearch(headerTitle);

            int headerIcon = android.R.drawable.ic_menu_agenda;
            for (StudentItem masterItem : masterStudentList) {
                if (masterItem.getItemViewType() == StudentItem.TYPE_HEADER && masterItem.getHeaderTitle().equalsIgnoreCase(headerTitle)) {
                    headerIcon = masterItem.getHeaderIconResId();
                    break;
                }
            }

            for (StudentItem item : masterStudentList) {
                if (item.getItemViewType() == StudentItem.TYPE_STUDENT
                        && item.getSection() != null
                        && item.getSection().equalsIgnoreCase(headerTitle)) {

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

            sectionStudents.sort(Comparator.comparing(StudentItem::getName, Comparator.nullsFirst(String::compareToIgnoreCase)));

            if (!sectionStudents.isEmpty()) {
                displayedStudentList.add(new StudentItem(StudentItem.TYPE_HEADER, headerTitle, headerIcon));
                displayedStudentList.addAll(sectionStudents);
            }
        }

        adapter.notifyDataSetChanged();
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

    private void saveStudentsPermanently() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(masterStudentList);
        editor.putString(KEY_STUDENT_LIST, json);
        editor.apply();
    }

    private void loadSavedStudents() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(KEY_STUDENT_LIST, null);

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<StudentItem>>() {}.getType();
            List<StudentItem> savedList = gson.fromJson(json, type);

            if (savedList != null) {
                masterStudentList.clear();
                masterStudentList.addAll(savedList);
            }
        }
        applyCombinedFilters();
    }
}
