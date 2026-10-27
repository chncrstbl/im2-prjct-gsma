package com.example.priangradingsystem.records;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
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
import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import com.example.priangradingsystem.adapter.StudentAdapter;
import com.example.priangradingsystem.data.DatabaseHelper;
import com.example.priangradingsystem.model.StudentItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("all")
public class StudentRecordsActivity extends AppCompatActivity {

    private static final String TAG = "StudentRecordsActivity";

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
        setContentView(R.layout.activity_student_records);

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnFilter = findViewById(R.id.btnFilter);
        EditText etSearch = findViewById(R.id.etSearch);
        RecyclerView rvStudents = findViewById(R.id.rvStudents);

        adapter = new StudentAdapter(displayedStudentList);
        adapter.setOnStudentClickListener(item -> {
            Intent intent = new Intent(StudentRecordsActivity.this, RecordsActivity.class);
            intent.putExtra("STUDENT_NAME", item.getName());
            String level = item.getLevel() != null ? item.getLevel() : item.getSection();
            intent.putExtra("STUDENT_LEVEL", level);
            if (item.getPhotoUri() != null) {
                intent.putExtra("STUDENT_PHOTO_URI", item.getPhotoUri());
            }
            if (item.getGender() != null) {
                intent.putExtra("STUDENT_GENDER", item.getGender());
            }
            startActivity(intent);
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
            btnFilter.setOnClickListener(v -> {
                Intent intent = new Intent(StudentRecordsActivity.this, FilterActivity.class);
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

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
        loadSavedStudents();
    }

    private void loadSavedStudents() {
        masterStudentList.clear();

        try {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            Cursor cursor = dbHelper.getAllStudents();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String fName = cursor.getString(cursor.getColumnIndexOrThrow("first_name"));
                    String mName = cursor.getString(cursor.getColumnIndexOrThrow("middle_name"));
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

                    String displayName = fName + (mName != null && !mName.trim().isEmpty() ? " " + mName : "") + " " + lName;
                    displayName = displayName.trim();
                    if (displayName.isEmpty()) {
                        displayName = lName + ", " + fName;
                    }
                    String ageStr = (age != null && !age.trim().isEmpty()) ? (age.toLowerCase().startsWith("age") ? age : "Age " + age) : "";

                    String sex = "";
                    try {
                        int sexIdx = cursor.getColumnIndex("sex");
                        if (sexIdx != -1) {
                            String dbSex = cursor.getString(sexIdx);
                            if (dbSex != null) sex = dbSex.trim();
                        }
                    } catch (Exception ignored) {}

                    StudentItem student = new StudentItem(StudentItem.TYPE_STUDENT, displayName, ageStr, level, "Good");
                    student.setSection(level);
                    student.setPhotoUri(photoUri);
                    student.setGender(sex);
                    masterStudentList.add(student);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading students from database", e);
        }

        applyCombinedFilters();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applyCombinedFilters() {
        displayedStudentList.clear();

        List<StudentItem> studentItems = new ArrayList<>();
        for (StudentItem item : masterStudentList) {
            if (item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                studentItems.add(item);
            }
        }

        for (StudentItem item : studentItems) {
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
