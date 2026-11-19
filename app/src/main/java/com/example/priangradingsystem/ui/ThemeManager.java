package com.example.priangradingsystem.ui;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Single source of truth for the app theme.
 *
 * The choice is stored in SharedPreferences ("UserPrefs" / DARK_MODE_ENABLED) and re-applied in
 * {@link #init(Application)} every time the process starts, so the theme survives the app being
 * closed and reopened. Because every screen uses the DayNight theme with semantic colors,
 * switching the mode restyles the whole app.
 */
public final class ThemeManager {

    public static final String PREFS = "UserPrefs";
    public static final String KEY_DARK = "DARK_MODE_ENABLED";

    private static Bitmap pendingSnapshot;

    private ThemeManager() {}

    public static void init(Application app) {
        applySavedMode(app);
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityResumed(Activity a) {
                androidx.core.view.WindowCompat.getInsetsController(a.getWindow(), a.getWindow().getDecorView())
                        .setAppearanceLightNavigationBars(!isDark(a));
                PressEffects.attachOnce(a);
                playPendingCrossfade(a);
            }
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }

    public static boolean isDark(Context c) {
        return prefs(c).getBoolean(KEY_DARK, false);
    }

    public static void applySavedMode(Context c) {
        AppCompatDelegate.setDefaultNightMode(
                isDark(c) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /** Persists the choice, then switches the whole app. The visible screen cross-fades. */
    public static void setDark(Activity activity, boolean dark) {
        snapshot(activity);
        prefs(activity).edit().putBoolean(KEY_DARK, dark).apply();
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        // Safety net: if nothing was recreated, drop the snapshot.
        activity.getWindow().getDecorView().postDelayed(() -> pendingSnapshot = null, 1500);
    }

    private static void snapshot(Activity a) {
        try {
            View decor = a.getWindow().getDecorView();
            if (decor.getWidth() == 0 || decor.getHeight() == 0) return;
            Bitmap bmp = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(), Bitmap.Config.ARGB_8888);
            decor.draw(new Canvas(bmp));
            pendingSnapshot = bmp;
        } catch (Throwable t) {
            pendingSnapshot = null;
        }
    }

    private static void playPendingCrossfade(Activity a) {
        if (pendingSnapshot == null) return;
        final Bitmap bmp = pendingSnapshot;
        pendingSnapshot = null;
        final ViewGroup decor = (ViewGroup) a.getWindow().getDecorView();
        final ImageView overlay = new ImageView(a);
        overlay.setImageBitmap(bmp);
        overlay.setScaleType(ImageView.ScaleType.FIT_XY);
        decor.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.animate().alpha(0f).setDuration(420).withEndAction(() -> {
            decor.removeView(overlay);
            bmp.recycle();
        }).start();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
