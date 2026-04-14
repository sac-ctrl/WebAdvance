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
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.manager.DownloadItem
import com.cylonid.nativealpha.viewmodel.DownloadViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadHistoryScreen(
    webAppId: Long,
    viewModel: DownloadViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloads.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val filterBy by viewModel.filterBy.collectAsState()

    LaunchedEffect(webAppId) {
        viewModel.loadDownloads(webAppId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download History") },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Search, Filter and Sort
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = { Text("Search downloads") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sort dropdown
                    var sortExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = sortExpanded,
                        onExpandedChange = { sortExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = when (sortBy) {
                                DownloadViewModel.SortBy.DATE_DESC -> "Newest first"
                                DownloadViewModel.SortBy.DATE_ASC -> "Oldest first"
                                DownloadViewModel.SortBy.NAME_ASC -> "Name A-Z"
                                DownloadViewModel.SortBy.NAME_DESC -> "Name Z-A"
                                DownloadViewModel.SortBy.SIZE_DESC -> "Largest first"
                                DownloadViewModel.SortBy.SIZE_ASC -> "Smallest first"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sort by") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded)
                            },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest first") },
                                onClick = {
                                    viewModel.updateSortBy(DownloadViewModel.SortBy.DATE_DESC)
                                    sortExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest first") },
                                onClick = {
                                    viewModel.updateSortBy(DownloadViewModel.SortBy.DATE_ASC)
                                    sortExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name A-Z") },
                                onClick = {
                                    viewModel.updateSortBy(DownloadViewModel.SortBy.NAME_ASC)
                                    sortExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name Z-A") },
                                onClick = {
                                    viewModel.updateSortBy(DownloadViewModel.SortBy.NAME_DESC)
                                    sortExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Largest first") },
                                onClick = {
                                    viewModel.updateSortBy(DownloadViewModel.SortBy.SIZE_DESC)
                                    sortExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Smallest first") },
                                onClick = {
                                    viewModel.updateSortBy(DownloadViewModel.SortBy.SIZE_ASC)
                                    sortExpanded = false
                                }
                            )
                        }
                    }

                    // Filter dropdown
                    var filterExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = filterExpanded,
                        onExpandedChange = { filterExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = when (filterBy) {
                                DownloadViewModel.FilterBy.ALL -> "All files"
                                DownloadViewModel.FilterBy.COMPLETED -> "Completed"
                                DownloadViewModel.FilterBy.DOWNLOADING -> "Downloading"
                                DownloadViewModel.FilterBy.FAILED -> "Failed"
                                DownloadViewModel.FilterBy.IMAGES -> "Images"
                                DownloadViewModel.FilterBy.VIDEOS -> "Videos"
                                DownloadViewModel.FilterBy.DOCUMENTS -> "Documents"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Filter") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = filterExpanded)
                            },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = filterExpanded,
                            onDismissRequest = { filterExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All files") },
                                onClick = {
                                    viewModel.updateFilterBy(DownloadViewModel.FilterBy.ALL)
                                    filterExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Completed") },
                                onClick = {
                                    viewModel.updateFilterBy(DownloadViewModel.FilterBy.COMPLETED)
                                    filterExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Downloading") },
                                onClick = {
                                    viewModel.updateFilterBy(DownloadViewModel.FilterBy.DOWNLOADING)
                                    filterExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Failed") },
                                onClick = {
                                    viewModel.updateFilterBy(DownloadViewModel.FilterBy.FAILED)
                                    filterExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Images") },
                                onClick = {
                                    viewModel.updateFilterBy(DownloadViewModel.FilterBy.IMAGES)
                                    filterExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Videos") },
                                onClick = {
                                    viewModel.updateFilterBy(DownloadViewModel.FilterBy.VIDEOS)
                                    filterExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Documents") },
                                onClick = {
                                    viewModel.updateFilterBy(DownloadViewModel.FilterBy.DOCUMENTS)
                                    filterExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No downloads yet")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(downloads) { download ->
                        DownloadItemCard(
                            download = download,
                            onOpen = { viewModel.openFile(download) },
                            onShare = { viewModel.shareFile(download) },
                            onDelete = { viewModel.deleteDownload(download) },
                            onRetry = { viewModel.retryDownload(download) },
                            onPause = { viewModel.pauseDownload(download) },
                            onResume = { viewModel.resumeDownload(download) },
                            onCancel = { viewModel.cancelDownload(download) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadItemCard(
    download: DownloadItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // File name and type icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = rememberAsyncImagePainter(
                                model = getFileTypeIcon(download.mimeType)
                            ),
                            contentDescription = "File type",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = download.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress and size info
                    if (download.status == DownloadItem.Status.DOWNLOADING) {
                        LinearProgressIndicator(
                            progress = download.progressPercentage / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${download.progressPercentage}% • ${download.formattedDownloadedSize} / ${download.formattedFileSize}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (download.formattedSpeed.isNotEmpty()) {
                            Text(
                                text = "${download.formattedSpeed} • ETA: ${download.formattedETA}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = download.formattedFileSize,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Date
                    Text(
                        text = formatTimestamp(download.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status indicator
                Column(horizontalAlignment = Alignment.End) {
                    when (download.status) {
                        DownloadItem.Status.COMPLETED -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DownloadItem.Status.DOWNLOADING -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                        DownloadItem.Status.FAILED -> {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Failed",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        DownloadItem.Status.PAUSED -> {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = "Paused",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        DownloadItem.Status.PENDING -> {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Pending",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (download.status) {
                    DownloadItem.Status.COMPLETED -> {
                        TextButton(onClick = onOpen) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open")
                        }
                        TextButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                    DownloadItem.Status.DOWNLOADING -> {
                        TextButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause")
                        }
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Default.Cancel, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel")
                        }
                    }
                    DownloadItem.Status.PAUSED -> {
                        TextButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume")
                        }
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Default.Cancel, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel")
                        }
                    }
                    DownloadItem.Status.FAILED -> {
                        TextButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry")
                        }
                    }
                    else -> {} // PENDING - no actions
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getFileTypeIcon(mimeType: String?): Int {
    return when {
        mimeType?.startsWith("image/") == true -> R.drawable.ic_image
        mimeType?.startsWith("video/") == true -> R.drawable.ic_video
        mimeType?.startsWith("audio/") == true -> R.drawable.ic_audio
        mimeType == "application/pdf" -> R.drawable.ic_pdf
        mimeType?.startsWith("text/") == true -> R.drawable.ic_text
        mimeType == "application/vnd.android.package-archive" -> R.drawable.ic_apk
        else -> R.drawable.ic_file
    }
}