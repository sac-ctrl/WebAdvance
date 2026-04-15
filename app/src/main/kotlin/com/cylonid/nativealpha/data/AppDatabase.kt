package com.cylonid.nativealpha.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cylonid.nativealpha.manager.ClipboardItem
import com.cylonid.nativealpha.manager.Credential
import com.cylonid.nativealpha.manager.DownloadItem
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.model.WindowPresetEntity
import com.cylonid.nativealpha.model.WindowEntity
import com.cylonid.nativealpha.model.NotificationEntity
import com.cylonid.nativealpha.model.NotificationSettingsEntity
import com.cylonid.nativealpha.model.SecuritySettingsEntity
import com.cylonid.nativealpha.model.BackupEntity

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE webapps ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [WebApp::class, ClipboardItem::class, Credential::class, DownloadItem::class, WindowPresetEntity::class, WindowEntity::class, NotificationEntity::class, NotificationSettingsEntity::class, SecuritySettingsEntity::class, BackupEntity::class],
    version = 6,
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
