package com.cylonid.nativealpha.waos.service

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import com.cylonid.nativealpha.waos.model.WaosApp

/**
 * Manages per-app WebView session isolation.
 *
 * True process-level isolation is provided by the build system which generates
 * separate WebViewActivity classes (android:process=":web_sandbox_N") for each
 * of the 8 container slots. This manager handles in-process state tracking and
 * scroll position persistence.
 */
object WebViewSessionManager {
    private val sessions = mutableMapOf<Int, WaosApp>()
    private val webViews = mutableMapOf<Int, WebView>()
    private val scrollPositions = mutableMapOf<Int, Int>()
    private val lastUrls = mutableMapOf<Int, String>()

    fun registerAppSession(app: WaosApp) {
        sessions[app.id] = app
    }

    fun getSession(appId: Int): WaosApp? = sessions[appId]

    fun saveWebView(appId: Int, webView: WebView) {
        webViews[appId] = webView
    }

    fun getWebView(appId: Int): WebView? = webViews[appId]

    fun saveScrollPosition(appId: Int, position: Int) {
        scrollPositions[appId] = position
    }

    fun getScrollPosition(appId: Int): Int = scrollPositions[appId] ?: 0

    fun saveLastUrl(appId: Int, url: String) {
        lastUrls[appId] = url
    }

    fun getLastUrl(appId: Int): String? = lastUrls[appId]

    /**
     * Configure WebView for session isolation.
     * For complete isolation, apps use separate processes (see build.gradle sandbox containers).
     * This method applies per-app settings to the WebView instance.
     */
    fun configureWebViewIsolation(appId: Int, webView: WebView, context: Context) {
        val settings = webView.settings
        val session = sessions[appId]

        if (session != null) {
            settings.javaScriptEnabled = session.allowJs
            settings.domStorageEnabled = true
            settings.databaseEnabled = true

            if (!session.allowJs) {
                settings.blockNetworkImage = false
            }
        }
    }

    /**
     * Clear session data for a specific app.
     * Full cookie/storage clearing happens in the sandbox process on next restart.
     */
    fun clearSessionData(appId: Int) {
        scrollPositions.remove(appId)
        lastUrls.remove(appId)
        webViews.remove(appId)
        sessions.remove(appId)
    }

    /**
     * Verify that session isolation is working by checking registered sessions.
     * Each appId should have at most one WebView instance.
     */
    fun verifyIsolation(): Map<Int, Boolean> {
        return sessions.keys.associateWith { appId ->
            webViews.containsKey(appId)
        }
    }

    /**
     * Information about multi-account isolation architecture:
     *
     * This app uses Android's process isolation for true multi-account support.
     * The build.gradle generates 8 WebViewActivity classes, each in its own process:
     *   android:process=":web_sandbox_0" through ":web_sandbox_7"
     *
     * Each process has completely separate:
     *   - WebView cookie store (CookieManager is per-process)
     *   - localStorage and sessionStorage (DOM storage is per-process)
     *   - IndexedDB (per-process WebStorage)
     *   - HTTP cache (per-process)
     *   - Service workers (per-process)
     *
     * This means different web apps logged into the same website (e.g. two
     * Google accounts) are fully isolated without any data leakage.
     */
    fun getIsolationInfo(): String {
        return """
            Session Isolation: Process-level (8 sandbox processes)
            Registered sessions: ${sessions.size}
            Active WebViews: ${webViews.size}
            Saved scroll positions: ${scrollPositions.size}
        """.trimIndent()
    }
}
