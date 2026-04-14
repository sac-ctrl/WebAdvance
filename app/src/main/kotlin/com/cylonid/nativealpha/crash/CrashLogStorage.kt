package com.cylonid.nativealpha.crash

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class CrashEntry(
    val id: String,
    val timestamp: String,
    val message: String,
    val stackTrace: String,
    val threadName: String,
    val appVersion: String
)

object CrashLogStorage {
    private const val PREFS_NAME = "waos_crash_logs"
    private const val KEY_LOGS = "crash_log_list"
    private const val MAX_ENTRIES = 20

    fun saveCrash(context: Context, throwable: Throwable, threadName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = try {
            JSONArray(prefs.getString(KEY_LOGS, "[]") ?: "[]")
        } catch (e: Exception) {
            JSONArray()
        }

        val entry = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            put("message", throwable.message ?: throwable.javaClass.simpleName)
            put("stackTrace", throwable.stackTraceToString())
            put("threadName", threadName)
            put("appVersion", try {
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                "${info.versionName} (${info.versionCode})"
            } catch (e: Exception) { "unknown" })
        }

        val updated = JSONArray().apply {
            put(entry)
            for (i in 0 until minOf(existing.length(), MAX_ENTRIES - 1)) {
                put(existing.getJSONObject(i))
            }
        }

        prefs.edit().putString(KEY_LOGS, updated.toString()).apply()
    }

    fun getCrashLogs(context: Context): List<CrashEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = try {
            JSONArray(prefs.getString(KEY_LOGS, "[]") ?: "[]")
        } catch (e: Exception) {
            JSONArray()
        }
        return (0 until json.length()).mapNotNull {
            try {
                val obj = json.getJSONObject(it)
                CrashEntry(
                    id = obj.getString("id"),
                    timestamp = obj.getString("timestamp"),
                    message = obj.getString("message"),
                    stackTrace = obj.getString("stackTrace"),
                    threadName = obj.optString("threadName", "main"),
                    appVersion = obj.optString("appVersion", "unknown")
                )
            } catch (e: Exception) { null }
        }
    }

    fun clearLogs(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_LOGS).apply()
    }

    fun getLatestCrash(context: Context): CrashEntry? = getCrashLogs(context).firstOrNull()
}
