package com.cylonid.nativealpha.service

import android.content.Context
import com.cylonid.nativealpha.data.AppDatabase
import com.cylonid.nativealpha.manager.CredentialManager
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.repository.WebAppRepository
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
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

    private val gson = GsonBuilder()
        .serializeNulls()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .create()
    private val backupDir = File(context.getExternalFilesDir(null), "backups")

    init {
        backupDir.mkdirs()
    }

    suspend fun createBackup(): String? = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "waos_backup_$timestamp.waos")

            val webApps = webAppRepository.getAllWebApps().first()
            val settings = mapOf(
                "version" to 2,
                "timestamp" to timestamp,
                "format" to "waos",
                "appName" to "WAOS - Web App Operating System"
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

            val root = gson.fromJson(json, com.google.gson.JsonObject::class.java)
            val webAppsJson = root.getAsJsonArray("webApps") ?: return@withContext false

            val webAppsType = object : TypeToken<List<WebApp>>() {}.type
            val webApps: List<WebApp> = try {
                gson.fromJson(webAppsJson, webAppsType)
            } catch (e: Exception) {
                emptyList()
            }

            webApps.forEach { app ->
                try {
                    // Reset id=0 so Room auto-generates a new primary key on insert
                    webAppRepository.insertWebApp(app.copy(id = 0, thumbnail = null))
                } catch (e: Exception) {
                    // Skip apps that fail to insert individually
                }
            }

            webApps.isNotEmpty()
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