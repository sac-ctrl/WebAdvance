package com.cylonid.nativealpha.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cylonid.nativealpha.viewmodel.FileViewerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    viewModel: FileViewerViewModel = hiltViewModel()
) {
    val recentFiles by viewModel.recentFiles.collectAsState()
    val fileTypes by viewModel.fileTypes.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Viewer") },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // File Type Filter
            ScrollableTabRow(
                selectedTabIndex = fileTypes.indexOf(selectedType),
                modifier = Modifier.fillMaxWidth()
            ) {
                fileTypes.forEach { type ->
                    Tab(
                        selected = selectedType == type,
                        onClick = { viewModel.selectFileType(type) },
                        text = { Text(type.displayName) }
                    )
                }
            }

            // Recent Files List
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentFiles) { file ->
                    FileItemCard(
                        file = file,
                        onOpen = { viewModel.openFile(file) },
                        onShare = { viewModel.shareFile(file) },
                        onDelete = { viewModel.deleteFile(file) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileItemCard(
    file: FileItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File Icon
            Icon(
                imageVector = file.type.icon,
                contentDescription = file.type.displayName,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${file.size} • ${file.lastModified}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

// Data classes for the screen
data class FileItem(
    val id: Long,
    val name: String,
    val path: String,
    val size: String,
    val lastModified: String,
    val type: FileType
)

enum class FileType(val displayName: String, val extensions: List<String>, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    IMAGES("Images", listOf("jpg", "jpeg", "png", "gif", "bmp", "webp"), Icons.Default.Image),
    VIDEOS("Videos", listOf("mp4", "avi", "mkv", "mov", "wmv"), Icons.Default.VideoFile),
    DOCUMENTS("Documents", listOf("pdf", "doc", "docx", "txt", "rtf"), Icons.Default.Description),
    AUDIO("Audio", listOf("mp3", "wav", "flac", "aac"), Icons.Default.AudioFile),
    ARCHIVES("Archives", listOf("zip", "rar", "7z", "tar", "gz"), Icons.Default.FolderZip),
    ALL("All Files", emptyList(), Icons.Default.File)
}