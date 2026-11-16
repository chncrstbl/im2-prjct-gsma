package com.example.priangradingsystem.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.example.priangradingsystem.R;
import com.example.priangradingsystem.data.DatabaseHelper;
import com.example.priangradingsystem.data.DatabaseHelper.CarryOver;
import com.example.priangradingsystem.data.SchoolYear;

import java.util.ArrayList;
import java.util.List;

/**
 * A slim "School year" strip placed below the top header titles.
 * Tap the year dropdown to switch it; with {@code manage} on, the "Manage" button creates school years,
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

    /** Adds the bar below the top titles header. Call right after setContentView. Returns null on failure. */
    public static SchoolYearBar attach(Activity activity, DatabaseHelper db, boolean selectable, boolean manage,
                                       Listener listener) {
        try {
            ViewGroup content = activity.findViewById(android.R.id.content);
            if (content == null || content.getChildCount() == 0) return null;
            View root = content.getChildAt(0);
            if (root.findViewWithTag("school_year_bar") != null || "school_year_wrapper".equals(root.getTag())) {
                return null; // already attached
            }

            SchoolYearBar bar = new SchoolYearBar(activity, db, selectable, manage, listener);
            View strip = bar.build();
            strip.setTag("school_year_bar");

            if (root instanceof ViewGroup) {
                ViewGroup rootGroup = (ViewGroup) root;
                int insertIndex = rootGroup.getChildCount() > 0 ? 1 : 0;
                rootGroup.addView(strip, insertIndex, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                content.removeView(root);
                LinearLayout wrapper = new LinearLayout(activity);
                wrapper.setTag("school_year_wrapper");
                wrapper.setOrientation(LinearLayout.VERTICAL);
                wrapper.addView(root, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
                wrapper.addView(strip, 1, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                content.addView(wrapper, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

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
        row.setBackgroundColor(Color.WHITE);
        row.setPadding(dp(16), dp(8), dp(16), dp(8));

        // School year icon (sy_icon.png)
        ImageView ivIcon = new ImageView(activity);
        ivIcon.setImageResource(R.drawable.sy_icon);
        ivIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        iconLp.setMargins(0, 0, dp(10), 0);
        row.addView(ivIcon, iconLp);

        // Middle container for label + dropdown
        LinearLayout yearContainer = new LinearLayout(activity);
        yearContainer.setOrientation(LinearLayout.VERTICAL);

        TextView label = new TextView(activity);
        label.setText("School Year");
        label.setTextColor(Color.parseColor("#333333"));
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextSize(12);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelLp.bottomMargin = dp(3);
        yearContainer.addView(label, labelLp);

        tvYear = new TextView(activity);
        tvYear.setTextColor(Color.parseColor("#000000"));
        tvYear.setTypeface(Typeface.DEFAULT_BOLD);
        tvYear.setTextSize(13);
        tvYear.setGravity(Gravity.CENTER_VERTICAL);
        if (selectable) {
            tvYear.setBackgroundResource(R.drawable.bg_input_field_spinner);
            tvYear.setPadding(dp(12), dp(6), dp(32), dp(6));
            tvYear.setOnClickListener(v -> showPicker());
        } else {
            tvYear.setBackgroundResource(R.drawable.bg_input_field);
            tvYear.setPadding(dp(12), dp(6), dp(12), dp(6));
        }

        LinearLayout.LayoutParams tvYearLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        yearContainer.addView(tvYear, tvYearLp);

        LinearLayout.LayoutParams middleLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(yearContainer, middleLp);

        // Manage button on the right
        if (manage) {
            TextView btn = new TextView(activity);
            btn.setText("Manage");
            btn.setTextColor(Color.parseColor("#000000"));
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setTextSize(13);
            btn.setGravity(Gravity.CENTER);
            btn.setBackgroundResource(R.drawable.bg_manage_btn);
            btn.setPadding(dp(18), dp(8), dp(18), dp(8));
            btn.setOnClickListener(v -> showManageMenu());

            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnLp.setMargins(dp(12), 0, 0, 0);
            row.addView(btn, btnLp);
        }

        return row;
    }

    /** Re-reads the selected year (call from onResume). */
    public void refresh() {
        try {
            SchoolYear y = db.getSelectedSchoolYear();
            if (tvYear != null && y != null) {
                tvYear.setText(y.label + (y.current ? " (current)" : ""));
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
        if (years.isEmpty() || tvYear == null) return;

        final ListPopupWindow popupWindow = new ListPopupWindow(activity);
        final String[] items = new String[years.size()];
        final long selectedId = db.getSelectedSchoolYearId();
        int selectedIndex = 0;

        for (int i = 0; i < years.size(); i++) {
            SchoolYear y = years.get(i);
            items[i] = y.label + (y.current ? " (current)" : "");
            if (y.id == selectedId) {
                selectedIndex = i;
            }
        }
        final int finalSelectedIndex = selectedIndex;

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                activity,
                android.R.layout.simple_list_item_1,
                items
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.BLACK);
                view.setTextSize(14);
                view.setPadding(dp(16), dp(12), dp(16), dp(12));

                if (position == finalSelectedIndex) {
                    try {
                        view.setBackgroundResource(R.drawable.dropdown_selected);
                    } catch (Exception ignored) {
                        view.setBackgroundColor(Color.parseColor("#E0E0E0"));
                    }
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT);
                }
                return view;
            }
        };

        popupWindow.setAdapter(adapter);
        popupWindow.setAnchorView(tvYear);
        try {
            popupWindow.setBackgroundDrawable(ResourcesCompat.getDrawable(
                    activity.getResources(), R.drawable.dropdown_box, null));
        } catch (Exception ignored) {}

        popupWindow.setWidth(Math.max(tvYear.getWidth(), dp(180)));
        popupWindow.setHeight(ListPopupWindow.WRAP_CONTENT);
        popupWindow.setModal(true);

        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            SchoolYear chosen = years.get(position);
            popupWindow.dismiss();
            if (chosen.id != selectedId && db.selectSchoolYear(chosen.id)) {
                changed();
            }
        });

        popupWindow.show();
    }

    private void showManageMenu() {
        final SchoolYear sel = db.getSelectedSchoolYear();
        final List<String> items = new ArrayList<>();
        items.add("New school year…");
        if (sel != null && !sel.current) items.add("Make " + sel.label + " the current school year");
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
        if (y == null) return;
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
