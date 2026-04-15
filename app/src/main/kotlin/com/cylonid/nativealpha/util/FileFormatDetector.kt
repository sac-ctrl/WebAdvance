package com.cylonid.nativealpha.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.webkit.MimeTypeMap
import java.io.File

/**
 * Utility for detecting file types and MIME types
 */
object FileFormatDetector {

    enum class FileType {
        IMAGE, VIDEO, AUDIO, PDF, DOCUMENT, ARCHIVE, TEXT, UNKNOWN
    }

    data class FileFormat(
        val type: FileType,
        val mimeType: String,
        val description: String,
        val icon: String
    )

    fun detectFormat(file: File): FileFormat {
        val fileName = file.name
        val extension = fileName.substringAfterLast(".", "").lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"

        return when {
            // Images
            extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "tiff") ->
                FileFormat(FileType.IMAGE, mimeType, "Image", "🖼️")

            // Videos
            extension in listOf("mp4", "webm", "mkv", "avi", "flv", "mov", "m4v", "3gp") ->
                FileFormat(FileType.VIDEO, mimeType, "Video", "🎬")

            // Audio
            extension in listOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "wma", "aiff") ->
                FileFormat(FileType.AUDIO, mimeType, "Audio", "🎵")

            // PDF
            extension == "pdf" ->
                FileFormat(FileType.PDF, "application/pdf", "PDF Document", "📕")

            // Documents
            extension in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods") ->
                FileFormat(FileType.DOCUMENT, mimeType, "Document", "📄")

            // Archives
            extension in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") ->
                FileFormat(FileType.ARCHIVE, mimeType, "Archive", "📦")

            // Text
            extension in listOf("txt", "csv", "json", "xml", "html", "htm", "css", "js", "kt", "java", "py", "md") ->
                FileFormat(FileType.TEXT, "text/plain", "Text File", "📝")

            else -> FileFormat(FileType.UNKNOWN, mimeType, "Unknown", "📄")
        }
    }

    fun getVideoMetadata(file: File): VideoMetadata? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            retriever.release()
            VideoMetadata(duration, width, height)
        } catch (e: Exception) {
            null
        }
    }

    data class VideoMetadata(val duration: Long, val width: Int, val height: Int)
}
