package com.cylonid.nativealpha.util;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import androidx.work.Configuration;
import androidx.work.WorkManager;
import com.cylonid.nativealpha.crash.GlobalCrashHandler;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class App extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public void onCreate() {
        super.onCreate();

        App.context = getApplicationContext();

        GlobalCrashHandler.Companion.install(this);

        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(this, new Configuration.Builder().build());
        }
    }

    public static Context getAppContext() {
        return App.context;
    }
}