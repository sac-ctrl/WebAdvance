package com.cylonid.nativealpha.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cylonid.nativealpha.manager.ClipboardItem
import com.cylonid.nativealpha.viewmodel.ClipboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardManagerScreen(
    webAppId: Long,
    onBackPressed: () -> Unit,
    viewModel: ClipboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardItems by viewModel.clipboardItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    LaunchedEffect(webAppId) {
        viewModel.loadClipboardItems(webAppId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clipboard Manager") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Clear all button
                    IconButton(onClick = {
                        viewModel.clearAllItems(webAppId)
                    }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear all")
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
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search clipboard") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            )

            if (clipboardItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No clipboard items yet")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clipboardItems) { item ->
                        ClipboardItemCard(
                            item = item,
                            onCopy = { viewModel.copyToSystemClipboard(item, context) },
                            onPin = { viewModel.togglePin(item) },
                            onDelete = { viewModel.deleteItem(item) },
                            onEdit = { /* TODO: Implement edit */ }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardItemCard(
    item: ClipboardItem,
    onCopy: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with type icon and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Type icon
                    val icon = when (item.type) {
                        ClipboardItem.Type.URL -> Icons.Default.Link
                        ClipboardItem.Type.IMAGE -> Icons.Default.Image
                        else -> Icons.Default.TextFields
                    }
                    Icon(
                        icon,
                        contentDescription = item.type.name,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Timestamp
                    Text(
                        text = formatTimestamp(item.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Pin indicator
                if (item.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content preview
            val previewText = when (item.type) {
                ClipboardItem.Type.URL -> item.content
                ClipboardItem.Type.IMAGE -> "[Image data]"
                else -> item.content.take(200) + if (item.content.length > 200) "..." else ""
            }

            Text(
                text = previewText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy")
                }
                IconButton(onClick = onPin) {
                    Icon(
                        if (item.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                        contentDescription = if (item.isPinned) "Unpin" else "Pin"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Date): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(timestamp)
}