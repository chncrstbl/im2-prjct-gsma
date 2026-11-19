package com.example.priangradingsystem.ui;

import android.animation.AnimatorInflater;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.CompoundButton;

import androidx.recyclerview.widget.RecyclerView;

import com.example.priangradingsystem.R;

/** Gives every tappable button / card a subtle "squish" on press, app-wide. */
final class PressEffects {

    private static final int TAG = R.id.tag_press_effects_attached;

    private PressEffects() {}

    static void attachOnce(Activity activity) {
        final View decor = activity.getWindow().getDecorView();
        if (decor.getTag(TAG) != null) return;
        decor.setTag(TAG, Boolean.TRUE);
        decor.post(() -> apply(decor.findViewById(android.R.id.content), decor.getHeight()));
    }

    private static void apply(View v, int screenHeight) {
        if (v == null) return;
        if (isCandidate(v, screenHeight)) {
            v.setStateListAnimator(AnimatorInflater.loadStateListAnimator(v.getContext(), R.animator.press_scale));
        }
        if (v instanceof ViewGroup && !(v instanceof AdapterView) && !(v instanceof RecyclerView)) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) apply(g.getChildAt(i), screenHeight);
        }
    }

    private static boolean isCandidate(View v, int screenHeight) {
        if (!v.isClickable() || !v.isEnabled()) return false;
        if (v instanceof EditText || v instanceof AdapterView || v instanceof ScrollView
                || v instanceof RecyclerView || v instanceof CompoundButton) return false;
        if (v.getStateListAnimator() != null) return false;
        return screenHeight <= 0 || v.getHeight() < screenHeight / 2;
    }
}
