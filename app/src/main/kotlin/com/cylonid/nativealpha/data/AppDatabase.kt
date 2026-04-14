package com.cylonid.nativealpha.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cylonid.nativealpha.manager.ClipboardItem
import com.cylonid.nativealpha.manager.Credential
import com.cylonid.nativealpha.manager.DownloadItem
import com.cylonid.nativealpha.model.WindowPresetEntity
import com.cylonid.nativealpha.model.WindowEntity
import com.cylonid.nativealpha.model.NotificationEntity
import com.cylonid.nativealpha.model.NotificationSettingsEntity
import com.cylonid.nativealpha.model.SecuritySettingsEntity
import com.cylonid.nativealpha.model.BackupEntity

@Database(
    entities = [WebApp::class, ClipboardItem::class, Credential::class, DownloadItem::class, WindowPresetEntity::class, WindowEntity::class, NotificationEntity::class, NotificationSettingsEntity::class, SecuritySettingsEntity::class, BackupEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun webAppDao(): WebAppDao
    abstract fun clipboardItemDao(): ClipboardItemDao
    abstract fun credentialDao(): CredentialDao
    abstract fun downloadItemDao(): DownloadItemDao
    abstract fun windowPresetDao(): WindowPresetDao
    abstract fun notificationDao(): NotificationDao
    abstract fun notificationSettingsDao(): NotificationSettingsDao
    abstract fun securitySettingsDao(): SecuritySettingsDao
    abstract fun backupDao(): BackupDao
}