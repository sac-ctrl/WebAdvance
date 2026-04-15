package com.cylonid.nativealpha.util

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * Utility for managing storage paths across the app
 * Centralizes the WAOS folder structure:
 * /storage/emulated/0/Download/WAOS/{AppDisplayName}/
 *   ├── Screenshots/
 *   ├── Files, videos, images, etc.
 */
object StorageUtil {

    // Base WAOS directory
    fun getWaosBaseDir(): File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "WAOS"
    )

    // App-specific directory: /storage/emulated/0/Download/WAOS/{AppDisplayName}/
    fun getAppDownloadsDir(appDisplayName: String): File {
        val sanitizedName = appDisplayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(getWaosBaseDir(), sanitizedName).apply { mkdirs() }
    }

    // Screenshots folder: /storage/emulated/0/Download/WAOS/{AppDisplayName}/Screenshots/
    fun getScreenshotsDir(appDisplayName: String): File {
        return File(getAppDownloadsDir(appDisplayName), "Screenshots").apply { mkdirs() }
    }

    // Get all files and folders from app downloads directory
    fun getAppFilesAndFolders(appDisplayName: String): List<FileItem> {
        val dir = getAppDownloadsDir(appDisplayName)
        if (!dir.exists()) return emptyList()

        return dir.listFiles()?.map { file ->
            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) calculateDirSize(file) else file.length(),
                lastModified = file.lastModified(),
                mimeType = if (file.isDirectory) null else getMimeType(file.name)
            )
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
    }

    // Get all files in a specific directory (non-recursive)
    fun getFilesInDirectory(dirPath: String): List<FileItem> {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.listFiles()?.map { file ->
            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) calculateDirSize(file) else file.length(),
                lastModified = file.lastModified(),
                mimeType = if (file.isDirectory) null else getMimeType(file.name)
            )
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
    }

    // Recursively get all files in directory
    fun getAllFilesRecursive(dirPath: String): List<FileItem> {
        val result = mutableListOf<FileItem>()
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        dir.walkTopDown().forEach { file ->
            if (file != dir && file.isFile) {
                result.add(
                    FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = false,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        mimeType = getMimeType(file.name)
                    )
                )
            }
        }
        return result.sortedByDescending { it.lastModified }
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) calculateDirSize(file) else file.length()
            }
        }
        return size
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp|bmp)$", RegexOption.IGNORE_CASE)) -> "image/*"
            fileName.matches(Regex(".*\\.(mp4|mkv|webm|avi|mov|flv|wmv)$", RegexOption.IGNORE_CASE)) -> "video/*"
            fileName.matches(Regex(".*\\.(mp3|wav|aac|flac|opus|m4a|ogg)$", RegexOption.IGNORE_CASE)) -> "audio/*"
            fileName.matches(Regex(".*\\.pdf$", RegexOption.IGNORE_CASE)) -> "application/pdf"
            fileName.matches(Regex(".*\\.(txt|log|md|csv|json|xml|html)$", RegexOption.IGNORE_CASE)) -> "text/*"
            fileName.matches(Regex(".*\\.(zip|rar|7z|tar|gz)$", RegexOption.IGNORE_CASE)) -> "application/archive"
            fileName.matches(Regex(".*\\.apk$", RegexOption.IGNORE_CASE)) -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes == 0L) return "0 B"
        val k = 1024.0
        val sizes = arrayOf("B", "KB", "MB", "GB", "TB")
        val i = (Math.log(bytes.toDouble()) / Math.log(k)).toInt()
        val df = java.text.DecimalFormat("#,##0.#")
        return df.format(bytes / Math.pow(k, i.toDouble())) + " " + sizes[i]
    }

    data class FileItem(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
        val mimeType: String? = null
    )
}
