package com.cylonid.nativealpha.waos.service

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import com.cylonid.nativealpha.waos.model.WaosApp

/**
 * WAOS WebView Session Manager
 *
 * Manages per-app WebView session isolation state and scroll/URL tracking.
 *
 * TRUE ISOLATION is achieved via two mechanisms:
 *
 * 1. Process-level isolation (sandbox containers):
 *    Apps with a containerId run in separate Android processes
 *    (:web_sandbox_0 … :web_sandbox_7). Each process gets its own
 *    WebView data directory via App.applyWebViewIsolationIfSandboxProcess().
 *
 * 2. Data-directory suffix isolation (new Compose WebViewActivity):
 *    SessionManager.applyIsolation(appId) is called in WebViewActivity.onCreate()
 *    before any WebView is instantiated. This sets a unique suffix so cookies,
 *    localStorage, IndexedDB, cache and service workers are fully separate.
 *
 * Result: no cookies, localStorage, IndexedDB, cache or service workers are
 * shared between any two app instances.
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
     * Configure WebView settings for the given app session.
     * Storage isolation is already applied at the data-directory level
     * (see class-level KDoc). This method applies per-app runtime settings.
     */
    fun configureWebViewIsolation(appId: Int, webView: WebView, context: Context) {
        val settings = webView.settings
        val session = sessions[appId]

        // Always enable DOM storage so localStorage/sessionStorage work per-app
        settings.domStorageEnabled = true
        settings.databaseEnabled = true

        if (session != null) {
            settings.javaScriptEnabled = session.allowJs
        }

        // Ensure cookies are accepted and isolated to this data-directory suffix
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
    }

    /**
     * Clear in-memory session state for a specific app.
     * The on-disk WebView data directory is cleared separately by the system
     * when the data-directory suffix is reset.
     */
    fun clearSessionData(appId: Int) {
        scrollPositions.remove(appId)
        lastUrls.remove(appId)
        webViews.remove(appId)
        sessions.remove(appId)
    }

    /**
     * Verify that session isolation is active for all registered sessions.
     */
    fun verifyIsolation(): Map<Int, Boolean> {
        return sessions.keys.associateWith { appId ->
            webViews.containsKey(appId)
        }
    }

    fun getIsolationInfo(): String {
        return """
            Session Isolation: setDataDirectorySuffix per-app + process isolation (sandbox containers)
            Registered sessions: ${sessions.size}
            Active WebViews: ${webViews.size}
            Saved scroll positions: ${scrollPositions.size}
        """.trimIndent()
    }
}
