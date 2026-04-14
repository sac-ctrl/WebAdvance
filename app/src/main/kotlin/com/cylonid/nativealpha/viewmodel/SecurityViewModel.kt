package com.cylonid.nativealpha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.data.SecurityRepository
import com.cylonid.nativealpha.model.SecuritySettingsEntity
import com.cylonid.nativealpha.service.BiometricService
import com.cylonid.nativealpha.service.SecurityService
import com.cylonid.nativealpha.ui.screens.AutoLockTime
import com.cylonid.nativealpha.ui.screens.SecuritySettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val biometricService: BiometricService,
    private val securityService: SecurityService
) : ViewModel() {

    private val _securitySettings = MutableStateFlow(SecuritySettings())
    val securitySettings: StateFlow<SecuritySettings> = _securitySettings.asStateFlow()

    private val _biometricAvailable = MutableStateFlow(false)
    val biometricAvailable: StateFlow<Boolean> = _biometricAvailable.asStateFlow()

    private val _pinSet = MutableStateFlow(false)
    val pinSet: StateFlow<Boolean> = _pinSet.asStateFlow()

    init {
        loadSettings()
        checkBiometricAvailability()
        checkPinStatus()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = securityRepository.getSecuritySettings()
            _securitySettings.value = SecuritySettings(
                biometricEnabled = settings.biometricEnabled,
                autoLockTime = AutoLockTime.values().find { it.minutes == settings.autoLockMinutes }
                    ?: AutoLockTime.FIFTEEN_MINUTES,
                encryptCredentials = settings.encryptCredentials,
                clearDataOnFail = settings.clearDataOnFail,
                blockScreenshots = settings.blockScreenshots,
                incognitoMode = settings.incognitoMode
            )
        }
    }

    private fun checkBiometricAvailability() {
        _biometricAvailable.value = biometricService.isBiometricAvailable()
    }

    private fun checkPinStatus() {
        _pinSet.value = securityService.isPinSet()
    }

    fun toggleBiometric(enabled: Boolean) {
        if (enabled && !biometricService.authenticate()) {
            return // Authentication failed
        }
        _securitySettings.value = _securitySettings.value.copy(biometricEnabled = enabled)
        saveSettings()
    }

    fun setAutoLockTime(time: AutoLockTime) {
        _securitySettings.value = _securitySettings.value.copy(autoLockTime = time)
        saveSettings()
    }

    fun toggleCredentialEncryption(enabled: Boolean) {
        _securitySettings.value = _securitySettings.value.copy(encryptCredentials = enabled)
        saveSettings()
        securityService.setCredentialEncryption(enabled)
    }

    fun toggleClearDataOnFail(enabled: Boolean) {
        _securitySettings.value = _securitySettings.value.copy(clearDataOnFail = enabled)
        saveSettings()
    }

    fun toggleBlockScreenshots(enabled: Boolean) {
        _securitySettings.value = _securitySettings.value.copy(blockScreenshots = enabled)
        saveSettings()
        securityService.setBlockScreenshots(enabled)
    }

    fun toggleIncognitoMode(enabled: Boolean) {
        _securitySettings.value = _securitySettings.value.copy(incognitoMode = enabled)
        saveSettings()
    }

    fun showPinSetup() {
        // This would typically navigate to a PIN setup screen
        // For now, we'll just trigger the PIN setup process
        securityService.setupPin()
        checkPinStatus()
    }

    private fun saveSettings() {
        viewModelScope.launch {
            val settings = SecuritySettingsEntity(
                biometricEnabled = _securitySettings.value.biometricEnabled,
                autoLockMinutes = _securitySettings.value.autoLockTime.minutes,
                encryptCredentials = _securitySettings.value.encryptCredentials,
                clearDataOnFail = _securitySettings.value.clearDataOnFail,
                blockScreenshots = _securitySettings.value.blockScreenshots,
                incognitoMode = _securitySettings.value.incognitoMode
            )
            securityRepository.saveSecuritySettings(settings)
        }
    }
}