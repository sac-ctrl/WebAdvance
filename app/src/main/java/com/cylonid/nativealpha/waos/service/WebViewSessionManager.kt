package com.cylonid.nativealpha.waos.service

import android.webkit.WebView
import com.cylonid.nativealpha.waos.model.WaosApp

/**
 * Manages per-app WebView session isolation and scroll state.
 */
object WebViewSessionManager {
    private val sessions = mutableMapOf<Int, WaosApp>()
    private val webViews = mutableMapOf<Int, WebView>()

    fun registerAppSession(app: WaosApp) {
        sessions[app.id] = app
    }

    fun getSession(appId: Int): WaosApp? {
        return sessions[appId]
    }

    fun saveWebView(appId: Int, webView: WebView) {
        webViews[appId] = webView
    }

    fun getWebView(appId: Int): WebView? {
        return webViews[appId]
    }
}
