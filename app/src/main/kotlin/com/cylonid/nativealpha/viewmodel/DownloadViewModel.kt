package com.cylonid.nativealpha.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.data.DownloadItemDao
import com.cylonid.nativealpha.manager.DownloadItem
import com.cylonid.nativealpha.manager.DownloadManager
import com.cylonid.nativealpha.manager.FileViewerManager
import com.cylonid.nativealpha.repository.WebAppRepository
import com.cylonid.nativealpha.util.StorageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadItemDao,
    private val downloadManager: DownloadManager,
    private val fileViewerManager: FileViewerManager,
    private val webAppRepository: WebAppRepository
) : ViewModel() {

    enum class SortBy {
        DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, SIZE_DESC, SIZE_ASC
    }

    enum class FilterBy {
        ALL, COMPLETED, DOWNLOADING, FAILED, IMAGES, VIDEOS, DOCUMENTS, FOLDERS, SCREENSHOTS
    }

    data class FileSystemItem(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long = 0,
        val lastModified: Long = System.currentTimeMillis(),
        val mimeType: String? = null,
        val icon: String = if (isDirectory) "📁" else "📄"
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortBy = MutableStateFlow(SortBy.DATE_DESC)
    val sortBy: StateFlow<SortBy> = _sortBy

    private val _filterBy = MutableStateFlow(FilterBy.ALL)
    val filterBy: StateFlow<FilterBy> = _filterBy

    private val _fileSystemItems = MutableStateFlow<List<FileSystemItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private var currentAppId: Long? = null
    private var currentAppName: String? = null

    val downloads: StateFlow<List<FileSystemItem>> = combine(
        _fileSystemItems,
        _searchQuery,
        _sortBy,
        _filterBy
    ) { items, query, sort, filter ->
        var filtered = items

        // Apply search filter
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }

        // Apply filter
        filtered = filtered.filter { item ->
            when (filter) {
                FilterBy.ALL -> true
                FilterBy.FOLDERS -> item.isDirectory
                FilterBy.SCREENSHOTS -> item.path.contains("Screenshots", ignoreCase = true)
                FilterBy.IMAGES -> !item.isDirectory && item.mimeType?.startsWith("image/") == true
                FilterBy.VIDEOS -> !item.isDirectory && item.mimeType?.startsWith("video/") == true
                FilterBy.DOCUMENTS -> !item.isDirectory && (
                    item.mimeType?.startsWith("text/") == true ||
                    item.mimeType == "application/pdf" ||
                    item.mimeType?.contains("document") == true
                )
                FilterBy.COMPLETED, FilterBy.DOWNLOADING, FilterBy.FAILED -> true // Legacy support
            }
        }

        // Apply sorting
        filtered.sortedWith { a, b ->
            when (sort) {
                SortBy.DATE_DESC -> b.lastModified.compareTo(a.lastModified)
                SortBy.DATE_ASC -> a.lastModified.compareTo(b.lastModified)
                SortBy.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                SortBy.NAME_DESC -> b.name.compareTo(a.name, ignoreCase = true)
                SortBy.SIZE_DESC -> b.size.compareTo(a.size)
                SortBy.SIZE_ASC -> a.size.compareTo(b.size)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadDownloads(webAppId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            currentAppId = webAppId
            val webApp = webAppRepository.getWebAppById(webAppId).firstOrNull()
            currentAppName = webApp?.name ?: "Unknown"
            
            // Scan the file system folder
            scanAppDownloadsFolder()
            _isLoading.value = false
        }
    }

    private fun scanAppDownloadsFolder() {
        currentAppName?.let { appName ->
            val storageItems = mutableListOf<FileSystemItem>()
            
            // Scan main downloads folder
            val mainDir = StorageUtil.getAppDownloadsDir(appName)
            if (mainDir.exists()) {
                mainDir.listFiles()?.forEach { file ->
                    val mimeType = if (file.isDirectory) null else getMimeType(file.name)
                    storageItems.add(
                        FileSystemItem(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = file.isDirectory,
                            size = if (file.isDirectory) calculateDirSize(file) else file.length(),
                            lastModified = file.lastModified(),
                            mimeType = mimeType,
                            icon = getIconForMimeType(mimeType, file.isDirectory)
                        )
                    )
                }
            }
            
            _fileSystemItems.value = storageItems
        }
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

    private fun getIconForMimeType(mimeType: String?, isDirectory: Boolean): String {
        return when {
            isDirectory -> "📁"
            mimeType?.startsWith("image/") == true -> "🖼️"
            mimeType?.startsWith("video/") == true -> "🎬"
            mimeType?.startsWith("audio/") == true -> "🎵"
            mimeType == "application/pdf" -> "📕"
            mimeType?.startsWith("text/") == true -> "📄"
            mimeType?.contains("archive") == true -> "📦"
            mimeType?.contains("android.package") == true -> "🔧"
            else -> "📄"
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortBy(sortBy: SortBy) {
        _sortBy.value = sortBy
    }

    fun updateFilterBy(filterBy: FilterBy) {
        _filterBy.value = filterBy
    }

    fun openFile(item: FileSystemItem) {
        viewModelScope.launch {
            if (!item.isDirectory) {
                fileViewerManager.openFile(File(item.path))
            }
        }
    }

    fun shareFile(item: FileSystemItem) {
        viewModelScope.launch {
            if (!item.isDirectory) {
                // TODO: Implement share functionality
            }
        }
    }

    fun deleteFile(item: FileSystemItem) {
        viewModelScope.launch {
            val file = File(item.path)
            if (file.exists()) {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
                scanAppDownloadsFolder()
            }
        }
    }

    fun renameFile(item: FileSystemItem, newName: String) {
        viewModelScope.launch {
            val file = File(item.path)
            val newFile = File(file.parent, newName)
            if (file.exists() && !newFile.exists()) {
                file.renameTo(newFile)
                scanAppDownloadsFolder()
            }
        }
    }

    fun getFileSize(item: FileSystemItem): String = StorageUtil.formatFileSize(item.size)

    fun reloadFiles() {
        scanAppDownloadsFolder()
    }
}