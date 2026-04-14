package com.cylonid.nativealpha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.manager.FileViewerManager
import com.cylonid.nativealpha.ui.screens.FileItem
import com.cylonid.nativealpha.ui.screens.FileType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FileViewerViewModel @Inject constructor(
    private val fileViewerManager: FileViewerManager
) : ViewModel() {

    private val _recentFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val recentFiles: StateFlow<List<FileItem>> = _recentFiles.asStateFlow()

    private val _fileTypes = MutableStateFlow(FileType.values().toList())
    val fileTypes: StateFlow<List<FileType>> = _fileTypes.asStateFlow()

    private val _selectedType = MutableStateFlow(FileType.ALL)
    val selectedType: StateFlow<FileType> = _selectedType.asStateFlow()

    init {
        loadRecentFiles()
    }

    fun selectFileType(type: FileType) {
        _selectedType.value = type
        loadRecentFiles()
    }

    private fun loadRecentFiles() {
        viewModelScope.launch {
            val files = fileViewerManager.getRecentFiles().map { file ->
                FileItem(
                    id = file.lastModified(),
                    name = file.name,
                    path = file.absolutePath,
                    size = formatFileSize(file.length()),
                    lastModified = formatLastModified(file.lastModified()),
                    type = getFileType(file)
                )
            }.filter { fileItem ->
                _selectedType.value == FileType.ALL ||
                _selectedType.value.extensions.any { ext ->
                    fileItem.name.lowercase().endsWith(".$ext")
                }
            }
            _recentFiles.value = files
        }
    }

    fun refreshFiles() {
        loadRecentFiles()
    }

    fun openFile(file: FileItem) {
        fileViewerManager.openFile(File(file.path))
    }

    fun shareFile(file: FileItem) {
        fileViewerManager.shareFile(File(file.path))
    }

    fun deleteFile(file: FileItem) {
        viewModelScope.launch {
            if (fileViewerManager.deleteFile(File(file.path))) {
                loadRecentFiles()
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.1f %s".format(size, units[unitIndex])
    }

    private fun formatLastModified(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val days = diff / (1000 * 60 * 60 * 24)

        return when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days < 7 -> "$days days ago"
            days < 30 -> "${days / 7} weeks ago"
            else -> "${days / 30} months ago"
        }
    }

    private fun getFileType(file: File): FileType {
        val extension = file.extension.lowercase()
        return FileType.values().find { type ->
            type.extensions.contains(extension)
        } ?: FileType.ALL
    }
}