package com.example.priangradingsystem.reports;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnFilter = findViewById(R.id.btnFilter);
        EditText etSearch = findViewById(R.id.etSearch);
        RecyclerView rvStudents = findViewById(R.id.rvStudents);

        adapter = new StudentAdapter(displayedStudentList, item -> {
            if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                Intent intent = new Intent(SelectStudentsActivity.this, ReportsActivity.class);
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
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(KEY_STUDENT_LIST, null);

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<StudentItem>>() {}.getType();
            List<StudentItem> savedList = gson.fromJson(json, type);

            if (savedList != null && !savedList.isEmpty()) {
                for (StudentItem item : savedList) {
                    if (item != null && item.getItemViewType() == StudentItem.TYPE_STUDENT) {
                        masterStudentList.add(item);
                    }
                }
            }
        }

        if (masterStudentList.isEmpty()) {
            StudentItem s1 = new StudentItem(StudentItem.TYPE_STUDENT, "Arevalo, Vea", "4", "Nursery", "Excellent");
            s1.setSection("Nursery");
            masterStudentList.add(s1);

            StudentItem s2 = new StudentItem(StudentItem.TYPE_STUDENT, "Cueva, Ive Brayne", "5", "Nursery", "Good");
            s2.setSection("Nursery");
            masterStudentList.add(s2);

            StudentItem s3 = new StudentItem(StudentItem.TYPE_STUDENT, "Jumao-as, Lea", "4", "Nursery", "Needs Improvement");
            s3.setSection("Nursery");
            masterStudentList.add(s3);

            StudentItem s4 = new StudentItem(StudentItem.TYPE_STUDENT, "De guzman, Jamaila", "4", "Toddler", "Excellent");
            s4.setSection("Toddler");
            masterStudentList.add(s4);

            StudentItem s5 = new StudentItem(StudentItem.TYPE_STUDENT, "Ceron, Angel Adrian", "5", "Toddler", "Good");
            s5.setSection("Toddler");
            masterStudentList.add(s5);

            StudentItem s6 = new StudentItem(StudentItem.TYPE_STUDENT, "Delos santos, Brylle", "5", "Toddler", "Good");
            s6.setSection("Toddler");
            masterStudentList.add(s6);
        }

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
