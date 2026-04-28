package com.cylonid.nativealpha.downloads

import android.app.DownloadManager
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import com.cylonid.nativealpha.waos.model.DownloadRecord
import com.cylonid.nativealpha.waos.model.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced download manager that tracks downloads and integrates with system DownloadManager
 */
class EnhancedDownloadManager(
    private val context: Context,
    private val appId: Long,
    private val appName: String
) {
    
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val appDownloadDir = getAppDownloadDirectory()
    
    private val _downloads = MutableStateFlow<List<DownloadRecord>>(emptyList())
    val downloads: StateFlow<List<DownloadRecord>> = _downloads.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Map<Long, Int>>(emptyMap()) // downloadId -> progress
    val activeDownloads: StateFlow<Map<Long, Int>> = _activeDownloads.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                updateDownloadStatus(downloadId)
            }
        }
    }

    init {
        loadDownloads()
        registerReceiver()
    }

    private fun getAppDownloadDirectory(): File {
        // App-private storage; matches the WAOS root used everywhere else so
        // every file sits in the same predictable place.
        val dir = File(context.getExternalFilesDir(null), "WAOS/$appName")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Start a download
     */
    fun downloadFile(url: String, filename: String = "", mimeType: String = ""): Long {
        val finalFilename = filename.ifEmpty { extractFilenameFromUrl(url) }
        
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setMimeType(mimeType)
            setTitle(finalFilename)
            setDescription("Downloading to $appName")
            setDestinationUri(Uri.fromFile(File(appDownloadDir, finalFilename)))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 11)")
        }

        val downloadId = downloadManager.enqueue(request)
        
        // Record in our database
        val record = DownloadRecord(
            appId = appId.toInt(),
            fileName = finalFilename,
            mimeType = mimeType,
            sizeBytes = 0L,
            timestamp = System.currentTimeMillis(),
            status = "downloading",
            uriPath = File(appDownloadDir, finalFilename).absolutePath
        )
        DownloadRepository.saveDownload(context, record)
        
        return downloadId
    }

    /**
     * Get download progress
     */
    fun getDownloadProgress(downloadId: Long): Int {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        
        return try {
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                val downloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                
                if (total > 0) {
                    ((downloaded * 100L) / total).toInt()
                } else 0
            } else 0
        } finally {
            cursor.close()
        }
    }

    /**
     * Pause download (if supported)
     */
    fun pauseDownload(downloadId: Long) {
        // Not directly supported by Android DownloadManager
        // Would need to implement custom download handling
    }

    /**
     * Resume download (if supported)
     */
    fun resumeDownload(downloadId: Long) {
        // Not directly supported by Android DownloadManager
    }

    /**
     * Cancel download
     */
    fun cancelDownload(downloadId: Long) {
        downloadManager.remove(downloadId)
    }

    /**
     * Delete downloaded file
     */
    fun deleteDownload(record: DownloadRecord) {
        try {
            val file = File(record.uriPath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        DownloadRepository.deleteDownload(context, record)
        loadDownloads()
    }

    /**
     * Open downloaded file
     */
    fun openDownload(record: DownloadRecord) {
        try {
            val file = File(record.uriPath)
            if (file.exists()) {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, record.mimeType)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Share downloaded file
     */
    fun shareDownload(record: DownloadRecord) {
        try {
            val file = File(record.uriPath)
            if (file.exists()) {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = record.mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(intent, "Share file"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get storage usage
     */
    fun getStorageUsage(): Long {
        return appDownloadDir.walkTopDown().sumOf { it.length() }
    }

    /**
     * Get download statistics
     */
    fun getStatistics(): DownloadStatistics {
        val downloads = _downloads.value
        return DownloadStatistics(
            totalDownloads = downloads.size,
            totalSize = downloads.sumOf { it.sizeBytes },
            successCount = downloads.count { it.status == "completed" },
            failedCount = downloads.count { it.status == "failed" },
            pendingCount = downloads.count { it.status == "downloading" }
        )
    }

    private fun loadDownloads() {
        val records = DownloadRepository.loadDownloads(context)
            .filter { it.appId.toLong() == appId }
            .sortedByDescending { it.timestamp }
        _downloads.value = records
    }

    private fun updateDownloadStatus(downloadId: Long) {
        try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                val downloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
                
                val statusString = when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> "completed"
                    DownloadManager.STATUS_FAILED -> "failed"
                    DownloadManager.STATUS_RUNNING -> "downloading"
                    DownloadManager.STATUS_PAUSED -> "paused"
                    else -> "unknown"
                }
                
                val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                
                // Update in database
                val records = DownloadRepository.loadDownloads(context).toMutableList()
                val record = records.find { it.status == "downloading" && it.fileName == title }
                if (record != null) {
                    val index = records.indexOf(record)
                    records[index] = record.copy(
                        status = statusString,
                        sizeBytes = total
                    )
                    DownloadRepository.saveDownloads(context, records)
                }
            }
            cursor.close()
            loadDownloads()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerReceiver() {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        context.registerReceiver(receiver, filter)
    }

    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractFilenameFromUrl(url: String): String {
        val uri = Uri.parse(url)
        val filename = uri.lastPathSegment ?: "download_${System.currentTimeMillis()}"
        return filename.split("?").first().ifEmpty { "download_${System.currentTimeMillis()}" }
    }

    data class DownloadStatistics(
        val totalDownloads: Int,
        val totalSize: Long,
        val successCount: Int,
        val failedCount: Int,
        val pendingCount: Int
    )
}
