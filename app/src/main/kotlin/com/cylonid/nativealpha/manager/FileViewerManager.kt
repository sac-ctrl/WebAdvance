package com.cylonid.nativealpha.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.cylonid.nativealpha.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileViewerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun openFile(file: File) {
        val mimeType = getMimeType(file.extension)

        // Try internal viewer first for supported formats
        if (canHandleInternally(mimeType)) {
            openWithInternalViewer(file)
            return
        }

        // Fallback to external apps
        openWithExternalApp(file, mimeType)
    }

    private fun openWithInternalViewer(file: File) {
        val intent = android.content.Intent(context, com.cylonid.nativealpha.ui.UniversalFileViewerActivity::class.java).apply {
            putExtra("FILE_PATH", file.absolutePath)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openWithExternalApp(file: File, mimeType: String? = null) {
        val finalMimeType = mimeType ?: getMimeType(file.extension)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, finalMimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic file opener
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }

    private fun canHandleInternally(mimeType: String?): Boolean {
        return when {
            mimeType?.startsWith("image/") == true -> true
            mimeType?.startsWith("video/") == true -> true
            mimeType?.startsWith("audio/") == true -> true
            mimeType == "application/pdf" -> true
            mimeType?.startsWith("text/") == true -> true
            else -> false
        }
    }

    fun getRecentFiles(): List<java.io.File> {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val appDir = context.getExternalFilesDir(null)
        val dirs = listOfNotNull(downloadsDir, appDir)
        return dirs.flatMap { dir ->
            dir.listFiles()?.toList() ?: emptyList()
        }.filter { it.isFile }.sortedByDescending { it.lastModified() }.take(50)
    }

    fun shareFile(file: java.io.File) {
        val mimeType = getMimeType(file.extension) ?: "*/*"
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${file.name}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun deleteFile(file: java.io.File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    private fun getMimeType(extension: String): String? {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "avi" -> "video/avi"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "html" -> "text/html"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            else -> null
        }
    }
}