package com.example.priangradingsystem.records;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.priangradingsystem.R;

public class FilterActivity extends AppCompatActivity {

    private CheckBox cbLevelAll, cbNursery, cbPreK;
    private CheckBox cbPerfAll, cbExcellent, cbGood, cbNeedsImprovement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        ImageView btnBack = findViewById(R.id.btnBack);
        View btnApply = findViewById(R.id.btnApply);
        View btnReset = findViewById(R.id.btnReset);

        cbLevelAll = findViewById(R.id.cbLevelAll);
        cbNursery = findViewById(R.id.cbNursery);
        cbPreK = findViewById(R.id.cbPreK);

        cbPerfAll = findViewById(R.id.cbPerfAll);
        cbExcellent = findViewById(R.id.cbExcellent);
        cbGood = findViewById(R.id.cbGood);
        cbNeedsImprovement = findViewById(R.id.cbNeedsImprovement);

        String activeLevel = getIntent().getStringExtra("CURRENT_LEVEL_FILTER");
        String activePerf = getIntent().getStringExtra("CURRENT_PERFORMANCE_FILTER");

        if (activeLevel != null) {
            clearLevelCheckBoxes();
            if (activeLevel.equalsIgnoreCase("Nursery") && cbNursery != null) {
                cbNursery.setChecked(true);
            } else if (activeLevel.equalsIgnoreCase("Pre-K") && cbPreK != null) {
                cbPreK.setChecked(true);
            } else if (cbLevelAll != null) {
                cbLevelAll.setChecked(true);
            }
        }

        if (activePerf != null) {
            clearPerfCheckBoxes();
            if (activePerf.equalsIgnoreCase("Excellent") && cbExcellent != null) {
                cbExcellent.setChecked(true);
            } else if (activePerf.equalsIgnoreCase("Good") && cbGood != null) {
                cbGood.setChecked(true);
            } else if (activePerf.equalsIgnoreCase("Needs Improvement") && cbNeedsImprovement != null) {
                cbNeedsImprovement.setChecked(true);
            } else if (cbPerfAll != null) {
                cbPerfAll.setChecked(true);
            }
        }

        if (cbLevelAll != null) {
            cbLevelAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (cbNursery != null) cbNursery.setChecked(false);
                    if (cbPreK != null) cbPreK.setChecked(false);
                }
            });
        }

        if (cbNursery != null) {
            cbNursery.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (cbLevelAll != null) cbLevelAll.setChecked(false);
                    if (cbPreK != null) cbPreK.setChecked(false);
                }
            });
        }

        if (cbPreK != null) {
            cbPreK.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (cbLevelAll != null) cbLevelAll.setChecked(false);
                    if (cbNursery != null) cbNursery.setChecked(false);
                }
            });
        }

        if (cbPerfAll != null) {
            cbPerfAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (cbExcellent != null) cbExcellent.setChecked(false);
                    if (cbGood != null) cbGood.setChecked(false);
                    if (cbNeedsImprovement != null) cbNeedsImprovement.setChecked(false);
                }
            });
        }

        if (cbExcellent != null) {
            cbExcellent.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (cbPerfAll != null) cbPerfAll.setChecked(false);
                    if (cbGood != null) cbGood.setChecked(false);
                    if (cbNeedsImprovement != null) cbNeedsImprovement.setChecked(false);
                }
            });
        }

        if (cbGood != null) {
            cbGood.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (cbPerfAll != null) cbPerfAll.setChecked(false);
                    if (cbExcellent != null) cbExcellent.setChecked(false);
                    if (cbNeedsImprovement != null) cbNeedsImprovement.setChecked(false);
                }
            });
        }

        if (cbNeedsImprovement != null) {
            cbNeedsImprovement.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (cbPerfAll != null) cbPerfAll.setChecked(false);
                    if (cbExcellent != null) cbExcellent.setChecked(false);
                    if (cbGood != null) cbGood.setChecked(false);
                }
            });
        }

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                clearLevelCheckBoxes();
                clearPerfCheckBoxes();
                if (cbLevelAll != null) cbLevelAll.setChecked(true);
                if (cbPerfAll != null) cbPerfAll.setChecked(true);
            });
        }

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                String selectedLevel = (cbNursery != null && cbNursery.isChecked()) ? "Nursery" :
                        (cbPreK != null && cbPreK.isChecked()) ? "Pre-K" : "All";

                String selectedPerf = (cbExcellent != null && cbExcellent.isChecked()) ? "Excellent" :
                        (cbGood != null && cbGood.isChecked()) ? "Good" :
                        (cbNeedsImprovement != null && cbNeedsImprovement.isChecked()) ? "Needs Improvement" : "All";

                Intent resultIntent = new Intent();
                resultIntent.putExtra("LEVEL_FILTER", selectedLevel);
                resultIntent.putExtra("PERFORMANCE_FILTER", selectedPerf);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        }
    }

    private void clearLevelCheckBoxes() {
        if (cbLevelAll != null) cbLevelAll.setChecked(false);
        if (cbNursery != null) cbNursery.setChecked(false);
        if (cbPreK != null) cbPreK.setChecked(false);
    }

    private void clearPerfCheckBoxes() {
        if (cbPerfAll != null) cbPerfAll.setChecked(false);
        if (cbExcellent != null) cbExcellent.setChecked(false);
        if (cbGood != null) cbGood.setChecked(false);
        if (cbNeedsImprovement != null) cbNeedsImprovement.setChecked(false);
    }
}
