package com.example.priangradingsystem.ui;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.example.priangradingsystem.R;

/**
 * Idle motion for the decorative sun and clouds (login / sign-up). Animators are tied to the view's
 * attach state, so nothing keeps running (or leaks) once the screen is gone.
 */
public final class Ambient {

    private Ambient() {}

    public static void start(Activity a) {
        float dp = a.getResources().getDisplayMetrics().density;
        drift(a.findViewById(R.id.cloudTopLeft), View.TRANSLATION_X, 10 * dp, 3600);
        drift(a.findViewById(R.id.cloudTopRight), View.TRANSLATION_X, -12 * dp, 4200);
        drift(a.findViewById(R.id.sun), View.TRANSLATION_Y, 5 * dp, 2600);
    }

    private static void drift(View v, android.util.Property<View, Float> prop, float amount, long duration) {
        if (v == null) return;
        ObjectAnimator o = ObjectAnimator.ofFloat(v, prop, -amount, amount);
        o.setDuration(duration);
        o.setRepeatCount(ObjectAnimator.INFINITE);
        o.setRepeatMode(ObjectAnimator.REVERSE);
        o.setInterpolator(new AccelerateDecelerateInterpolator());
        v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) { if (!o.isStarted()) o.start(); }
            @Override public void onViewDetachedFromWindow(View view) { o.cancel(); }
        });
        if (v.isAttachedToWindow()) o.start();
    }
}
