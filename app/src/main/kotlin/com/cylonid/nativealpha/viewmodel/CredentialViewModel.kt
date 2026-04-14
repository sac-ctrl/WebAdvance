package com.cylonid.nativealpha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.helper.BiometricPromptHelper
import com.cylonid.nativealpha.manager.CredentialManager
import com.cylonid.nativealpha.repository.WebAppRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CredentialViewModel @Inject constructor(
    private val credentialDao: CredentialDao,
    private val credentialManager: CredentialManager,
    private val webAppRepository: WebAppRepository
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

    fun loadCredentials(webAppId: Long?) {
        currentWebAppId = webAppId
        viewModelScope.launch {
            if (webAppId == null) {
                // Global credentials - combine all apps
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
        // TODO: Show toast
    }

    fun deleteCredential(credential: Credential) {
        credentialManager.deleteCredential(credential)
    }

    fun checkAuthentication(webAppId: Long) {
        viewModelScope.launch {
            val webApp = webAppRepository.getWebAppById(webAppId).firstOrNull()
            if (webApp?.isLocked == true && webApp.pin?.isNotBlank() == true) {
                _isAuthenticated.value = false
                _showPinDialog.value = false
                _showBiometricButton.value = true // Assume biometric is available for now
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
            } else {
                // TODO: Show error
            }
        }
    }

    fun authenticateWithBiometric(activity: androidx.fragment.app.FragmentActivity) {
        val helper = BiometricPromptHelper(activity)
        helper.showPrompt(
            funSuccess = { _isAuthenticated.value = true; _showPinDialog.value = false },
            funFail = { /* Handle failure */ },
            promptTitle = "Unlock Credential Vault"
        )
    }

    fun showPinDialog() {
        _showPinDialog.value = true
    }
}