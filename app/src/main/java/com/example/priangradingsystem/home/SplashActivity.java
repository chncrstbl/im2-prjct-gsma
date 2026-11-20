package com.example.priangradingsystem.home;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.priangradingsystem.R;
import com.example.priangradingsystem.auth.LoginActivity;
import com.example.priangradingsystem.auth.SignUpActivity;
import com.example.priangradingsystem.data.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Animated intro: the hills rise, the sun pops up and starts spinning its rays, clouds float in,
 * the kids bounce up and the title lands. Returning users flow on to Login by themselves
 * (tap to skip); new users get a "Get Started" button.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long AUTO_ADVANCE_MS = 2600;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ObjectAnimator> loops = new ArrayList<>();
    private boolean hasAccount;
    private boolean navigated = false;
    private float dp;
    private boolean night;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        setContentView(R.layout.activity_splash);
        // Keep the next launch's system splash in sync with the saved theme (sun for light, moon for dark).
        com.example.priangradingsystem.ui.ThemeManager.syncSplashTheme(this, night);
        getWindow().setStatusBarColor(getColor(R.color.sky_top));
        getWindow().setNavigationBarColor(getColor(R.color.sky_bottom));
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(!night);
        dp = getResources().getDisplayMetrics().density;

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        hasAccount = prefs.getBoolean("HAS_ACCOUNT", false) || new DatabaseHelper(this).hasAnyTeacher();

        Button btnGetStarted = findViewById(R.id.btnGetStarted);
        if (hasAccount) {
            btnGetStarted.setVisibility(View.GONE);
            // Tap anywhere to skip the intro.
            findViewById(R.id.main).setOnClickListener(v -> proceed());
            handler.postDelayed(this::proceed, AUTO_ADVANCE_MS);
        } else {
            btnGetStarted.setOnClickListener(v -> proceed());
        }

        playIntro(btnGetStarted);
    }

    private void playIntro(View button) {
        View field = findViewById(R.id.fieldBackground);
        View kids = findViewById(R.id.children);
        View sun = findViewById(R.id.sun);
        View rays = findViewById(R.id.sunRays);
        View logo = findViewById(R.id.logo);
        View sub = findViewById(R.id.gradingSystem);
        View tag = findViewById(R.id.tagline);
        View[] clouds = {
                findViewById(R.id.cloudTopLeft), findViewById(R.id.cloudTopRight),
                findViewById(R.id.cloudMiddleLeft), findViewById(R.id.cloudMiddleRight)};
        float[] side = {-1, 1, -1, 1};

        // Start states
        field.setTranslationY(260 * dp);
        kids.setTranslationY(240 * dp);
        kids.setAlpha(0f);
        sun.setAlpha(0f);
        sun.setScaleX(0.3f);
        sun.setScaleY(0.3f);
        sun.setTranslationY(170 * dp);
        logo.setAlpha(0f);
        logo.setScaleX(0.6f);
        logo.setScaleY(0.6f);
        for (View v : new View[]{sub, tag}) {
            v.setAlpha(0f);
            v.setTranslationY(18 * dp);
        }
        button.setAlpha(0f);
        button.setScaleX(0.6f);
        button.setScaleY(0.6f);
        for (int i = 0; i < clouds.length; i++) {
            clouds[i].setAlpha(0f);
            clouds[i].setTranslationX(side[i] * 150 * dp);
        }

        // Hills + kids
        field.animate().translationY(0).setDuration(750).setInterpolator(new DecelerateInterpolator(2f)).start();
        kids.animate().translationY(0).alpha(1f).setStartDelay(350).setDuration(750)
                .setInterpolator(new OvershootInterpolator(1.15f)).start();

        if (night) {
            playMoonRise(sun, rays);
        } else {
            // Sun rises, then its rays keep turning
            sun.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0).setStartDelay(0).setDuration(950)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .withEndAction(() -> {
                        loop(rays, View.ROTATION, 0f, 360f, 22000, LinearInterpolator.class, false);
                        loop(sun, View.TRANSLATION_Y, -4 * dp, 4 * dp, 2400, AccelerateDecelerateInterpolator.class, true);
                    }).start();
        }

        // Clouds float in, then drift forever
        for (int i = 0; i < clouds.length; i++) {
            final View c = clouds[i];
            final int idx = i;
            c.animate().alpha(1f).translationX(0).setStartDelay(450 + i * 130L).setDuration(900)
                    .setInterpolator(new DecelerateInterpolator(1.6f))
                    .withEndAction(() -> loop(c, View.TRANSLATION_X, -8 * dp * (idx % 2 == 0 ? 1 : -1),
                            8 * dp * (idx % 2 == 0 ? 1 : -1), 3200 + idx * 450L,
                            AccelerateDecelerateInterpolator.class, true)).start();
        }

        // Title block
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(850).setDuration(600)
                .setInterpolator(new OvershootInterpolator(1.7f)).start();
        sub.animate().alpha(1f).translationY(0).setStartDelay(1100).setDuration(450)
                .setInterpolator(new DecelerateInterpolator()).start();
        tag.animate().alpha(1f).translationY(0).setStartDelay(1280).setDuration(450)
                .setInterpolator(new DecelerateInterpolator()).start();
        if (!hasAccount) {
            button.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(1500).setDuration(450)
                    .setInterpolator(new OvershootInterpolator(1.6f))
                    .withEndAction(() -> loop(button, View.SCALE_X, 1f, 1.05f, 900, AccelerateDecelerateInterpolator.class, true))
                    .start();
        }
    }

    /** Dark-mode intro: the moon glides up in an arc, its halo glows, stars twinkle on, a comet streaks by. */
    private void playMoonRise(View moon, View halo) {
        moon.setTranslationX(-110 * dp);
        moon.setRotation(-25f);
        moon.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0).translationX(0).rotation(0f)
                .setStartDelay(0).setDuration(1100).setInterpolator(new DecelerateInterpolator(1.6f))
                .withEndAction(() -> {
                    loop(halo, View.ROTATION, 0f, 360f, 42000, LinearInterpolator.class, false);
                    loop(moon, View.TRANSLATION_Y, -5 * dp, 5 * dp, 2800, AccelerateDecelerateInterpolator.class, true);
                    loop(halo, View.ALPHA, 1f, 0.55f, 2000, AccelerateDecelerateInterpolator.class, true);
                }).start();

        int[] ids = {R.id.star1, R.id.star2, R.id.star3, R.id.star4, R.id.star5, R.id.star6, R.id.star7};
        for (int i = 0; i < ids.length; i++) {
            final View star = findViewById(ids[i]);
            final int idx = i;
            star.setVisibility(View.VISIBLE);
            star.setAlpha(0f);
            star.setScaleX(0f);
            star.setScaleY(0f);
            star.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(500 + i * 170L).setDuration(450)
                    .setInterpolator(new OvershootInterpolator(2f))
                    .withEndAction(() -> loop(star, View.ALPHA, 1f, 0.25f, 900 + idx * 170L,
                            AccelerateDecelerateInterpolator.class, true)).start();
        }

        final View comet = findViewById(R.id.shootingStar);
        comet.setVisibility(View.VISIBLE);
        comet.setAlpha(0f);
        comet.setRotation(18f);
        comet.setTranslationX(40 * dp);
        comet.animate().alpha(1f).translationX(-260 * dp).translationY(110 * dp)
                .setStartDelay(1500).setDuration(900).setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> comet.setAlpha(0f)).start();
    }

    private void loop(View v, android.util.Property<View, Float> prop, float from, float to, long dur,
                      Class<? extends android.view.animation.Interpolator> interp, boolean reverse) {
        if (isDestroyed()) return;
        ObjectAnimator a = ObjectAnimator.ofFloat(v, prop, from, to);
        a.setDuration(dur);
        a.setRepeatCount(ObjectAnimator.INFINITE);
        a.setRepeatMode(reverse ? ObjectAnimator.REVERSE : ObjectAnimator.RESTART);
        try {
            a.setInterpolator(interp.getDeclaredConstructor().newInstance());
        } catch (Exception ignored) { }
        a.start();
        loops.add(a);
        if (prop == View.SCALE_X) {
            // keep the button's pulse uniform
            ObjectAnimator y = ObjectAnimator.ofFloat(v, View.SCALE_Y, from, to);
            y.setDuration(dur);
            y.setRepeatCount(ObjectAnimator.INFINITE);
            y.setRepeatMode(ObjectAnimator.REVERSE);
            y.setInterpolator(new AccelerateDecelerateInterpolator());
            y.start();
            loops.add(y);
        }
    }

    @SuppressWarnings("deprecation")
    private void proceed() {
        if (navigated || isFinishing()) return;
        navigated = true;
        Intent intent = new Intent(this, hasAccount ? LoginActivity.class : SignUpActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        for (ObjectAnimator a : loops) a.cancel();
        loops.clear();
        super.onDestroy();
    }
}
