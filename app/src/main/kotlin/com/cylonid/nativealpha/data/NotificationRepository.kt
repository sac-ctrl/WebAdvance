package com.cylonid.nativealpha.data

import com.cylonid.nativealpha.model.NotificationEntity
import com.cylonid.nativealpha.model.NotificationSettingsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao,
    private val notificationSettingsDao: NotificationSettingsDao
) {

    fun getRecentNotifications(): Flow<List<NotificationEntity>> {
        return notificationDao.getRecentNotifications()
    }

    suspend fun saveNotification(notification: NotificationEntity) {
        notificationDao.insertNotification(notification)
    }

    suspend fun deleteNotification(id: Long) {
        notificationDao.deleteNotification(id)
    }

    suspend fun clearAllNotifications() {
        notificationDao.clearAllNotifications()
    }

    suspend fun getNotificationSettings(): NotificationSettingsEntity {
        return notificationSettingsDao.getSettings() ?: NotificationSettingsEntity()
    }

    suspend fun saveNotificationSettings(settings: NotificationSettingsEntity) {
        notificationSettingsDao.insertSettings(settings)
    }
}