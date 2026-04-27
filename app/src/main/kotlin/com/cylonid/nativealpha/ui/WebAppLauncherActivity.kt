package com.cylonid.nativealpha.ui

import android.app.Activity
import android.os.Bundle
import com.cylonid.nativealpha.util.WebAppRouter

/**
 * Tiny exported trampoline activity used by home-screen shortcuts. It receives
 * the `webAppId` extra, hands off to [WebAppRouter] (which picks the correct
 * sandboxed `:webapp_N` process), then finishes immediately so it never appears
 * in Recents or shows a blank screen.
 */
class WebAppLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webAppId = intent?.getLongExtra(WebAppRouter.EXTRA_WEB_APP_ID, -1L) ?: -1L
        if (webAppId > 0L) {
            try {
                WebAppRouter.launch(this, webAppId)
            } catch (_: Exception) {}
        }
        finish()
    }
}
