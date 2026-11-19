package com.example.priangradingsystem;

import android.app.Application;

import com.example.priangradingsystem.ui.ThemeManager;

public class PrianApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Restores the saved light/dark choice before any screen is created.
        ThemeManager.init(this);
    }
}
