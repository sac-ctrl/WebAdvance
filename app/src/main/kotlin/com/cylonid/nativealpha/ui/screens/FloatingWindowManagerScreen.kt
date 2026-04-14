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
import com.cylonid.nativealpha.viewmodel.FloatingWindowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingWindowManagerScreen(
    viewModel: FloatingWindowViewModel = hiltViewModel()
) {
    val openWindows by viewModel.openWindows.collectAsState()
    val windowPresets by viewModel.windowPresets.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Floating Window Manager") },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.closeAllWindows() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Close all")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.saveCurrentLayout() }) {
                Icon(Icons.Default.Save, contentDescription = "Save layout")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Open Windows Section
            Text(
                text = "Open Windows",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (openWindows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No floating windows open")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(openWindows) { window ->
                        FloatingWindowCard(
                            window = window,
                            onClose = { viewModel.closeWindow(window.id) },
                            onMinimize = { viewModel.minimizeWindow(window.id) },
                            onMaximize = { viewModel.maximizeWindow(window.id) }
                        )
                    }
                }
            }

            // Window Presets Section
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Saved Layouts",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(windowPresets) { preset ->
                    PresetCard(
                        preset = preset,
                        onLoad = { viewModel.loadPreset(preset.id) },
                        onDelete = { viewModel.deletePreset(preset.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingWindowCard(
    window: FloatingWindowInfo,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = window.appName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${window.width}x${window.height} at (${window.x}, ${window.y})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onMinimize) {
                    Icon(Icons.Default.Minimize, contentDescription = "Minimize")
                }
                IconButton(onClick = onMaximize) {
                    Icon(Icons.Default.Maximize, contentDescription = "Maximize")
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetCard(
    preset: WindowPreset,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onLoad
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${preset.windows.size} windows",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete preset")
            }
        }
    }
}

// Data classes for the screen
data class FloatingWindowInfo(
    val id: Long,
    val appName: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val isMinimized: Boolean = false
)

data class WindowPreset(
    val id: Long,
    val name: String,
    val windows: List<FloatingWindowInfo>
)