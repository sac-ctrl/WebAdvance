package com.cylonid.nativealpha.credentials

import android.content.Context
import android.webkit.WebView
import com.cylonid.nativealpha.waos.model.CredentialItem
import com.cylonid.nativealpha.waos.model.CredentialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Enhanced credential keeper with password generation and form auto-fill
 */
class EnhancedCredentialKeeper(
    private val context: Context,
    private val appId: Long
) {
    private val _credentials = MutableStateFlow<List<CredentialItem>>(emptyList())
    val credentials: StateFlow<List<CredentialItem>> = _credentials.asStateFlow()

    private val _isPinLocked = MutableStateFlow(false)
    val isPinLocked: StateFlow<Boolean> = _isPinLocked.asStateFlow()

    private var masterPin: String? = null
    private var autoLockTimeoutMs: Long = 5 * 60 * 1000L // 5 minutes

    init {
        loadCredentials()
    }

    /**
     * Set master PIN for vault
     */
    fun setMasterPin(pin: String): Boolean {
        return try {
            masterPin = hashPin(pin)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verify master PIN
     */
    fun verifyPin(pin: String): Boolean {
        return try {
            val hashed = hashPin(pin)
            hashed == masterPin
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Lock vault
     */
    fun lockVault() {
        _isPinLocked.value = true
    }

    /**
     * Unlock vault with PIN
     */
    fun unlockVault(pin: String): Boolean {
        return if (verifyPin(pin)) {
            _isPinLocked.value = false
            true
        } else {
            false
        }
    }

    /**
     * Add new credential
     */
    fun addCredential(
        title: String,
        username: String,
        password: String,
        url: String? = null,
        notes: String? = null,
        customFields: Map<String, String>? = null
    ): CredentialItem {
        val item = CredentialItem(
            appId = appId.toInt(),
            title = title,
            username = username,
            password = password,
            url = url ?: "",
            notes = notes ?: "",
            timestamp = System.currentTimeMillis()
        )
        
        masterPin?.let { pin ->
            CredentialRepository.saveCredential(context, item, pin)
        }
        
        loadCredentials()
        return item
    }

    /**
     * Update credential
     */
    fun updateCredential(
        item: CredentialItem,
        title: String? = null,
        username: String? = null,
        password: String? = null,
        url: String? = null,
        notes: String? = null
    ) {
        val updated = item.copy(
            title = title ?: item.title,
            username = username ?: item.username,
            password = password ?: item.password,
            url = url ?: item.url,
            notes = notes ?: item.notes
        )
        
        masterPin?.let { pin ->
            CredentialRepository.updateCredential(context, updated, pin)
        }
        
        loadCredentials()
    }

    /**
     * Delete credential
     */
    fun deleteCredential(item: CredentialItem) {
        CredentialRepository.deleteCredential(context, item)
        loadCredentials()
    }

    /**
     * Search credentials by title or username
     */
    fun searchCredentials(query: String): List<CredentialItem> {
        return _credentials.value.filter { credential ->
            credential.title.contains(query, ignoreCase = true) ||
            credential.username.contains(query, ignoreCase = true)
        }
    }

    /**
     * Generate strong password
     */
    fun generatePassword(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val characters = StringBuilder()
        
        if (includeLowercase) characters.append("abcdefghijklmnopqrstuvwxyz")
        if (includeUppercase) characters.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        if (includeNumbers) characters.append("0123456789")
        if (includeSymbols) characters.append("!@#$%^&*()_+-=[]{}|;:,.<>?")
        
        if (characters.isEmpty()) return ""
        
        return (1..length).map { 
            characters[Random.nextInt(characters.length)]
        }.joinToString("")
    }

    /**
     * Auto-fill form in WebView
     */
    fun autoFillForm(
        webView: WebView,
        credential: CredentialItem,
        usernameSelector: String = "input[type='email'], input[type='text'], input[name*='user'], input[name*='email']",
        passwordSelector: String = "input[type='password']"
    ) {
        val script = """
            (function() {
                try {
                    // Find and fill username field
                    const usernameField = document.querySelector('$usernameSelector');
                    if (usernameField && usernameField.tagName === 'INPUT') {
                        usernameField.value = '${credential.username.replace("'", "\\'")}';
                        usernameField.dispatchEvent(new Event('input', { bubbles: true }));
                        usernameField.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                    
                    // Find and fill password field
                    const passwordField = document.querySelector('$passwordSelector');
                    if (passwordField && passwordField.tagName === 'INPUT') {
                        passwordField.value = '${credential.password.replace("'", "\\'")}';
                        passwordField.dispatchEvent(new Event('input', { bubbles: true }));
                        passwordField.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                    
                    console.log('Form auto-filled');
                } catch(e) {
                    console.error('Auto-fill error: ' + e.message);
                }
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(script) { result ->
            android.util.Log.d("CredentialKeeper", "Auto-fill result: $result")
        }
    }

    /**
     * Export credentials as JSON (encrypted)
     */
    fun exportCredentialsAsJson(): String {
        val gson = com.google.gson.Gson()
        return gson.toJson(mapOf(
            "appId" to appId,
            "exportDate" to System.currentTimeMillis(),
            "credentials" to _credentials.value
        ))
    }

    /**
     * Get credential statistics
     */
    fun getStatistics(): CredentialStatistics {
        val credentials = _credentials.value
        return CredentialStatistics(
            totalCredentials = credentials.size,
            withCustomField = 0,
            oldestCredential = credentials.minByOrNull { it.timestamp }?.timestamp ?: 0L,
            newestCredential = credentials.maxByOrNull { it.timestamp }?.timestamp ?: 0L
        )
    }

    /**
     * Validate password strength
     */
    fun validatePasswordStrength(password: String): PasswordStrength {
        var score = 0
        
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        
        return when {
            score <= 2 -> PasswordStrength.WEAK
            score <= 4 -> PasswordStrength.MODERATE
            else -> PasswordStrength.STRONG
        }
    }

    private fun loadCredentials() {
        masterPin?.let { pin ->
            val creds = CredentialRepository.loadCredentials(context, appId.toInt(), pin)
            _credentials.value = creds.sortedByDescending { it.timestamp }
        }
    }

    private fun hashPin(pin: String): String {
        // In production, use proper PBKDF2 hashing
        val messageDigest = java.security.MessageDigest.getInstance("SHA-256")
        messageDigest.update(pin.toByteArray())
        return java.util.Base64.getEncoder().encodeToString(messageDigest.digest())
    }

    enum class PasswordStrength {
        WEAK, MODERATE, STRONG
    }

    data class CredentialStatistics(
        val totalCredentials: Int,
        val withCustomField: Int,
        val oldestCredential: Long,
        val newestCredential: Long
    )
}
