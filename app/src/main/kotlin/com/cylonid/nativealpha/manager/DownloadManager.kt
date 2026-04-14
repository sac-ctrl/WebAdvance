package com.cylonid.nativealpha.manager

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cylonid.nativealpha.data.AppDatabase
import com.cylonid.nativealpha.model.WebApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import javax.inject.Inject
import javax.inject.Singleton

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val webAppId: Long,
    val url: String,
    val fileName: String,
    val fileSize: Long = 0,
    val downloadedBytes: Long = 0,
    val status: Status = Status.PENDING,
    val filePath: String? = null,
    val mimeType: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val downloadSpeed: Long = 0, // bytes per second
    val etaSeconds: Long = 0, // estimated time remaining
    val errorMessage: String? = null
) {
    enum class Status {
        PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED
    }

    val progressPercentage: Int
        get() = if (fileSize > 0) ((downloadedBytes.toFloat() / fileSize) * 100).toInt() else 0

    val formattedFileSize: String
        get() = formatFileSize(fileSize)

    val formattedDownloadedSize: String
        get() = formatFileSize(downloadedBytes)

    val formattedSpeed: String
        get() = if (downloadSpeed > 0) "${formatFileSize(downloadedBytes)}/s" else ""

    val formattedETA: String
        get() = if (etaSeconds > 0) formatDuration(etaSeconds) else ""

    private fun formatFileSize(bytes: Long): String {
        if (bytes == 0L) return "0 B"
        val k = 1024.0
        val sizes = arrayOf("B", "KB", "MB", "GB", "TB")
        val i = (Math.log(bytes.toDouble()) / Math.log(k)).toInt()
        return DecimalFormat("#,##0.#").format(bytes / Math.pow(k, i.toDouble())) + " " + sizes[i]
    }

    private fun formatDuration(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
}

@Singleton
class DownloadManager @Inject constructor(
    private val context: Context,
    private val database: AppDatabase
) {
    private val systemDownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
    private val dao = database.downloadItemDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun downloadFile(webAppId: Long, url: String, fileName: String? = null, webApp: WebApp? = null): Long {
        val finalFileName = fileName ?: getFileNameFromUrl(url)
        val mimeType = getMimeTypeFromUrl(url)
        val fileTypeFolder = getFileTypeFolder(mimeType)
        val appName = webApp?.name?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "Unknown"

        // Create directory structure: /sdcard/WAOS/{AppName}/{FileType}/
        val baseDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "WAOS")
        val appDir = File(baseDir, appName)
        val typeDir = File(appDir, fileTypeFolder)
        typeDir.mkdirs()

        val request = android.app.DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(finalFileName)
            setDescription("Downloading from ${Uri.parse(url).host}")
            setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(File(typeDir, finalFileName)))
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setMimeType(mimeType)
        }

        val downloadId = systemDownloadManager.enqueue(request)

        // Save to database
        scope.launch {
            val item = DownloadItem(
                webAppId = webAppId,
                url = url,
                fileName = finalFileName,
                mimeType = mimeType,
                filePath = File(typeDir, finalFileName).absolutePath,
                id = downloadId
            )
            dao.insertDownload(item)
        }

        return downloadId
    }

    fun getDownloadsForApp(webAppId: Long): Flow<List<DownloadItem>> {
        return dao.getDownloadsForApp(webAppId).map { downloads ->
            downloads.map { updateDownloadProgress(it) }
        }
    }

    fun getAllDownloads(): Flow<List<DownloadItem>> {
        return dao.getAllDownloads().map { downloads ->
            downloads.map { updateDownloadProgress(it) }
        }
    }

    fun pauseDownload(downloadId: Long) {
        // DownloadManager doesn't support pause/resume directly
        // For now, just cancel and mark as paused
        systemDownloadManager.remove(downloadId)
        scope.launch {
            val item = dao.getDownloadById(downloadId)
            item?.let {
                val updated = it.copy(status = DownloadItem.Status.PAUSED)
                dao.updateDownload(updated)
            }
        }
    }

    fun resumeDownload(download: DownloadItem) {
        // Re-enqueue the download
        downloadFile(download.webAppId, download.url, download.fileName)
    }

    fun cancelDownload(downloadId: Long) {
        systemDownloadManager.remove(downloadId)
        scope.launch {
            dao.deleteDownload(downloadId)
        }
    }

    fun retryDownload(download: DownloadItem) {
        downloadFile(download.webAppId, download.url, download.fileName)
    }

    fun deleteDownloadAndFile(download: DownloadItem) {
        scope.launch {
            // Delete from database
            dao.deleteDownload(download.id)
            // Delete file if exists
            download.filePath?.let { path ->
                File(path).delete()
            }
        }
    }

    private fun updateDownloadProgress(item: DownloadItem): DownloadItem {
        return try {
            val query = android.app.DownloadManager.Query().setFilterById(item.id)
            val cursor = systemDownloadManager.query(query)

            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS))
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                val newStatus = when (status) {
                    android.app.DownloadManager.STATUS_SUCCESSFUL -> DownloadItem.Status.COMPLETED
                    android.app.DownloadManager.STATUS_FAILED -> DownloadItem.Status.FAILED
                    android.app.DownloadManager.STATUS_RUNNING -> DownloadItem.Status.DOWNLOADING
                    android.app.DownloadManager.STATUS_PAUSED -> DownloadItem.Status.PAUSED
                    else -> DownloadItem.Status.PENDING
                }

                item.copy(
                    status = newStatus,
                    downloadedBytes = bytesDownloaded,
                    fileSize = totalBytes
                )
            } else {
                item
            }
        } catch (e: Exception) {
            item
        }
    }

    private fun getFileNameFromUrl(url: String): String {
        return Uri.parse(url).lastPathSegment ?: "download_${System.currentTimeMillis()}"
    }

    private fun getMimeTypeFromUrl(url: String): String? {
        val extension = getFileNameFromUrl(url).substringAfterLast('.', "")
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "avi" -> "video/avi"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "html", "htm" -> "text/html"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "apk" -> "application/vnd.android.package-archive"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            else -> null
        }
    }

    private fun getFileTypeFolder(mimeType: String?): String {
        return when {
            mimeType?.startsWith("image/") == true -> "Images"
            mimeType?.startsWith("video/") == true -> "Videos"
            mimeType?.startsWith("audio/") == true -> "Audio"
            mimeType == "application/pdf" -> "Documents"
            mimeType?.startsWith("text/") == true -> "Documents"
            mimeType == "application/vnd.android.package-archive" -> "APKs"
            mimeType?.contains("document") == true || mimeType?.contains("presentation") == true -> "Documents"
            mimeType?.startsWith("application/") == true -> "Others"
            else -> "Others"
        }
    }
}