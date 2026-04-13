package com.cylonid.nativealpha.waos.model

import com.cylonid.nativealpha.model.WebApp

/**
 * WAOS application metadata that extends the existing WebApp model.
 * This class stores per-app settings and runtime state.
 */
data class WaosApp(
    val id: Int,
    var title: String,
    var url: String,
    var iconUrl: String? = null,
    var group: String = "Default",
    var desktopMode: Boolean = false,
    var allowJs: Boolean = true,
    var enableAdblock: Boolean = true,
    var darkMode: Boolean = false,
    var refreshIntervalSeconds: Int = 30,
    var useSmartRefresh: Boolean = true,
    var clipperEnabled: Boolean = true,
    var credentialKeeperEnabled: Boolean = true,
    var floatingWindowEnabled: Boolean = true,
    var floatingWindowWidth: Int = 360,
    var floatingWindowHeight: Int = 640,
    var floatingWindowOpacity: Int = 100,
    var downloadFolder: String = "WAOS/$title/Downloads",
    var lastUrl: String = url,
    var lastScrollPosition: Int = 0,
) {
    fun toWebApp(): WebApp {
        return WebApp(url, id, id).apply {
            this.title = title
            this.isAllowJs = allowJs
            this.isRequestDesktop = desktopMode
            this.isUseAdblock = enableAdblock
            this.isUseContainer = floatingWindowEnabled
        }
    }
}
