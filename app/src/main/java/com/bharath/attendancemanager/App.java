package com.bharath.attendancemanager;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.bharath.attendancemanager.data.LocalStorage;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Apply saved appearance mode before any activity renders
        int nightMode = LocalStorage.getInstance(this).getNightMode();
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}