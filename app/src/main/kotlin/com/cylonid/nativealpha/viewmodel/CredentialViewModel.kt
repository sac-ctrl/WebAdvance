package com.cylonid.nativealpha.viewmodel

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.data.CredentialDao
import com.cylonid.nativealpha.helper.BiometricPromptHelper
import com.cylonid.nativealpha.manager.Credential
import com.cylonid.nativealpha.manager.CredentialManager
import com.cylonid.nativealpha.repository.WebAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CredentialViewModel @Inject constructor(
    private val credentialDao: CredentialDao,
    private val credentialManager: CredentialManager,
    private val webAppRepository: WebAppRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var currentWebAppId: Long? = null
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _credentials = MutableStateFlow<List<Credential>>(emptyList())
    val credentials: StateFlow<List<Credential>> = combine(
        _credentials,
        _searchQuery
    ) { creds, query ->
        if (query.isBlank()) creds
        else creds.filter { it.title.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val _showBiometricButton = MutableStateFlow(false)
    val showBiometricButton: StateFlow<Boolean> = _showBiometricButton

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    private val _editingCredential = MutableStateFlow<Credential?>(null)
    val editingCredential: StateFlow<Credential?> = _editingCredential

    private val _showPinDialog = MutableStateFlow(false)
    val showPinDialog: StateFlow<Boolean> = _showPinDialog

    // Auto-lock timer properties
    private val autoLockHandler = Handler(Looper.getMainLooper())
    private var autoLockTimeoutMs = (5 * 60 * 1000).toLong() // Default 5 minutes
    private val autoLockRunnable = Runnable {
        lockVault()
    }

    fun loadCredentials(webAppId: Long?) {
        currentWebAppId = webAppId
        viewModelScope.launch {
            if (webAppId == null) {
                credentialManager.getDecryptedCredentialsForApp(0L).collect { creds ->
                    _credentials.value = creds
                }
            } else {
                credentialManager.getDecryptedCredentialsForApp(webAppId).collect { creds ->
                    _credentials.value = creds
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun showAddCredentialDialog() {
        _editingCredential.value = null
        _showAddDialog.value = true
    }

    fun hideAddCredentialDialog() {
        _showAddDialog.value = false
        _editingCredential.value = null
    }

    fun editCredential(credential: Credential) {
        _editingCredential.value = credential
        _showAddDialog.value = true
    }

    fun saveCredential(webAppId: Long, credential: Credential) {
        val credentialWithAppId = credential.copy(webAppId = webAppId)
        if (credential.id == 0L) {
            credentialManager.saveCredential(webAppId, credentialWithAppId)
        } else {
            credentialManager.updateCredential(credentialWithAppId)
        }
    }

    fun copyUsername(credential: Credential, context: android.content.Context) {
        copyToClipboard(context, "Username", credential.username)
    }

    fun copyPassword(credential: Credential, context: android.content.Context) {
        copyToClipboard(context, "Password", credential.password)
    }

    private fun copyToClipboard(context: android.content.Context, label: String, text: String) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun deleteCredential(credential: Credential) {
        credentialManager.deleteCredential(credential)
    }

    fun checkAuthentication(webAppId: Long) {
        viewModelScope.launch {
            val webApp = webAppRepository.getWebAppById(webAppId).firstOrNull()
            val hasPin = webApp?.pin?.isNotBlank() == true
            if (webApp?.isLocked == true && hasPin) {
                _isAuthenticated.value = false
                _showPinDialog.value = false
                _showBiometricButton.value = true
            } else if (webApp?.isLocked == true && !hasPin) {
                // Locked but no PIN configured — require biometric only
                _isAuthenticated.value = false
                _showPinDialog.value = false
                _showBiometricButton.value = true
            } else {
                _isAuthenticated.value = true
            }
        }
    }

    fun authenticateWithPin(pin: String) {
        viewModelScope.launch {
            val webApp = currentWebAppId?.let { webAppRepository.getWebAppById(it).firstOrNull() }
            if (webApp?.pin == pin) {
                _isAuthenticated.value = true
                _showPinDialog.value = false
            }
        }
    }

    fun authenticateWithBiometric(activity: androidx.fragment.app.FragmentActivity) {
        val helper = BiometricPromptHelper(activity)
        helper.showPrompt(
            funSuccess = { _isAuthenticated.value = true; _showPinDialog.value = false },
            funFail = { },
            promptTitle = "Unlock Credential Vault"
        )
    }

    fun showPinDialog() {
        _showPinDialog.value = true
    }

    fun hidePinDialog() {
        _showPinDialog.value = false
    }

    // Auto-lock timer management
    fun startAutoLockTimer() {
        autoLockHandler.removeCallbacks(autoLockRunnable)
        if (autoLockTimeoutMs > 0) {
            autoLockHandler.postDelayed(autoLockRunnable, autoLockTimeoutMs)
        }
    }

    fun resetAutoLockTimer() {
        if (_isAuthenticated.value) {
            autoLockHandler.removeCallbacks(autoLockRunnable)
            autoLockHandler.postDelayed(autoLockRunnable, autoLockTimeoutMs)
        }
    }

    fun cancelAutoLockTimer() {
        autoLockHandler.removeCallbacks(autoLockRunnable)
    }

    private fun lockVault() {
        _isAuthenticated.value = false
        _showPinDialog.value = false
        Toast.makeText(context, "Vault locked due to inactivity", Toast.LENGTH_SHORT).show()
    }

    fun hapticTap() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30)
            }
        } catch (ignored: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        cancelAutoLockTimer()
    }
}
