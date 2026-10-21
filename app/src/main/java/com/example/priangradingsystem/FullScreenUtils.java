package com.example.priangradingsystem;

import android.app.Activity;
import android.view.Window;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

@SuppressWarnings({"unused", "ConstantConditions"})
public class FullScreenUtils {

    public static void enableFullScreen(Activity activity) {
        if (activity == null) return;

        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.show(WindowInsetsCompat.Type.systemBars());
            controller.setAppearanceLightStatusBars(true);
        }
    }
}
