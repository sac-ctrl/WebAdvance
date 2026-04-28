package com.cylonid.nativealpha.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
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

    /**
     * Chrome-parity download entry point. Accepts every signal the browser
     * exposes (Content-Disposition, mime, UA, referer) and forwards the
     * WebView's cookie jar so authenticated downloads (Drive, intranet, etc.)
     * succeed exactly like in Chrome. Routes data: and blob: URLs to local
     * decoders since the system DownloadManager can't fetch them.
     */
    fun downloadFile(
        webAppId: Long,
        url: String,
        fileName: String? = null,
        webApp: WebApp? = null,
        userAgent: String? = null,
        contentDisposition: String? = null,
        explicitMimeType: String? = null,
        referer: String? = null
    ): Long {
        // data: URLs cannot be fetched by the system DownloadManager — decode
        // and write the payload locally.
        if (url.startsWith("data:", ignoreCase = true)) {
            return saveDataUrl(webAppId, url, fileName, contentDisposition, webApp)
        }

        // Chrome filename resolution: explicit → Content-Disposition + URLUtil
        // → URL path → generated.
        val resolvedName = fileName?.takeIf { it.isNotBlank() }
            ?: try {
                URLUtil.guessFileName(url, contentDisposition, explicitMimeType)
            } catch (_: Exception) { null }
            ?: getFileNameFromUrl(url)

        val mimeType = explicitMimeType
            ?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
            ?: getMimeTypeFromUrl(resolvedName)

        val fileTypeFolder = getFileTypeFolder(mimeType)
        val appName = webApp?.name?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "Unknown"

        // App-private external storage:
        // /Android/data/<pkg>/files/WAOS/{AppName}/{FileType}/ — invisible to
        // Gallery / file manager, but the app has full access without
        // storage permissions.
        val baseDir = File(context.getExternalFilesDir(null), "WAOS")
        val typeDir = File(File(baseDir, appName), fileTypeFolder).apply { mkdirs() }
        val destFile = uniqueFile(typeDir, resolvedName)

        val request = android.app.DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(destFile.name)
            setDescription("Downloading from ${Uri.parse(url).host ?: "the web"}")
            setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(destFile))
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            if (mimeType != null) setMimeType(mimeType)

            // Forward the WebView's cookies so logged-in downloads work
            // exactly like Chrome (Drive, intranet, paywalled CDNs, etc).
            try {
                val cookie = CookieManager.getInstance().getCookie(url)
                if (!cookie.isNullOrBlank()) addRequestHeader("Cookie", cookie)
            } catch (_: Exception) {}
            if (!userAgent.isNullOrBlank()) addRequestHeader("User-Agent", userAgent)
            if (!referer.isNullOrBlank()) addRequestHeader("Referer", referer)
        }

        val downloadId = try {
            systemDownloadManager.enqueue(request)
        } catch (e: Exception) {
            Log.e("DownloadManager", "enqueue failed for $url: ${e.message}")
            return -1L
        }

        scope.launch {
            val item = DownloadItem(
                webAppId = webAppId,
                url = url,
                fileName = destFile.name,
                mimeType = mimeType,
                filePath = destFile.absolutePath,
                status = DownloadItem.Status.DOWNLOADING,
                id = downloadId
            )
            dao.insertDownload(item)
        }

        ensureDownloadReceiverRegistered()
        return downloadId
    }

    /**
     * Decode and save a `data:` URL (e.g. `data:image/png;base64,…` saved from
     * a long-press image menu on a generated canvas / inline SVG).
     */
    private fun saveDataUrl(
        webAppId: Long,
        url: String,
        fileName: String?,
        contentDisposition: String?,
        webApp: WebApp?
    ): Long {
        return try {
            val comma = url.indexOf(',')
            if (comma < 0) return -1L
            val header = url.substring(5, comma) // strip "data:"
            val payload = url.substring(comma + 1)
            val isBase64 = header.contains(";base64", ignoreCase = true)
            val mime = header.substringBefore(';').ifBlank { "application/octet-stream" }
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
            val resolved = fileName?.takeIf { it.isNotBlank() }
                ?: try { URLUtil.guessFileName(url, contentDisposition, mime) } catch (_: Exception) { null }
                ?: "download_${System.currentTimeMillis()}.$ext"

            val appName = webApp?.name?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "Unknown"
            val baseDir = File(context.getExternalFilesDir(null), "WAOS")
            val typeDir = File(File(baseDir, appName), getFileTypeFolder(mime)).apply { mkdirs() }
            val destFile = uniqueFile(typeDir, resolved)

            val bytes = if (isBase64) {
                android.util.Base64.decode(payload, android.util.Base64.DEFAULT)
            } else {
                Uri.decode(payload).toByteArray(Charsets.UTF_8)
            }
            destFile.writeBytes(bytes)
            // Intentionally not calling MediaScannerConnection — the file lives
            // in app-private storage and must NOT be exposed to Gallery /
            // device file manager.

            val id = System.currentTimeMillis()
            scope.launch {
                dao.insertDownload(
                    DownloadItem(
                        webAppId = webAppId,
                        url = "data:$mime",
                        fileName = destFile.name,
                        fileSize = bytes.size.toLong(),
                        downloadedBytes = bytes.size.toLong(),
                        status = DownloadItem.Status.COMPLETED,
                        filePath = destFile.absolutePath,
                        mimeType = mime,
                        id = id
                    )
                )
            }
            id
        } catch (e: Exception) {
            Log.e("DownloadManager", "data: URL decode failed: ${e.message}")
            -1L
        }
    }

    /** Append " (1)", " (2)" etc. so we never overwrite an existing file. */
    private fun uniqueFile(dir: File, fileName: String): File {
        val target = File(dir, fileName)
        if (!target.exists()) return target
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var i = 1
        while (true) {
            val candidate = File(dir, "$base ($i)$ext")
            if (!candidate.exists()) return candidate
            i++
        }
    }

    @Volatile private var receiverRegistered = false

    private fun ensureDownloadReceiverRegistered() {
        if (receiverRegistered) return
        synchronized(this) {
            if (receiverRegistered) return
            try {
                val filter = IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                if (Build.VERSION.SDK_INT >= 33) {
                    context.registerReceiver(
                        completionReceiver, filter, Context.RECEIVER_EXPORTED
                    )
                } else {
                    context.registerReceiver(completionReceiver, filter)
                }
                receiverRegistered = true
            } catch (e: Exception) {
                Log.w("DownloadManager", "register receiver failed: ${e.message}")
            }
        }
    }

    private val completionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
            if (id <= 0L) return
            scope.launch {
                val item = dao.getDownloadById(id) ?: return@launch
                try {
                    val cursor = systemDownloadManager.query(
                        android.app.DownloadManager.Query().setFilterById(id)
                    )
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val sysStatus = c.getInt(c.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS))
                            val total = c.getLong(c.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            val done = c.getLong(c.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val reason = try {
                                c.getInt(c.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_REASON))
                            } catch (_: Exception) { 0 }
                            val newStatus = when (sysStatus) {
                                android.app.DownloadManager.STATUS_SUCCESSFUL -> DownloadItem.Status.COMPLETED
                                android.app.DownloadManager.STATUS_FAILED -> DownloadItem.Status.FAILED
                                else -> item.status
                            }
                            val updated = item.copy(
                                status = newStatus,
                                downloadedBytes = done,
                                fileSize = if (total > 0) total else done,
                                errorMessage = if (newStatus == DownloadItem.Status.FAILED) "Reason $reason" else null
                            )
                            dao.updateDownload(updated)
                            // No MediaScannerConnection call — files live in
                            // app-private storage and must not appear in
                            // Gallery / device file manager.
                        }
                    }
                } catch (e: Exception) {
                    Log.w("DownloadManager", "completion update failed: ${e.message}")
                }
            }
        }
    }

    fun saveBlobFile(
        webAppId: Long,
        filename: String,
        base64Data: String,
        explicitMimeType: String? = null,
        webApp: WebApp? = null
    ): Long {
        val mimeType = explicitMimeType?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
            ?: getMimeTypeFromUrl(filename)
        val fileTypeFolder = getFileTypeFolder(mimeType)
        val appName = webApp?.name?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "Unknown"

        // If JS gave us no name, derive a sensible one from the mime type.
        val resolvedName = filename.takeIf { it.isNotBlank() }
            ?: run {
                val ext = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "bin"
                "download_${System.currentTimeMillis()}.$ext"
            }

        val baseDir = File(context.getExternalFilesDir(null), "WAOS")
        val typeDir = File(File(baseDir, appName), fileTypeFolder).apply { mkdirs() }
        val file = uniqueFile(typeDir, resolvedName)
        val id = System.currentTimeMillis()

        scope.launch {
            try {
                val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                file.writeBytes(bytes)
                // App-private storage: do not expose to Gallery / file manager.
                dao.insertDownload(
                    DownloadItem(
                        webAppId = webAppId,
                        url = "blob:data",
                        fileName = file.name,
                        fileSize = bytes.size.toLong(),
                        downloadedBytes = bytes.size.toLong(),
                        status = DownloadItem.Status.COMPLETED,
                        mimeType = mimeType,
                        filePath = file.absolutePath,
                        id = id
                    )
                )
            } catch (e: Exception) {
                Log.e("DownloadManager", "blob save failed: ${e.message}")
            }
        }
        return id
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

    /**
     * Register an already-saved local file (e.g. session export, screenshot) in
     * the in-app download history so it appears like a normal download.
     */
    fun registerLocalFile(
        webAppId: Long,
        filePath: String,
        fileName: String,
        mimeType: String? = null
    ) {
        scope.launch {
            try {
                val file = File(filePath)
                val item = DownloadItem(
                    webAppId = webAppId,
                    url = "local:${file.name}",
                    fileName = fileName,
                    fileSize = file.length(),
                    downloadedBytes = file.length(),
                    status = DownloadItem.Status.COMPLETED,
                    filePath = file.absolutePath,
                    mimeType = mimeType,
                    timestamp = System.currentTimeMillis(),
                    id = System.currentTimeMillis()
                )
                dao.insertDownload(item)
                // App-private storage: intentionally not media-scanned, so the
                // file does NOT show up in Gallery / device file manager.
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveScreenshot(fileName: String, bitmap: android.graphics.Bitmap) {
        scope.launch {
            try {
                val baseDir = File(context.getExternalFilesDir(null), "WAOS")
                val screenshotsDir = File(baseDir, "Screenshots")
                screenshotsDir.mkdirs()

                val file = File(screenshotsDir, fileName)
                val stream = java.io.FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                // App-private storage: do NOT media-scan; screenshots stay
                // hidden from Gallery and device file manager.

                // Optionally save to database as download
                val item = DownloadItem(
                    webAppId = 0L, // Screenshots not tied to specific app
                    url = "",
                    fileName = fileName,
                    fileSize = file.length(),
                    downloadedBytes = file.length(),
                    status = DownloadItem.Status.COMPLETED,
                    filePath = file.absolutePath,
                    mimeType = "image/png",
                    timestamp = System.currentTimeMillis()
                )
                dao.insertDownload(item)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}