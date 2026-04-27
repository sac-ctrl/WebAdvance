package com.cylonid.nativealpha.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.service.BackupService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupService: BackupService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("waos_settings", Context.MODE_PRIVATE)

    var appTheme by mutableStateOf(prefs.getString("app_theme", "Dark") ?: "Dark")
        private set
    var dashboardColumns by mutableStateOf(prefs.getInt("dashboard_columns", 2))
        private set
    var showCategoryChips by mutableStateOf(prefs.getBoolean("show_category_chips", true))
        private set
    var showStatusIndicators by mutableStateOf(prefs.getBoolean("show_status_indicators", true))
        private set
    var animatedCards by mutableStateOf(prefs.getBoolean("animated_cards", true))
        private set
    var globalNotificationsEnabled by mutableStateOf(prefs.getBoolean("global_notifications", true))
        private set
    var showBadgeCount by mutableStateOf(prefs.getBoolean("show_badge_count", true))
        private set
    var floatingWindowsEnabled by mutableStateOf(prefs.getBoolean("floating_windows", true))
        private set
    var maxFloatingWindows by mutableStateOf(prefs.getInt("max_floating_windows", 5))
        private set
    var globalClipboardEnabled by mutableStateOf(prefs.getBoolean("global_clipboard", true))
        private set
    var globalVaultEnabled by mutableStateOf(prefs.getBoolean("global_vault", false))
        private set
    var globalAutoLockTimeout by mutableStateOf(prefs.getLong("auto_lock_timeout", 300000L))
        private set
    var screenOrientation by mutableStateOf(prefs.getString("screen_orientation", "Portrait") ?: "Portrait")
        private set
    var developerModeEnabled by mutableStateOf(prefs.getBoolean("developer_mode", false))
        private set
    var autoScrollSpeed by mutableStateOf(prefs.getInt("auto_scroll_speed", 3))
        private set

    var isExporting by mutableStateOf(false)
        private set
    var isImporting by mutableStateOf(false)
        private set
    var lastExportMessage by mutableStateOf("")
        private set

    fun updateAppTheme(value: String) {
        appTheme = value
        prefs.edit().putString("app_theme", value).apply()
        // Push to the global theme switch so every currently-visible activity
        // re-themes immediately (no restart needed).
        com.cylonid.nativealpha.ui.theme.ThemeState.applyMode(value)
    }

    fun updateShowCategoryChips(value: Boolean) {
        showCategoryChips = value
        prefs.edit().putBoolean("show_category_chips", value).apply()
    }

    fun updateShowStatusIndicators(value: Boolean) {
        showStatusIndicators = value
        prefs.edit().putBoolean("show_status_indicators", value).apply()
    }

    fun updateAnimatedCards(value: Boolean) {
        animatedCards = value
        prefs.edit().putBoolean("animated_cards", value).apply()
    }

    fun updateGlobalNotificationsEnabled(value: Boolean) {
        globalNotificationsEnabled = value
        prefs.edit().putBoolean("global_notifications", value).apply()
    }

    fun updateShowBadgeCount(value: Boolean) {
        showBadgeCount = value
        prefs.edit().putBoolean("show_badge_count", value).apply()
    }

    fun updateFloatingWindowsEnabled(value: Boolean) {
        floatingWindowsEnabled = value
        prefs.edit().putBoolean("floating_windows", value).apply()
    }

    fun updateMaxFloatingWindows(value: Int) {
        maxFloatingWindows = value
        prefs.edit().putInt("max_floating_windows", value).apply()
    }

    fun updateGlobalClipboardEnabled(value: Boolean) {
        globalClipboardEnabled = value
        prefs.edit().putBoolean("global_clipboard", value).apply()
    }

    fun updateGlobalVaultEnabled(value: Boolean) {
        globalVaultEnabled = value
        prefs.edit().putBoolean("global_vault", value).apply()
    }

    fun updateDeveloperModeEnabled(value: Boolean) {
        developerModeEnabled = value
        prefs.edit().putBoolean("developer_mode", value).apply()
    }

    fun updateAutoScrollSpeed(value: Int) {
        autoScrollSpeed = value
        prefs.edit().putInt("auto_scroll_speed", value).apply()
    }

    /**
     * User picked a destination folder via SAF — write the comprehensive
     * .waos backup into it.
     */
    fun exportToFolder(treeUri: Uri) {
        isExporting = true
        lastExportMessage = ""
        viewModelScope.launch {
            try {
                // Persist read/write permission on the picked folder.
                try {
                    val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(treeUri, flags)
                } catch (_: Exception) { /* not all providers support this */ }

                val result = backupService.writeBackupToFolder(treeUri)
                lastExportMessage = result.message
            } catch (e: Exception) {
                lastExportMessage = "Export error: ${e.message}"
            } finally {
                isExporting = false
            }
        }
    }

    /**
     * User picked a .waos file via SAF — restore everything from it.
     */
    fun importFromFile(fileUri: Uri) {
        isImporting = true
        lastExportMessage = ""
        viewModelScope.launch {
            try {
                val result = backupService.restoreFromUri(fileUri)
                lastExportMessage = result.message
            } catch (e: Exception) {
                lastExportMessage = "Restore error: ${e.message}"
            } finally {
                isImporting = false
            }
        }
    }

    fun clearExportMessage() {
        lastExportMessage = ""
    }

    companion object {
        fun isDarkMode(context: Context): Boolean {
            val prefs = context.getSharedPreferences("waos_settings", Context.MODE_PRIVATE)
            return when (prefs.getString("app_theme", "Dark")) {
                "Light" -> false
                "System" -> {
                    val cfg = context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    cfg == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
                else -> true // Dark, Matrix
            }
        }
    }
}
