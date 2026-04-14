package com.cylonid.nativealpha.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.cylonid.nativealpha.R
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileViewerManager @Inject constructor(
    private val context: Context
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