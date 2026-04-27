package com.cylonid.nativealpha.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Process
import com.cylonid.nativealpha.ui.WebViewActivity0
import com.cylonid.nativealpha.ui.WebViewActivity1
import com.cylonid.nativealpha.ui.WebViewActivity2
import com.cylonid.nativealpha.ui.WebViewActivity3
import com.cylonid.nativealpha.ui.WebViewActivity4
import com.cylonid.nativealpha.ui.WebViewActivity5
import com.cylonid.nativealpha.ui.WebViewActivity6
import com.cylonid.nativealpha.ui.WebViewActivity7

/**
 * Routes every web-app launch to a dedicated, per-app sandboxed process so each
 * app gets a fully isolated WebView profile (cookies, localStorage, IndexedDB,
 * cache, service workers). Login data persists *per app*, but is NEVER shared
 * across apps — same as opening each app in its own incognito browser, except
 * each app keeps its own persistent identity.
 *
 * How it works:
 * 1. Each app is mapped to one of 8 slots: `slot = appId % 8`.
 * 2. The slot's currently-loaded appId is recorded in SharedPreferences.
 * 3. Before launching, if the slot currently holds a *different* appId, that
 *    process is killed so the next launch starts a clean process which then
 *    applies the new app's data-directory suffix in `App.onCreate()`.
 * 4. The activity is then launched into the matching `:webapp_N` process.
 *
 * Apps that map to the same slot can both run; switching between them recycles
 * the slot's process so the WebView always uses the correct profile.
 */
object WebAppRouter {

    const val SLOT_COUNT = 8
    const val PREF_NAME = "waos_app_slots"
    const val EXTRA_WEB_APP_ID = "webAppId"

    private fun slotFor(appId: Long): Int {
        val n = SLOT_COUNT
        return ((appId % n).toInt() + n) % n
    }

    @JvmStatic
    fun launch(context: Context, webAppId: Long) {
        if (webAppId <= 0L) return

        val slot = slotFor(webAppId)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = "slot_${slot}_app_id"
        val previousId = prefs.getLong(key, -1L)

        if (previousId != webAppId) {
            prefs.edit().putLong(key, webAppId).commit()
            if (previousId >= 0L) {
                killSlotProcess(context, slot)
            }
        }

        val activityClass: Class<*> = when (slot) {
            0 -> WebViewActivity0::class.java
            1 -> WebViewActivity1::class.java
            2 -> WebViewActivity2::class.java
            3 -> WebViewActivity3::class.java
            4 -> WebViewActivity4::class.java
            5 -> WebViewActivity5::class.java
            6 -> WebViewActivity6::class.java
            else -> WebViewActivity7::class.java
        }

        val intent = Intent(context, activityClass).apply {
            putExtra(EXTRA_WEB_APP_ID, webAppId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        context.startActivity(intent)
    }

    private fun killSlotProcess(context: Context, slot: Int) {
        try {
            val processName = "${context.packageName}:webapp_$slot"
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return
            val procs = am.runningAppProcesses ?: return
            for (p in procs) {
                if (p.processName == processName && p.pid != Process.myPid()) {
                    Process.killProcess(p.pid)
                }
            }
            // Brief pause to let the OS reap the process before we relaunch it
            Thread.sleep(180)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
