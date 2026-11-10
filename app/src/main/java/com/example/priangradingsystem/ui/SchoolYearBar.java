package com.example.priangradingsystem.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.priangradingsystem.data.DatabaseHelper;
import com.example.priangradingsystem.data.DatabaseHelper.CarryOver;
import com.example.priangradingsystem.data.SchoolYear;

import java.util.ArrayList;
import java.util.List;

/**
 * A slim "School year" strip placed above a screen's own layout (no layout XML change needed).
 * Tap the year to switch it; with {@code manage} on, the "Manage" button creates school years,
 * sets the current one and brings students over from another year.
 *
 * Switching only changes which year the screens show ({@link DatabaseHelper#selectSchoolYear});
 * grades are stored per year, so nothing is ever recalculated or mixed.
 */
public final class SchoolYearBar {

    public interface Listener { void onSchoolYearChanged(); }

    private final Activity activity;
    private final DatabaseHelper db;
    private final Listener listener;
    private final boolean selectable;
    private final boolean manage;
    private TextView tvYear;

    private SchoolYearBar(Activity activity, DatabaseHelper db, boolean selectable, boolean manage, Listener listener) {
        this.activity = activity;
        this.db = db;
        this.selectable = selectable;
        this.manage = manage;
        this.listener = listener;
    }

