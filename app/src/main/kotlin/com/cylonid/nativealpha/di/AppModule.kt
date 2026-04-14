package com.cylonid.nativealpha.di

import android.content.Context
import androidx.room.Room
import com.cylonid.nativealpha.data.*
import com.cylonid.nativealpha.manager.*
import com.cylonid.nativealpha.repository.WebAppRepository
import com.cylonid.nativealpha.service.BackupService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "waos_database"
        ).build()
    }

    @Provides
    fun provideWebAppDao(database: AppDatabase): WebAppDao {
        return database.webAppDao()
    }

    @Provides
    @Singleton
    fun provideWebAppRepository(webAppDao: WebAppDao): WebAppRepository {
        return WebAppRepository(webAppDao)
    }

    @Provides
    fun provideClipboardItemDao(database: AppDatabase): ClipboardItemDao {
        return database.clipboardItemDao()
    }

    @Provides
    @Singleton
    fun provideClipboardManager(clipboardItemDao: ClipboardItemDao): ClipboardManager {
        return ClipboardManager(clipboardItemDao)
    }

    @Provides
    fun provideCredentialDao(database: AppDatabase): CredentialDao {
        return database.credentialDao()
    }

    @Provides
    @Singleton
    fun provideCredentialManager(database: AppDatabase): CredentialManager {
        return CredentialManager(database)
    }

    @Provides
    fun provideDownloadItemDao(database: AppDatabase): DownloadItemDao {
        return database.downloadItemDao()
    }

    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context,
        database: AppDatabase
    ): DownloadManager {
        return DownloadManager(context, database)
    }

    @Provides
    fun provideWindowPresetDao(database: AppDatabase): WindowPresetDao {
        return database.windowPresetDao()
    }

    @Provides
    @Singleton
    fun provideWindowPresetRepository(windowPresetDao: WindowPresetDao): WindowPresetRepository {
        return WindowPresetRepository(windowPresetDao)
    }

    @Provides
    fun provideNotificationDao(database: AppDatabase): NotificationDao {
        return database.notificationDao()
    }

    @Provides
    fun provideNotificationSettingsDao(database: AppDatabase): NotificationSettingsDao {
        return database.notificationSettingsDao()
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        notificationDao: NotificationDao,
        notificationSettingsDao: NotificationSettingsDao
    ): NotificationRepository {
        return NotificationRepository(notificationDao, notificationSettingsDao)
    }

    @Provides
    fun provideSecuritySettingsDao(database: AppDatabase): SecuritySettingsDao {
        return database.securitySettingsDao()
    }

    @Provides
    @Singleton
    fun provideSecurityRepository(securitySettingsDao: SecuritySettingsDao): SecurityRepository {
        return SecurityRepository(securitySettingsDao)
    }

    @Provides
    fun provideBackupDao(database: AppDatabase): BackupDao {
        return database.backupDao()
    }

    @Provides
    @Singleton
    fun provideBackupService(
        @ApplicationContext context: Context,
        database: AppDatabase,
        webAppRepository: WebAppRepository,
        credentialManager: CredentialManager
    ): BackupService {
        return BackupService(context, database, webAppRepository, credentialManager)
    }
}