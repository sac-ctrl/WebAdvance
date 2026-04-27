package com.cylonid.nativealpha.service

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.cylonid.nativealpha.data.AppDatabase
import com.cylonid.nativealpha.manager.ClipboardItem
import com.cylonid.nativealpha.manager.Credential
import com.cylonid.nativealpha.manager.DownloadItem
import com.cylonid.nativealpha.model.NotificationEntity
import com.cylonid.nativealpha.model.NotificationSettingsEntity
import com.cylonid.nativealpha.model.SecuritySettingsEntity
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.model.WindowPresetEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class BackupResult(
    val success: Boolean,
    val message: String,
    val location: String? = null
)

@Singleton
class BackupService @Inject constructor(
    private val context: Context,
    private val database: AppDatabase
) {

    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .create()

    private val internalBackupDir = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }

    /**
     * Build the comprehensive WAOS backup payload as a JSON string.
     * Bundles every Room table plus every SharedPreferences XML file the app owns.
     */
    private suspend fun buildBackupJson(): String = withContext(Dispatchers.IO) {
        val webApps: List<WebApp> = safeCall { database.webAppDao().getAllWebAppsList() } ?: emptyList()
        val clipboardItems: List<ClipboardItem> = safeCall { database.clipboardItemDao().getAllItems() } ?: emptyList()
        val credentials: List<Credential> = safeCall { database.credentialDao().getAllCredentials() } ?: emptyList()
        val downloads: List<DownloadItem> = safeCall { database.downloadItemDao().getAllDownloadsList() } ?: emptyList()
        val windowPresets: List<WindowPresetEntity> = safeCall { database.windowPresetDao().getAllPresetsList() } ?: emptyList()
        val notifications: List<NotificationEntity> = safeCall { database.notificationDao().getAllNotificationsList() } ?: emptyList()
        val notificationSettings: NotificationSettingsEntity? = safeCall { database.notificationSettingsDao().getSettings() }
        val securitySettings: SecuritySettingsEntity? = safeCall { database.securitySettingsDao().getSettings() }

        val preferences: Map<String, Map<String, SerializedPref>> = readAllSharedPreferences()

        val payload = mapOf(
            "format" to "WAOS",
            "version" to BACKUP_VERSION,
            "appName" to "WAOS - Web App Operating System",
            "createdAt" to System.currentTimeMillis(),
            "createdAtReadable" to readableDate(System.currentTimeMillis()),
            "data" to mapOf(
                "webApps" to webApps,
                "clipboardItems" to clipboardItems,
                "credentials" to credentials,
                "downloads" to downloads,
                "windowPresets" to windowPresets,
                "notifications" to notifications,
                "notificationSettings" to notificationSettings,
                "securitySettings" to securitySettings,
                "preferences" to preferences
            )
        )

        gson.toJson(payload)
    }

    /**
     * Write a backup file to the user-selected folder (tree URI from SAF).
     * Returns the readable display path on success.
     */
    suspend fun writeBackupToFolder(treeUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val tree = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext BackupResult(false, "Selected folder is not accessible.")
            if (!tree.canWrite()) {
                return@withContext BackupResult(false, "Cannot write to the selected folder.")
            }

            val fileName = "WAOS_Backup_${fileTimestamp()}.waos"
            // Remove any pre-existing file with the same name (rare).
            tree.findFile(fileName)?.delete()

            val file = tree.createFile("application/octet-stream", fileName)
                ?: return@withContext BackupResult(false, "Failed to create backup file in selected folder.")

            val json = buildBackupJson()
            context.contentResolver.openOutputStream(file.uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return@withContext BackupResult(false, "Failed to open output stream for backup file.")

            // Also drop a local copy for the in-app backup history.
            try {
                val internalCopy = File(internalBackupDir, fileName)
                internalCopy.writeBytes(json.toByteArray(Charsets.UTF_8))
            } catch (_: Exception) { /* non-fatal */ }

            BackupResult(
                success = true,
                message = "Backup saved as $fileName",
                location = file.uri.toString()
            )
        } catch (e: Exception) {
            BackupResult(false, "Backup failed: ${e.message ?: "unknown error"}")
        }
    }

    /**
     * Restore from a user-picked .waos file (file URI from SAF).
     */
    suspend fun restoreFromUri(fileUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                ?: return@withContext BackupResult(false, "Could not open the selected backup file.")

            val json = bytes.toString(Charsets.UTF_8).trim()
            if (json.isEmpty()) {
                return@withContext BackupResult(false, "The selected file is empty.")
            }

            val root = try {
                gson.fromJson(json, JsonObject::class.java)
            } catch (e: Exception) {
                return@withContext BackupResult(false, "The selected file is not a valid WAOS backup.")
            }

            val format = root.get("format")?.asString
            if (format != null && format != "WAOS" && format != "waos") {
                return@withContext BackupResult(false, "Unsupported backup format: $format")
            }

            // Support legacy v1/v2 backups where webApps lived at the top level.
            val data: JsonObject = if (root.has("data") && root.get("data").isJsonObject) {
                root.getAsJsonObject("data")
            } else {
                root
            }

            applyBackupData(data)
            BackupResult(true, "Restore complete. Restart the app to see all changes.")
        } catch (e: Exception) {
            BackupResult(false, "Restore failed: ${e.message ?: "unknown error"}")
        }
    }

    /**
     * Legacy in-app backup (no folder picker). Used by the older backup history list.
     */
    suspend fun createBackup(): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "WAOS_Backup_${fileTimestamp()}.waos"
            val file = File(internalBackupDir, fileName)
            file.writeBytes(buildBackupJson().toByteArray(Charsets.UTF_8))
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    suspend fun restoreBackup(backupPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(backupPath)
            if (!file.exists()) return@withContext false
            val uri = Uri.fromFile(file)
            restoreFromUri(uri).success
        } catch (e: Exception) {
            false
        }
    }

    fun deleteBackupFile(path: String) {
        try { File(path).delete() } catch (_: Exception) {}
    }

    // ------------------------------------------------------------------------------------------
    // Restore implementation
    // ------------------------------------------------------------------------------------------

    private suspend fun applyBackupData(data: JsonObject) {
        // WebApps
        data.getAsJsonArray("webApps")?.let { arr ->
            val type = object : TypeToken<List<WebApp>>() {}.type
            val list: List<WebApp> = try { gson.fromJson(arr, type) } catch (_: Exception) { emptyList() }
            if (list.isNotEmpty()) {
                runCatching { database.webAppDao().clearAll() }
                list.forEach { app ->
                    runCatching { database.webAppDao().insertWebApp(app.copy(thumbnail = null)) }
                }
            }
        }

        // Clipboard
        data.getAsJsonArray("clipboardItems")?.let { arr ->
            val type = object : TypeToken<List<ClipboardItem>>() {}.type
            val list: List<ClipboardItem> = try { gson.fromJson(arr, type) } catch (_: Exception) { emptyList() }
            runCatching { database.clipboardItemDao().clearAll() }
            list.forEach { item ->
                runCatching { database.clipboardItemDao().insertItem(item.copy(id = 0)) }
            }
        }

        // Credentials (vault)
        data.getAsJsonArray("credentials")?.let { arr ->
            val type = object : TypeToken<List<Credential>>() {}.type
            val list: List<Credential> = try { gson.fromJson(arr, type) } catch (_: Exception) { emptyList() }
            runCatching { database.credentialDao().clearAll() }
            list.forEach { cred ->
                runCatching { database.credentialDao().insertCredential(cred.copy(id = 0)) }
            }
        }

        // Downloads
        data.getAsJsonArray("downloads")?.let { arr ->
            val type = object : TypeToken<List<DownloadItem>>() {}.type
            val list: List<DownloadItem> = try { gson.fromJson(arr, type) } catch (_: Exception) { emptyList() }
            runCatching { database.downloadItemDao().clearAll() }
            list.forEach { d ->
                runCatching { database.downloadItemDao().insertDownload(d.copy(id = 0)) }
            }
        }

        // Window presets
        data.getAsJsonArray("windowPresets")?.let { arr ->
            val type = object : TypeToken<List<WindowPresetEntity>>() {}.type
            val list: List<WindowPresetEntity> = try { gson.fromJson(arr, type) } catch (_: Exception) { emptyList() }
            runCatching { database.windowPresetDao().clearAll() }
            list.forEach { p ->
                runCatching { database.windowPresetDao().insertPreset(p.copy(id = 0)) }
            }
        }

        // Notifications
        data.getAsJsonArray("notifications")?.let { arr ->
            val type = object : TypeToken<List<NotificationEntity>>() {}.type
            val list: List<NotificationEntity> = try { gson.fromJson(arr, type) } catch (_: Exception) { emptyList() }
            runCatching { database.notificationDao().clearAllNotifications() }
            list.forEach { n ->
                runCatching { database.notificationDao().insertNotification(n.copy(id = 0)) }
            }
        }

        // Notification settings (single row)
        data.get("notificationSettings")?.takeIf { it.isJsonObject }?.let { el ->
            try {
                val s = gson.fromJson(el, NotificationSettingsEntity::class.java)
                if (s != null) database.notificationSettingsDao().insertSettings(s)
            } catch (_: Exception) {}
        }

        // Security settings (single row)
        data.get("securitySettings")?.takeIf { it.isJsonObject }?.let { el ->
            try {
                val s = gson.fromJson(el, SecuritySettingsEntity::class.java)
                if (s != null) database.securitySettingsDao().insertSettings(s)
            } catch (_: Exception) {}
        }

        // SharedPreferences (every named pref file)
        data.get("preferences")?.takeIf { it.isJsonObject }?.asJsonObject?.let { prefsObj ->
            for ((prefName, prefValuesEl) in prefsObj.entrySet()) {
                if (!prefValuesEl.isJsonObject) continue
                writeSharedPreferences(prefName, prefValuesEl.asJsonObject)
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // SharedPreferences helpers
    // ------------------------------------------------------------------------------------------

    private fun readAllSharedPreferences(): Map<String, Map<String, SerializedPref>> {
        val result = mutableMapOf<String, Map<String, SerializedPref>>()
        try {
            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (!prefsDir.exists() || !prefsDir.isDirectory) return result
            val files = prefsDir.listFiles { _, name -> name.endsWith(".xml") } ?: return result
            for (file in files) {
                val prefName = file.nameWithoutExtension
                val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                val all = prefs.all ?: continue
                val serialized = mutableMapOf<String, SerializedPref>()
                for ((k, v) in all) {
                    serialized[k] = serializePrefValue(v) ?: continue
                }
                if (serialized.isNotEmpty()) result[prefName] = serialized
            }
        } catch (_: Exception) {}
        return result
    }

    private fun serializePrefValue(value: Any?): SerializedPref? {
        return when (value) {
            null -> SerializedPref("NULL", null)
            is Boolean -> SerializedPref("BOOLEAN", value)
            is Int -> SerializedPref("INT", value)
            is Long -> SerializedPref("LONG", value)
            is Float -> SerializedPref("FLOAT", value)
            is String -> SerializedPref("STRING", value)
            is Set<*> -> {
                val strings = value.mapNotNull { it as? String }
                SerializedPref("STRING_SET", strings)
            }
            else -> SerializedPref("STRING", value.toString())
        }
    }

    private fun writeSharedPreferences(name: String, values: JsonObject) {
        val prefs: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.clear()
        for ((key, valEl) in values.entrySet()) {
            if (!valEl.isJsonObject) continue
            val obj = valEl.asJsonObject
            val type = obj.get("type")?.asString ?: continue
            val raw = obj.get("value")
            try {
                when (type) {
                    "BOOLEAN" -> editor.putBoolean(key, raw?.asBoolean == true)
                    "INT" -> editor.putInt(key, raw?.asInt ?: 0)
                    "LONG" -> editor.putLong(key, raw?.asLong ?: 0L)
                    "FLOAT" -> editor.putFloat(key, raw?.asFloat ?: 0f)
                    "STRING" -> editor.putString(key, raw?.takeIf { !it.isJsonNull }?.asString)
                    "STRING_SET" -> {
                        val arr = raw?.takeIf { it.isJsonArray }?.asJsonArray
                        val set = arr?.mapNotNull { it.asString }?.toSet() ?: emptySet()
                        editor.putStringSet(key, set)
                    }
                    "NULL" -> { /* skip */ }
                    else -> { /* unknown */ }
                }
            } catch (_: Exception) { /* skip bad entries */ }
        }
        editor.apply()
    }

    // ------------------------------------------------------------------------------------------
    // Misc helpers
    // ------------------------------------------------------------------------------------------

    private fun fileTimestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun readableDate(epochMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(epochMs))

    private inline fun <T> safeCall(block: () -> T): T? =
        try { block() } catch (_: Exception) { null }

    data class SerializedPref(val type: String, val value: Any?)

    companion object {
        const val BACKUP_VERSION = 3
    }
}
