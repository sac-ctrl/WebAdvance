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

        // Seed the global theme switch from prefs in every process so the very
        // first frame of any activity (main UI process AND :webapp_N sandbox
        // processes) renders with the user's selected theme rather than the
        // hardcoded default.
        try {
            String mode = getSharedPreferences("waos_settings", MODE_PRIVATE)
                    .getString("app_theme", "Dark");
            if (mode == null) mode = "Dark";
            com.cylonid.nativealpha.ui.theme.ThemeState.INSTANCE.applyMode(mode);
        } catch (Exception ignored) {}

        applyWebViewIsolationIfSandboxProcess();

        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(this, new Configuration.Builder().build());
        }
    }

    /**
     * WAOS Session Isolation.
     *
     * Each web app is launched into a dedicated process named ":webapp_N"
     * (see WebAppRouter and the WebViewActivityN classes). For these processes
     * we read the per-slot appId from SharedPreferences and apply a unique
     * data-directory suffix `waos_app_<appId>`, giving every web app a fully
     * isolated WebView profile (cookies, localStorage, IndexedDB, cache, service
     * workers). Login data persists per app, but is NEVER shared across apps.
     *
     * Legacy ":web_sandbox_N" processes are also still supported for backward
     * compatibility (they get a per-slot suffix instead of per-app).
     *
     * WebView.setDataDirectorySuffix() MUST be called once per process, before any
     * WebView is instantiated. Calling it here in Application.onCreate() guarantees
     * that, because Application.onCreate runs in every process the app spawns.
     */
    private void applyWebViewIsolationIfSandboxProcess() {
        try {
            String processName = getCurrentProcessName();
            if (processName == null) return;

            int colonIdx = processName.lastIndexOf(':');
            if (colonIdx < 0) return;
            String suffix = processName.substring(colonIdx + 1);

            // Per-app isolation: ":webapp_N" processes pick up the appId stored
            // by WebAppRouter and apply a per-app data directory.
            if (suffix.startsWith("webapp_")) {
                try {
                    int slot = Integer.parseInt(suffix.substring("webapp_".length()));
                    android.content.SharedPreferences prefs =
                            getSharedPreferences("waos_app_slots", MODE_PRIVATE);
                    long appId = prefs.getLong("slot_" + slot + "_app_id", -1L);
                    if (appId >= 0L) {
                        WebView.setDataDirectorySuffix("waos_app_" + appId);
                    } else {
                        WebView.setDataDirectorySuffix("waos_slot_" + slot);
                    }
                } catch (NumberFormatException ignored) {
                }
                return;
            }

            // Legacy per-slot isolation for ":web_sandbox_N" processes.
            int sandboxIdx = extractSandboxIndex(processName);
            if (sandboxIdx >= 0) {
                WebView.setDataDirectorySuffix("web_sandbox_" + sandboxIdx);
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
