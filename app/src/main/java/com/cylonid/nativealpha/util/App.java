package com.cylonid.nativealpha.util;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.webkit.WebView;

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

        applyWebViewIsolationIfSandboxProcess();

        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(this, new Configuration.Builder().build());
        }
    }

    /**
     * WAOS Session Isolation: If this process is a web_sandbox process (e.g. :web_sandbox_0),
     * set a unique WebView data directory suffix so cookies, localStorage, IndexedDB, cache,
     * and service workers are completely separate from every other sandbox.
     *
     * WebView.setDataDirectorySuffix() MUST be called once per process, before any WebView
     * is instantiated. Calling it here in Application.onCreate() guarantees that.
     */
    private void applyWebViewIsolationIfSandboxProcess() {
        try {
            String processName = getCurrentProcessName();
            if (processName != null) {
                int sandboxIdx = extractSandboxIndex(processName);
                if (sandboxIdx >= 0) {
                    WebView.setDataDirectorySuffix("web_sandbox_" + sandboxIdx);
                }
            }
        } catch (Exception e) {
            // Never crash on isolation setup
        }
    }

    private String getCurrentProcessName() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            return Application.getProcessName();
        }
        int pid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am != null) {
            for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
                if (info.pid == pid) return info.processName;
            }
        }
        return null;
    }

    /**
     * Extracts N from a process name like "com.cylonid.nativealpha:web_sandbox_N".
     * Returns -1 if not a sandbox process.
     */
    private int extractSandboxIndex(String processName) {
        int colonIdx = processName.lastIndexOf(':');
        if (colonIdx < 0) return -1;
        String suffix = processName.substring(colonIdx + 1);
        if (!suffix.startsWith("web_sandbox_")) return -1;
        try {
            return Integer.parseInt(suffix.substring("web_sandbox_".length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static Context getAppContext() {
        return App.context;
    }
}
