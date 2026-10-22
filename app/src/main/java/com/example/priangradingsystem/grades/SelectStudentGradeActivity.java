package com.example.priangradingsystem.grades;

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
import com.example.priangradingsystem.FullScreenUtils;
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

        // Naka-ayos na constructor na may kasamang click listener
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
            btnFilter.setOnClickListener(v -> {
                Intent intent = new Intent(SelectStudentGradeActivity.this, FilterActivity.class);
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
                    currentSearchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                    applyCombinedFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
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
                if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT
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
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(KEY_STUDENT_LIST, null);

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<StudentItem>>() {}.getType();
            List<StudentItem> savedList = gson.fromJson(json, type);

            if (savedList != null && !savedList.isEmpty()) {
                masterStudentList.clear();
                for (StudentItem item : savedList) {
                    if (item != null) {
                        masterStudentList.add(item);
                    }
                }
            }
        }

        if (masterStudentList.isEmpty()) {
            masterStudentList.add(new StudentItem(StudentItem.TYPE_HEADER, "Nursery", android.R.drawable.ic_menu_edit));

            StudentItem s1 = new StudentItem(StudentItem.TYPE_STUDENT, "Person 1", "", "Nursery", "Good");
            s1.setSection("Nursery");
            masterStudentList.add(s1);

            StudentItem s2 = new StudentItem(StudentItem.TYPE_STUDENT, "Person 2", "", "Nursery", "Excellent");
            s2.setSection("Nursery");
            masterStudentList.add(s2);

            StudentItem s3 = new StudentItem(StudentItem.TYPE_STUDENT, "Person 3", "", "Nursery", "Good");
            s3.setSection("Nursery");
            masterStudentList.add(s3);

            masterStudentList.add(new StudentItem(StudentItem.TYPE_HEADER, "Toddler", android.R.drawable.ic_menu_compass));

            StudentItem s4 = new StudentItem(StudentItem.TYPE_STUDENT, "Person 4", "", "Toddler", "Excellent");
            s4.setSection("Toddler");
            masterStudentList.add(s4);

            StudentItem s5 = new StudentItem(StudentItem.TYPE_STUDENT, "Person 5", "", "Toddler", "Good");
            s5.setSection("Toddler");
            masterStudentList.add(s5);

            StudentItem s6 = new StudentItem(StudentItem.TYPE_STUDENT, "Person 6", "", "Toddler", "Needs Improvement");
            s6.setSection("Toddler");
            masterStudentList.add(s6);
        }

        applyCombinedFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
    }
}