package com.cylonid.nativealpha.webview

import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File

/**
 * Manages isolated WebView session data (cookies, localStorage, cache) per app
 */
class SessionManager(
    private val context: Context,
    private val appId: Long,
    private val appName: String
) {
    private val gson = Gson()
    private val sessionDir = File(context.getExternalFilesDir("sessions"), appId.toString())
    private val prefs: SharedPreferences
    
    init {
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        prefs = context.getSharedPreferences("session_$appId", Context.MODE_PRIVATE)
    }

    /**
     * Create isolated WebView for this app
     */
    fun createIsolatedWebView(context: Context, url: String): WebView {
        val dataDir = File(context.dataDir, "webview_$appId")
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }

        val webView = WebView(context).apply {
            // Set data directory for isolation
            WebView.setDataDirectorySuffix("app_$appId")
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                setAppCachePath(dataDir.absolutePath + "/cache")
                setAppCacheEnabled(true)
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                
                // Set user agent if custom one is stored
                userAgentString?.let {
                    userAgentString = it
                }
            }

            // Set cache directory
            webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    saveSessionState("page_start", url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    saveSessionState("page_finish", url)
                }
            }
        }
        return webView
    }

    /**
     * Save session state (cookies, localStorage)
     */
    fun saveSessionState(trigger: String = "manual", uri: String? = null) {
        val state = JsonObject().apply {
            addProperty("appId", appId)
            addProperty("appName", appName)
            addProperty("timestamp", System.currentTimeMillis())
            addProperty("trigger", trigger)
            addProperty("uri", uri)
            addProperty("sessionData", prefs.all.toString())
        }

        val file = File(sessionDir, "session_state.json")
        file.writeText(gson.toJson(state))
    }

    /**
     * Load previous session state
     */
    fun loadSessionState(): JsonObject? {
        val file = File(sessionDir, "session_state.json")
        return if (file.exists()) {
            try {
                gson.fromJson(file.readText(), JsonObject::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    /**
     * Clear session data
     */
    fun clearSessionData() {
        prefs.edit().clear().apply()
        sessionDir.deleteRecursively()
        sessionDir.mkdirs()
    }

    /**
     * Export session to file (for backup)
     */
    fun exportSession(targetFile: File): Boolean {
        return try {
            val sessionFile = File(sessionDir, "session_state.json")
            if (sessionFile.exists()) {
                sessionFile.copyTo(targetFile, overwrite = true)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Import session from file (for restore)
     */
    fun importSession(sourceFile: File): Boolean {
        return try {
            if (sourceFile.exists()) {
                val targetFile = File(sessionDir, "session_state.json")
                sourceFile.copyTo(targetFile, overwrite = true)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get session data for manual saves
     */
    fun getSessionData(): String {
        return gson.toJson(mapOf(
            "appId" to appId,
            "appName" to appName,
            "timestamp" to System.currentTimeMillis(),
            "data" to prefs.all
        ))
    }

    /**
     * Store custom session value
     */
    fun setSessionValue(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    /**
     * Get custom session value
     */
    fun getSessionValue(key: String, default: String = ""): String {
        return prefs.getString(key, default) ?: default
    }

    /**
     * Get session directory size in bytes
     */
    fun getSessionSize(): Long {
        return sessionDir.walkTopDown().sumOf { it.length() }
    }

    /**
     * Clone current session to new app
     */
    fun cloneToApp(newAppId: Long, newAppName: String): SessionManager {
        val newManager = SessionManager(context, newAppId, newAppName)
        sessionDir.copyRecursively(newManager.sessionDir, overwrite = true)
        return newManager
    }
}
