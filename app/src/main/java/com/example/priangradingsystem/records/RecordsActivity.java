package com.example.priangradingsystem.records;

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
import com.example.priangradingsystem.reports.ReportsActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("all")
public class RecordsActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_records);

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnFilter = findViewById(R.id.btnFilter);
        EditText etSearchRecord = findViewById(R.id.etSearchRecord);
        RecyclerView recyclerViewRecords = findViewById(R.id.recyclerViewRecords);

        adapter = new StudentAdapter(displayedStudentList);
        adapter.setOnStudentClickListener(item -> {
            Intent intent = new Intent(RecordsActivity.this, ReportsActivity.class);
            intent.putExtra("STUDENT_NAME", item.getName());
            String level = item.getLevel() != null ? item.getLevel() : item.getSection();
            intent.putExtra("STUDENT_LEVEL", level);
            if (item.getPhotoUri() != null) {
                intent.putExtra("STUDENT_PHOTO_URI", item.getPhotoUri());
            }
            startActivity(intent);
        });

        if (recyclerViewRecords != null) {
            recyclerViewRecords.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewRecords.setAdapter(adapter);
        }

        loadSavedStudents();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> {
                Intent intent = new Intent(RecordsActivity.this, FilterActivity.class);
                intent.putExtra("CURRENT_LEVEL_FILTER", currentLevelFilter);
                intent.putExtra("CURRENT_PERFORMANCE_FILTER", currentPerformanceFilter);
                filterLauncher.launch(intent);
            });
        }

        if (etSearchRecord != null) {
            etSearchRecord.addTextChangedListener(new TextWatcher() {
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

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
        loadSavedStudents();
    }

    private void loadSavedStudents() {
        masterStudentList.clear();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_STUDENT_LIST, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<StudentItem>>() {}.getType();
            List<StudentItem> savedList = gson.fromJson(json, type);
            if (savedList != null) {
                masterStudentList.addAll(savedList);
            }
        }

        if (masterStudentList.isEmpty()) {
            masterStudentList.add(new StudentItem(StudentItem.TYPE_STUDENT, "Vea Arevalo", "4", "Nursery", "Excellent"));
            masterStudentList.add(new StudentItem(StudentItem.TYPE_STUDENT, "John Mark", "5", "Pre-K", "Good"));
            masterStudentList.add(new StudentItem(StudentItem.TYPE_STUDENT, "Sarah Jenkins", "4", "Nursery", "Needs Improvement"));
        }

        applyCombinedFilters();
    }

    private void applyCombinedFilters() {
        displayedStudentList.clear();

        for (StudentItem item : masterStudentList) {
            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    (item.getName() != null && item.getName().toLowerCase().contains(currentSearchQuery));

            String studentLevel = item.getLevel() != null ? item.getLevel() : item.getSection();
            boolean matchesLevel = currentLevelFilter.equalsIgnoreCase("All") ||
                    (studentLevel != null && studentLevel.equalsIgnoreCase(currentLevelFilter));

            String studentPerf = item.getPerformance() != null ? item.getPerformance() : "Excellent";
            boolean matchesPerf = currentPerformanceFilter.equalsIgnoreCase("All") ||
                    (studentPerf != null && studentPerf.equalsIgnoreCase(currentPerformanceFilter));

            if (matchesSearch && matchesLevel && matchesPerf) {
                displayedStudentList.add(item);
            }
        }

        displayedStudentList.sort(Comparator.comparing(StudentItem::getName, String.CASE_INSENSITIVE_ORDER));

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
