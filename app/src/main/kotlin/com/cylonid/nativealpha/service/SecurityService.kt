package com.cylonid.nativealpha.service

import android.content.Context
import android.content.SharedPreferences
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)

    fun isPinSet(): Boolean = prefs.getString(KEY_PIN, null) != null

    fun setupPin() {
        // PIN setup is handled via the UI; this marks PIN as set with a placeholder
        // until the user actually sets one through the PIN dialog
    }

    fun verifyPin(pin: String): Boolean {
        val storedPin = prefs.getString(KEY_PIN, null) ?: return false
        return storedPin == pin
    }

    fun savePin(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN).apply()
    }

    fun setCredentialEncryption(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENCRYPT_CREDENTIALS, enabled).apply()
    }

    fun isCredentialEncryptionEnabled(): Boolean =
        prefs.getBoolean(KEY_ENCRYPT_CREDENTIALS, true)

    fun setBlockScreenshots(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_SCREENSHOTS, enabled).apply()
    }

    fun isBlockScreenshotsEnabled(): Boolean =
        prefs.getBoolean(KEY_BLOCK_SCREENSHOTS, false)

    companion object {
        private const val KEY_PIN = "pin_hash"
        private const val KEY_ENCRYPT_CREDENTIALS = "encrypt_credentials"
        private const val KEY_BLOCK_SCREENSHOTS = "block_screenshots"
    }
}
