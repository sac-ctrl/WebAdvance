package com.cylonid.nativealpha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.data.DownloadItemDao
import com.cylonid.nativealpha.manager.DownloadItem
import com.cylonid.nativealpha.manager.DownloadManager
import com.cylonid.nativealpha.manager.FileViewerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadDao: DownloadItemDao,
    private val downloadManager: DownloadManager,
    private val fileViewerManager: FileViewerManager
) : ViewModel() {

    enum class SortBy {
        DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, SIZE_DESC, SIZE_ASC
    }

    enum class FilterBy {
        ALL, COMPLETED, DOWNLOADING, FAILED, IMAGES, VIDEOS, DOCUMENTS
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortBy = MutableStateFlow(SortBy.DATE_DESC)
    val sortBy: StateFlow<SortBy> = _sortBy

    private val _filterBy = MutableStateFlow(FilterBy.ALL)
    val filterBy: StateFlow<FilterBy> = _filterBy

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = combine(
        _downloads,
        _searchQuery,
        _sortBy,
        _filterBy
    ) { downloads, query, sort, filter ->
        var filtered = downloads

        // Apply search filter
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.fileName.contains(query, ignoreCase = true) }
        }

        // Apply status/type filter
        filtered = filtered.filter { item ->
            when (filter) {
                FilterBy.ALL -> true
                FilterBy.COMPLETED -> item.status == DownloadItem.Status.COMPLETED
                FilterBy.DOWNLOADING -> item.status == DownloadItem.Status.DOWNLOADING
                FilterBy.FAILED -> item.status == DownloadItem.Status.FAILED
                FilterBy.IMAGES -> item.mimeType?.startsWith("image/") == true
                FilterBy.VIDEOS -> item.mimeType?.startsWith("video/") == true
                FilterBy.DOCUMENTS -> item.mimeType?.startsWith("text/") == true ||
                        item.mimeType == "application/pdf" ||
                        item.mimeType?.contains("document") == true
            }
        }

        // Apply sorting
        filtered.sortedWith { a, b ->
            when (sort) {
                SortBy.DATE_DESC -> b.timestamp.compareTo(a.timestamp)
                SortBy.DATE_ASC -> a.timestamp.compareTo(b.timestamp)
                SortBy.NAME_ASC -> a.fileName.compareTo(b.fileName, ignoreCase = true)
                SortBy.NAME_DESC -> b.fileName.compareTo(a.fileName, ignoreCase = true)
                SortBy.SIZE_DESC -> b.fileSize.compareTo(a.fileSize)
                SortBy.SIZE_ASC -> a.fileSize.compareTo(b.fileSize)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadDownloads(webAppId: Long) {
        viewModelScope.launch {
            downloadDao.getDownloadsForApp(webAppId).collect { downloads ->
                _downloads.value = downloads
            }
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

    fun openFile(download: DownloadItem) {
        download.filePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                fileViewerManager.openFile(file)
            }
        }
    }

    fun shareFile(download: DownloadItem) {
        download.filePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                // TODO: Implement share functionality using Android Sharesheet
            }
        }
    }

    fun pauseDownload(download: DownloadItem) {
        downloadManager.pauseDownload(download.id)
    }

    fun resumeDownload(download: DownloadItem) {
        downloadManager.resumeDownload(download)
    }

    fun cancelDownload(download: DownloadItem) {
        downloadManager.cancelDownload(download.id)
    }

    fun deleteDownload(download: DownloadItem) {
        viewModelScope.launch {
            downloadDao.deleteDownload(download.id)
            // Also delete file if exists
            download.filePath?.let { path ->
                File(path).delete()
            }
        }
    }

    fun retryDownload(download: DownloadItem) {
        downloadManager.downloadFile(download.webAppId, download.url, download.fileName)
    }
}