package com.cylonid.nativealpha.data

import androidx.room.*
import com.cylonid.nativealpha.model.NotificationEntity
import com.cylonid.nativealpha.model.NotificationSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT 50")
    fun getRecentNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
}

@Dao
interface NotificationSettingsDao {

    @Query("SELECT * FROM notification_settings LIMIT 1")
    suspend fun getSettings(): NotificationSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: NotificationSettingsEntity)

    @Update
    suspend fun updateSettings(settings: NotificationSettingsEntity)
}