    /** Adds the bar above the activity's content. Call right after setContentView. Returns null on failure. */
    public static SchoolYearBar attach(Activity activity, DatabaseHelper db, boolean selectable, boolean manage,
                                       Listener listener) {
        try {
            ViewGroup content = activity.findViewById(android.R.id.content);
            if (content == null || content.getChildCount() == 0) return null;
            View root = content.getChildAt(0);
            if ("school_year_wrapper".equals(root.getTag())) return null; // already attached

            SchoolYearBar bar = new SchoolYearBar(activity, db, selectable, manage, listener);
            View strip = bar.build();

            content.removeView(root);
            LinearLayout wrapper = new LinearLayout(activity);
            wrapper.setTag("school_year_wrapper");
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.addView(strip, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            wrapper.addView(root, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            content.addView(wrapper, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            bar.refresh();
            return bar;
        } catch (Exception e) {
            android.util.Log.e("SchoolYearBar", "Could not attach the school year bar", e);
            return null;
        }
    }

    private int dp(int v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }

    private View build() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(Color.parseColor("#FFF3CD"));
        row.setPadding(dp(16), dp(6), dp(8), dp(6));

        TextView label = new TextView(activity);
        label.setText("School year");
        label.setTextColor(Color.parseColor("#5D4B00"));
        label.setTextSize(13);
        row.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        tvYear = new TextView(activity);
        tvYear.setTextColor(Color.parseColor("#3E2F00"));
        tvYear.setTypeface(Typeface.DEFAULT_BOLD);
        tvYear.setTextSize(15);
        tvYear.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams yearLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(tvYear, yearLp);
        if (selectable) tvYear.setOnClickListener(v -> showPicker());

        if (manage) {
            TextView btn = new TextView(activity);
            btn.setText("Manage");
            btn.setTextColor(Color.parseColor("#3E2F00"));
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setTextSize(13);
            btn.setPadding(dp(12), dp(8), dp(12), dp(8));
            btn.setOnClickListener(v -> showManageMenu());
            row.addView(btn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        return row;
    }

    /** Re-reads the selected year (call from onResume). */
    public void refresh() {
        try {
            SchoolYear y = db.getSelectedSchoolYear();
            if (tvYear != null && y != null) {
                tvYear.setText(y.label + (y.current ? "  (current)" : "") + (selectable ? "  ▾" : ""));
            }
        } catch (Exception e) {
            android.util.Log.e("SchoolYearBar", "refresh failed", e);
        }
    }

    private void changed() {
        refresh();
        if (listener != null) listener.onSchoolYearChanged();
    }

    private void showPicker() {
        final List<SchoolYear> years = db.getSchoolYears();
        if (years.isEmpty()) return;
        String[] names = new String[years.size()];
        int checked = 0;
        long selected = db.getSelectedSchoolYearId();
        for (int i = 0; i < names.length; i++) {
            names[i] = years.get(i).toString() + "  –  " + db.getStudentCount(years.get(i).id) + " students";
            if (years.get(i).id == selected) checked = i;
        }
        new AlertDialog.Builder(activity)
                .setTitle("Show school year")
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    d.dismiss();
                    if (years.get(which).id != selected && db.selectSchoolYear(years.get(which).id)) changed();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showManageMenu() {
        final SchoolYear sel = db.getSelectedSchoolYear();
        final List<String> items = new ArrayList<>();
        items.add("New school year…");
        if (!sel.current) items.add("Make " + sel.label + " the current school year");
        items.add("Add students from another school year…");
        new AlertDialog.Builder(activity)
                .setTitle("School years")
                .setItems(items.toArray(new String[0]), (d, which) -> {
                    String item = items.get(which);
                    if (item.startsWith("New")) askNewYear();
                    else if (item.startsWith("Make")) confirmMakeCurrent(sel);
                    else pickSourceYear();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmMakeCurrent(final SchoolYear y) {
        new AlertDialog.Builder(activity)
                .setTitle("Make " + y.label + " the current school year?")
                .setMessage("The app opens on the current school year. Nothing is deleted or changed in any other year.")
                .setPositiveButton("Make current", (d, w) -> {
                    if (db.setCurrentSchoolYear(y.id)) changed();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askNewYear() {
        List<SchoolYear> years = db.getSchoolYears();
        String suggestion = years.isEmpty() ? "" : SchoolYear.next(years.get(0).label);
        final EditText input = new EditText(activity);
        input.setHint("2027-2028");
        input.setText(suggestion);
        input.setSelection(input.getText().length());
        input.setSingleLine(true);
        LinearLayout box = new LinearLayout(activity);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(activity)
                .setTitle("New school year")
                .setMessage("Two consecutive years, for example 2027-2028.")
                .setView(box)
                .setPositiveButton("Create", (d, w) -> {
                    String raw = input.getText().toString();
                    long id = db.createSchoolYear(raw, false);
                    if (id < 0) {
                        Toast.makeText(activity, db.getLastError(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    afterYearCreated(id);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void afterYearCreated(final long newId) {
        final SchoolYear created = db.getSchoolYear(newId);
        final SchoolYear previous = db.getSelectedSchoolYear();
        db.selectSchoolYear(newId);
        changed();
        final SchoolYear cur = db.getCurrentSchoolYear();
        if (created != null && cur != null && created.label.compareTo(cur.label) > 0) {
            new AlertDialog.Builder(activity)
                    .setTitle("Make " + created.label + " the current school year?")
                    .setMessage("The app opens on the current school year.")
                    .setPositiveButton("Make current", (d, w) -> { db.setCurrentSchoolYear(newId); changed(); })
                    .setNegativeButton("Not yet", null)
                    .setOnDismissListener(d -> offerCarryOver(previous != null ? previous.id : -1, newId))
                    .show();
        } else {
            offerCarryOver(previous != null ? previous.id : -1, newId);
        }
    }

    private void offerCarryOver(long fromId, long toId) {
        if (fromId < 0 || fromId == toId) return;
        SchoolYear from = db.getSchoolYear(fromId);
        if (from == null || db.getStudentCount(fromId) == 0) return;
        askCarryMode(from, toId, true);
    }

    private void pickSourceYear() {
        final long target = db.getSelectedSchoolYearId();
        final List<SchoolYear> sources = new ArrayList<>();
        for (SchoolYear y : db.getSchoolYears()) if (y.id != target) sources.add(y);
        if (sources.isEmpty()) {
            Toast.makeText(activity, "There is no other school year yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[sources.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = sources.get(i).label + "  –  " + db.getStudentCount(sources.get(i).id) + " students";
        }
        new AlertDialog.Builder(activity)
                .setTitle("Add students from…")
                .setItems(names, (d, which) -> askCarryMode(sources.get(which), target, false))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askCarryMode(final SchoolYear from, final long toId, boolean optional) {
        final SchoolYear to = db.getSchoolYear(toId);
        if (to == null) return;
        final String[] options = {
                "Promote one level (Toddler → Nursery; Nursery students are not added)",
                "Keep the same level",
        };
        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setTitle("Add students from " + from.label + " to " + to.label + "?")
                .setItems(options, (d, which) -> {
                    int n = db.carryOverStudents(from.id, toId, which == 0 ? CarryOver.PROMOTE : CarryOver.SAME_LEVEL);
                    Toast.makeText(activity, n + (n == 1 ? " student" : " students") + " added to " + to.label
                            + ". Grades start fresh.", Toast.LENGTH_LONG).show();
                    changed();
                });
        b.setNegativeButton(optional ? "Start empty" : "Cancel", null);
        b.show();
    }
}
