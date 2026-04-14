package com.cylonid.nativealpha.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.cylonid.nativealpha.data.Converters

@Entity(tableName = "notifications")
@TypeConverters(Converters::class)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appName: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actions: List<String> = emptyList()
)

@Entity(tableName = "notification_settings")
data class NotificationSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val enabled: Boolean = true,
    val showPreview: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)