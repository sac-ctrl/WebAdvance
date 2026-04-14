package com.cylonid.nativealpha.util

import android.app.ActivityManager
import android.os.Process

object ProcessUtils {
    fun closeAllWebAppsAndProcesses(activityManager: ActivityManager) {
        for (task in activityManager.appTasks) {
            task.finishAndRemoveTask()
        }
        for (processInfo in activityManager.runningAppProcesses ?: emptyList()) {
            if (processInfo.processName.contains("web_sandbox_")) {
                Process.killProcess(processInfo.pid)
            }
        }
    }
}
