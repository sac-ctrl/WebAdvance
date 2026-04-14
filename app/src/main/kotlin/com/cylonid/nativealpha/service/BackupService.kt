package com.cylonid.nativealpha.service

import android.content.Context
import com.cylonid.nativealpha.data.AppDatabase
import com.cylonid.nativealpha.manager.CredentialManager
import com.cylonid.nativealpha.repository.WebAppRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
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

            // Collect all data
            val webApps = webAppRepository.getAllWebApps().first()
            val credentials = credentialManager.getAllCredentials()
            val settings = mapOf(
                "version" to 1,
                "timestamp" to timestamp
            )

            val backupData = mapOf(
                "settings" to settings,
                "webApps" to webApps,
                "credentials" to credentials
            )

            val json = gson.toJson(backupData)
            val encryptedJson = encrypt(json, "backup_key") // Simple encryption

            FileOutputStream(backupFile).use { it.write(encryptedJson.toByteArray()) }

            backupFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    suspend fun restoreBackup(backupPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(backupPath)
            val encryptedJson = FileInputStream(backupFile).use { it.readBytes().toString(Charsets.UTF_8) }
            val json = decrypt(encryptedJson, "backup_key")

            val backupData = gson.fromJson(json, Map::class.java)
            
            // Restore data (simplified)
            // In real implementation, would need to handle conflicts, validation, etc.
            
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
        // Would show file picker and restore
    }

    private fun encrypt(data: String, key: String): String {
        val cipher = Cipher.getInstance("AES")
        val keySpec = SecretKeySpec(key.toByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(data.toByteArray())
        return android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
    }

    private fun decrypt(data: String, key: String): String {
        val cipher = Cipher.getInstance("AES")
        val keySpec = SecretKeySpec(key.toByteArray(), "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        val decrypted = cipher.doFinal(android.util.Base64.decode(data, android.util.Base64.DEFAULT))
        return String(decrypted)
    }
}