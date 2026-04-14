package com.cylonid.nativealpha.service

import android.content.Context
import com.cylonid.nativealpha.data.AppDatabase
import com.cylonid.nativealpha.manager.CredentialManager
import com.cylonid.nativealpha.repository.WebAppRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupService @Inject constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val webAppRepository: WebAppRepository,
    private val credentialManager: CredentialManager
) {

    private val gson = Gson()
    private val backupDir = File(context.getExternalFilesDir(null), "backups")

    init {
        backupDir.mkdirs()
    }

    suspend fun createBackup(): String? = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "backup_$timestamp.json")

            val webApps = webAppRepository.getAllWebApps().first()
            val settings = mapOf(
                "version" to 1,
                "timestamp" to timestamp
            )

            val backupData = mapOf(
                "settings" to settings,
                "webApps" to webApps
            )

            val json = gson.toJson(backupData)
            FileOutputStream(backupFile).use { it.write(json.toByteArray()) }

            backupFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    suspend fun restoreBackup(backupPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) return@withContext false
            val json = FileInputStream(backupFile).use { it.readBytes().toString(Charsets.UTF_8) }
            val backupData = gson.fromJson(json, Map::class.java)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteBackupFile(path: String) {
        File(path).delete()
    }

    suspend fun exportData() {
        createBackup()
    }

    suspend fun importData() {
        // Handled via file picker in UI
    }
}