package com.example.priangradingsystem.records;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import com.example.priangradingsystem.FullScreenUtils;
import com.example.priangradingsystem.R;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RecordsActivity extends AppCompatActivity {

    private Button btn1stQtr, btn2ndQtr, btn3rdQtr;
    private int currentQuarter = 1;
    private final Map<String, Integer> recordRatings = new HashMap<>();
    private final Map<String, String> examScores = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FullScreenUtils.enableFullScreen(this);
        setContentView(R.layout.activity_records);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Student info from intent if available
        String studentName = getIntent().getStringExtra("STUDENT_NAME");
        String studentLevel = getIntent().getStringExtra("STUDENT_LEVEL");
        String studentPhotoUri = getIntent().getStringExtra("STUDENT_PHOTO_URI");

        TextView tvStudentName = findViewById(R.id.tvStudentName);
        TextView tvStudentGradeLevel = findViewById(R.id.tvStudentGradeLevel);
        ImageView imgStudentAvatar = findViewById(R.id.imgStudentAvatar);

        if (studentName != null && !studentName.trim().isEmpty() && tvStudentName != null) {
            tvStudentName.setText(studentName);
        }
        if (studentLevel != null && !studentLevel.trim().isEmpty() && tvStudentGradeLevel != null) {
            tvStudentGradeLevel.setText(studentLevel);
        }
        if (studentPhotoUri != null && !studentPhotoUri.trim().isEmpty() && imgStudentAvatar != null) {
            try {
                Uri uri = Uri.parse(studentPhotoUri);
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                imgStudentAvatar.setImageURI(uri);
            } catch (Exception e) {
                imgStudentAvatar.setImageResource(R.drawable.children);
            }
        }

        // Quarter Selection Buttons
        btn1stQtr = findViewById(R.id.btn1stQtr);
        btn2ndQtr = findViewById(R.id.btn2ndQtr);
        btn3rdQtr = findViewById(R.id.btn3rdQtr);

        if (btn1stQtr != null) {
            btn1stQtr.setOnClickListener(v -> selectQuarter(1, true));
        }
        if (btn2ndQtr != null) {
            btn2ndQtr.setOnClickListener(v -> selectQuarter(2, true));
        }
        if (btn3rdQtr != null) {
            btn3rdQtr.setOnClickListener(v -> selectQuarter(3, true));
        }

        selectQuarter(1, false);

        // Activity Dropdown Menu
        View layoutActivityDropdown = findViewById(R.id.layoutActivityDropdown);
        TextView tvSelectedActivity = findViewById(R.id.tvSelectedActivity);
        if (layoutActivityDropdown != null && tvSelectedActivity != null) {
            layoutActivityDropdown.setOnClickListener(v -> {
                String[] activities = new String[]{"Books", "Homeworks", "Attendance", "Exam", "Program Performance"};
                showCustomDropdown(v, tvSelectedActivity, activities, selectedActivity -> {
                    tvSelectedActivity.setText(selectedActivity);
                    if (selectedActivity.equalsIgnoreCase("Exam")) {
                        TextView tvSelectedSubject = findViewById(R.id.tvSelectedSubject);
                        if (tvSelectedSubject != null) {
                            tvSelectedSubject.setText("Select Subject");
                        }
                    }
                    updateUiForActivity();
                });
            });
        }

        // Month Dropdown Menu for Attendance
        View layoutMonthDropdown = findViewById(R.id.layoutMonthDropdown);
        TextView tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        if (layoutMonthDropdown != null && tvSelectedMonth != null) {
            layoutMonthDropdown.setOnClickListener(v -> {
                String[] months = new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
                showCustomDropdown(v, tvSelectedMonth, months, selectedMonth -> {
                    tvSelectedMonth.setText(selectedMonth);
                    populateAttendanceGridForMonth(selectedMonth);
                });
            });
        }

        // Subject Dropdown Menu
        View layoutSubjectDropdown = findViewById(R.id.layoutSubjectDropdown);
        TextView tvSelectedSubject = findViewById(R.id.tvSelectedSubject);
        if (layoutSubjectDropdown != null && tvSelectedSubject != null) {
            layoutSubjectDropdown.setOnClickListener(v -> {
                String[] subjects = new String[]{"Writing", "Math", "Reading", "Life Skills", "PE & Arts"};
                showCustomDropdown(v, tvSelectedSubject, subjects, selectedSubject -> {
                    tvSelectedSubject.setText(selectedSubject);
                    updateUiForActivity();
                });
            });
        }

        updateUiForActivity();

        // Edit button opens Rating Dialog or Exam Score Dialog
        ImageView btnEditRecord = findViewById(R.id.btnEditRecord);
        if (btnEditRecord != null) {
            btnEditRecord.setOnClickListener(v -> {
                boolean isExam = tvSelectedActivity != null && tvSelectedActivity.getText().toString().equalsIgnoreCase("Exam");

                if (isExam) {
                    EditText input = new EditText(this);
                    input.setHint("e.g. 20/20");
                    TextView tvExamScore = findViewById(R.id.tvExamScore);
                    if (tvExamScore != null) {
                        input.setText(tvExamScore.getText().toString());
                    }
                    new AlertDialog.Builder(this)
                            .setTitle("Edit Exam Score")
                            .setView(input)
                            .setPositiveButton("Save", (dialog, which) -> {
                                String newScore = input.getText().toString().trim();
                                if (!newScore.isEmpty()) {
                                    String key = getExamScoreKey();
                                    examScores.put(key, newScore);
                                    if (tvExamScore != null) {
                                        tvExamScore.setText(newScore);
                                    }
                                    Toast.makeText(this, "Exam score updated to " + newScore, Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    String[] ratings = new String[]{"No Star", "1 Star", "2 Stars", "3 Stars"};
                    new AlertDialog.Builder(this)
                            .setTitle("Rate Grade Record")
                            .setItems(ratings, (dialog, which) -> {
                                int selectedRating = which;
                                ImageView ivStar1 = findViewById(R.id.ivStar1);
                                ImageView ivStar2 = findViewById(R.id.ivStar2);
                                ImageView ivStar3 = findViewById(R.id.ivStar3);
                                if (ivStar1 != null && ivStar2 != null && ivStar3 != null) {
                                    setRating(selectedRating, ivStar1, ivStar2, ivStar3, true);
                                }
                            })
                            .show();
                }
            });
        }

        ImageView btnDeleteRecord = findViewById(R.id.btnDeleteRecord);
        if (btnDeleteRecord != null) {
            btnDeleteRecord.setOnClickListener(v ->
                Toast.makeText(this, "Delete Grade Record", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void updateUiForActivity() {
        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        TextView tvSelectedActivity = findViewById(R.id.tvSelectedActivity);
        TextView tvSelectedSubject = findViewById(R.id.tvSelectedSubject);
        TextView tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        TextView tvRecordSubtitle = findViewById(R.id.tvRecordSubtitle);
        View tvSubjectLabel = findViewById(R.id.tvSubjectLabel);
        View layoutSubjectDropdown = findViewById(R.id.layoutSubjectDropdown);
        View layoutMonthDropdown = findViewById(R.id.layoutMonthDropdown);
        View layoutStarsContainer = findViewById(R.id.layoutStarsContainer);
        View layoutExamScoreContainer = findViewById(R.id.layoutExamScoreContainer);
        View layoutAttendanceContainer = findViewById(R.id.layoutAttendanceContainer);
        View cardGradeRecord = findViewById(R.id.cardGradeRecord);

        String activity = (tvSelectedActivity != null) ? tvSelectedActivity.getText().toString() : "Books";
        boolean isExam = activity.equalsIgnoreCase("Exam");
        boolean isAttendance = activity.equalsIgnoreCase("Attendance");

        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText(isExam ? "Grade Records" : "Grade Record");
        }

        if (isAttendance) {
            if (tvSubjectLabel != null) tvSubjectLabel.setVisibility(View.GONE);
            if (layoutSubjectDropdown != null) layoutSubjectDropdown.setVisibility(View.GONE);
            if (layoutMonthDropdown != null) layoutMonthDropdown.setVisibility(View.VISIBLE);
            if (cardGradeRecord != null) cardGradeRecord.setVisibility(View.GONE);
            if (layoutAttendanceContainer != null) layoutAttendanceContainer.setVisibility(View.VISIBLE);
            String month = (tvSelectedMonth != null) ? tvSelectedMonth.getText().toString() : "January";
            populateAttendanceGridForMonth(month);
        } else {
            if (tvSubjectLabel != null) tvSubjectLabel.setVisibility(View.VISIBLE);
            if (layoutSubjectDropdown != null) layoutSubjectDropdown.setVisibility(View.VISIBLE);
            if (layoutMonthDropdown != null) layoutMonthDropdown.setVisibility(View.GONE);
            if (cardGradeRecord != null) cardGradeRecord.setVisibility(View.VISIBLE);
            if (layoutAttendanceContainer != null) layoutAttendanceContainer.setVisibility(View.GONE);

            if (isExam) {
                if (tvSelectedSubject != null && tvSelectedSubject.getText().toString().equals("Reading")) {
                    tvSelectedSubject.setText("Select Subject");
                }
                if (tvRecordSubtitle != null) {
                    String qName = (currentQuarter == 1) ? "1st Quarter" : (currentQuarter == 2) ? "2nd Quarter" : "3rd Quarter";
                    tvRecordSubtitle.setText(qName);
                    tvRecordSubtitle.setVisibility(View.VISIBLE);
                }
                if (layoutStarsContainer != null) layoutStarsContainer.setVisibility(View.GONE);
                if (layoutExamScoreContainer != null) layoutExamScoreContainer.setVisibility(View.VISIBLE);
                loadExamScoreForCurrentSelection();
            } else {
                if (tvRecordSubtitle != null) tvRecordSubtitle.setVisibility(View.GONE);
                if (layoutStarsContainer != null) layoutStarsContainer.setVisibility(View.VISIBLE);
                if (layoutExamScoreContainer != null) layoutExamScoreContainer.setVisibility(View.GONE);
                loadRatingForCurrentSelection();
            }
        }
    }

    private void populateAttendanceGridForMonth(String month) {
        GridLayout grid = findViewById(R.id.gridAttendanceDays);
        if (grid == null) return;
        grid.removeAllViews();

        int daysCount = 31;
        if (month.equalsIgnoreCase("February")) {
            daysCount = 28;
        } else if (month.equalsIgnoreCase("April") || month.equalsIgnoreCase("June") || month.equalsIgnoreCase("September") || month.equalsIgnoreCase("November")) {
            daysCount = 30;
        }

        Random random = new Random(month.hashCode() + currentQuarter);

        for (int i = 0; i < daysCount; i++) {
            View dayBox = new View(this);
            int heightPx = (int) (28 * getResources().getDisplayMetrics().density);
            int marginPx = (int) (4 * getResources().getDisplayMetrics().density);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = heightPx;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
            lp.setMargins(marginPx, marginPx, marginPx, marginPx);
            dayBox.setLayoutParams(lp);

            boolean isPresent = random.nextDouble() > 0.25;
            boolean isDarkGreen = random.nextDouble() > 0.5;
            if (isPresent) {
                dayBox.setBackgroundColor(isDarkGreen ? Color.parseColor("#006622") : Color.parseColor("#00A859"));
            } else {
                dayBox.setBackgroundColor(Color.parseColor("#E0E0E0"));
            }
            grid.addView(dayBox);
        }
    }

    private String getRatingKey() {
        TextView tvSubject = findViewById(R.id.tvSelectedSubject);
        TextView tvActivity = findViewById(R.id.tvSelectedActivity);
        String subject = (tvSubject != null) ? tvSubject.getText().toString() : "Reading";
        String activity = (tvActivity != null) ? tvActivity.getText().toString() : "Books";
        return currentQuarter + "_" + subject + "_" + activity;
    }

    private String getExamScoreKey() {
        TextView tvSubject = findViewById(R.id.tvSelectedSubject);
        String subject = (tvSubject != null) ? tvSubject.getText().toString() : "Select Subject";
        return currentQuarter + "_" + subject + "_Exam";
    }

    private void loadRatingForCurrentSelection() {
        ImageView ivStar1 = findViewById(R.id.ivStar1);
        ImageView ivStar2 = findViewById(R.id.ivStar2);
        ImageView ivStar3 = findViewById(R.id.ivStar3);
        if (ivStar1 != null && ivStar2 != null && ivStar3 != null) {
            String key = getRatingKey();
            Integer val = recordRatings.get(key);
            int rating = val != null ? val : 0;
            setRatingInternal(rating, ivStar1, ivStar2, ivStar3);
        }
    }

    private void loadExamScoreForCurrentSelection() {
        TextView tvExamScore = findViewById(R.id.tvExamScore);
        if (tvExamScore != null) {
            String key = getExamScoreKey();
            String score = examScores.getOrDefault(key, "20/20");
            tvExamScore.setText(score);
        }
    }

    private void setRating(int rating, ImageView s1, ImageView s2, ImageView s3, boolean showToast) {
        String key = getRatingKey();
        recordRatings.put(key, rating);
        setRatingInternal(rating, s1, s2, s3);
        if (showToast) {
            String msg = (rating == 0) ? "No stars" : (rating + " star(s)");
            Toast.makeText(this, "Rating updated for current Quarter/Subject to " + msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void setRatingInternal(int rating, ImageView s1, ImageView s2, ImageView s3) {
        s1.setImageResource(rating >= 1 ? R.drawable.star_filled : R.drawable.star_normal);
        s2.setImageResource(rating >= 2 ? R.drawable.star_filled : R.drawable.star_normal);
        s3.setImageResource(rating >= 3 ? R.drawable.star_filled : R.drawable.star_normal);
    }

    interface OnItemSelectedListener {
        void onItemSelected(String item);
    }

    private void showCustomDropdown(View anchor, TextView targetTextView, String[] items, OnItemSelectedListener listener) {
        String currentSelected = targetTextView.getText().toString();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                items
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.BLACK);
                view.setTextSize(16);
                view.setPadding(32, 24, 32, 24);

                String itemText = getItem(position);
                if (itemText != null && itemText.equals(currentSelected)) {
                    view.setBackgroundResource(R.drawable.dropdown_selected);
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT);
                }
                return view;
            }
        };

        ListPopupWindow popupWindow = new ListPopupWindow(this);
        popupWindow.setAdapter(adapter);
        popupWindow.setAnchorView(anchor);
        popupWindow.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.dropdown_box, null));
        popupWindow.setWidth(anchor.getWidth());
        popupWindow.setHeight(ListPopupWindow.WRAP_CONTENT);
        popupWindow.setModal(true);

        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(currentSelected)) {
                popupWindow.setSelection(i);
                break;
            }
        }

        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            String selected = items[position];
            listener.onItemSelected(selected);
            Toast.makeText(this, selected, Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });
        popupWindow.show();
    }

    private void selectQuarter(int quarter, boolean showToast) {
        if (btn1stQtr == null || btn2ndQtr == null || btn3rdQtr == null) return;

        currentQuarter = quarter;

        btn1stQtr.setBackgroundResource(quarter == 1 ? R.drawable.highlighted_quarter : R.drawable.regular_quarter);
        btn1stQtr.setTextColor(quarter == 1 ? Color.BLACK : Color.DKGRAY);

        btn2ndQtr.setBackgroundResource(quarter == 2 ? R.drawable.highlighted_quarter : R.drawable.regular_quarter);
        btn2ndQtr.setTextColor(quarter == 2 ? Color.BLACK : Color.DKGRAY);

        btn3rdQtr.setBackgroundResource(quarter == 3 ? R.drawable.highlighted_quarter : R.drawable.regular_quarter);
        btn3rdQtr.setTextColor(quarter == 3 ? Color.BLACK : Color.DKGRAY);

        updateUiForActivity();

        if (showToast) {
            String qName = (quarter == 1) ? "1st" : (quarter == 2) ? "2nd" : "3rd";
            Toast.makeText(this, qName + " Quarter Selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenUtils.enableFullScreen(this);
    }
}
