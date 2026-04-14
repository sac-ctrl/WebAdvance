package com.cylonid.nativealpha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.data.NotificationRepository
import com.cylonid.nativealpha.model.NotificationEntity
import com.cylonid.nativealpha.model.NotificationSettingsEntity
import com.cylonid.nativealpha.service.NotificationService
import com.cylonid.nativealpha.ui.screens.NotificationSettings
import com.cylonid.nativealpha.ui.screens.WebAppNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val notificationService: NotificationService
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<WebAppNotification>>(emptyList())
    val notifications: StateFlow<List<WebAppNotification>> = _notifications.asStateFlow()

    private val _notificationSettings = MutableStateFlow(NotificationSettings())
    val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    init {
        loadNotifications()
        loadSettings()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            notificationRepository.getRecentNotifications().collect { notificationEntities ->
                _notifications.value = notificationEntities.map { entity ->
                    WebAppNotification(
                        id = entity.id,
                        appName = entity.appName,
                        title = entity.title,
                        message = entity.message,
                        timestamp = dateFormat.format(Date(entity.timestamp)),
                        actions = entity.actions
                    )
                }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = notificationRepository.getNotificationSettings()
            _notificationSettings.value = NotificationSettings(
                enabled = settings.enabled,
                showPreview = settings.showPreview,
                soundEnabled = settings.soundEnabled,
                vibrationEnabled = settings.vibrationEnabled
            )
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationSettings.value = _notificationSettings.value.copy(enabled = enabled)
        saveSettings()
        notificationService.setNotificationsEnabled(enabled)
    }

    fun togglePreview(showPreview: Boolean) {
        _notificationSettings.value = _notificationSettings.value.copy(showPreview = showPreview)
        saveSettings()
    }

    fun toggleSound(soundEnabled: Boolean) {
        _notificationSettings.value = _notificationSettings.value.copy(soundEnabled = soundEnabled)
        saveSettings()
    }

    fun toggleVibration(vibrationEnabled: Boolean) {
        _notificationSettings.value = _notificationSettings.value.copy(vibrationEnabled = vibrationEnabled)
        saveSettings()
    }

    private fun saveSettings() {
        viewModelScope.launch {
            val settings = NotificationSettingsEntity(
                enabled = _notificationSettings.value.enabled,
                showPreview = _notificationSettings.value.showPreview,
                soundEnabled = _notificationSettings.value.soundEnabled,
                vibrationEnabled = _notificationSettings.value.vibrationEnabled
            )
            notificationRepository.saveNotificationSettings(settings)
        }
    }

    fun dismissNotification(notificationId: Long) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
            loadNotifications()
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationRepository.clearAllNotifications()
            _notifications.value = emptyList()
        }
    }

    fun performAction(notification: WebAppNotification, action: String) {
        // Handle notification actions (e.g., open web app, perform action)
        notificationService.performNotificationAction(notification.id, action)
    }
}