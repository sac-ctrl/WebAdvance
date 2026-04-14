package com.cylonid.nativealpha.crash

import android.content.Context
import android.content.Intent
import android.os.Process

class GlobalCrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            CrashLogStorage.saveCrash(context, throwable, thread.name)

            val intent = Intent(context, CrashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(CrashActivity.EXTRA_CRASH_ID, "latest")
            }
            context.startActivity(intent)

            Thread.sleep(400)
        } catch (_: Exception) {}

        defaultHandler?.uncaughtException(thread, throwable)
        Process.killProcess(Process.myPid())
    }

    companion object {
        fun install(context: Context) {
            val default = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                GlobalCrashHandler(context.applicationContext, default)
            )
        }
    }
}
