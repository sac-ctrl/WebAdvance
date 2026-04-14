package com.cylonid.nativealpha.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cylonid.nativealpha.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Theme
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            var themeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = themeExpanded,
                onExpandedChange = { themeExpanded = it }
            ) {
                OutlinedTextField(
                    value = viewModel.appTheme,
                    onValueChange = {},
                    label = { Text("App Theme") },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = themeExpanded,
                    onDismissRequest = { themeExpanded = false }
                ) {
                    listOf("System", "Light", "Dark").forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(theme) },
                            onClick = {
                                viewModel.appTheme = theme
                                themeExpanded = false
                            }
                        )
                    }
                }
            }

            // Dashboard Layout
            OutlinedTextField(
                value = viewModel.dashboardColumns.toString(),
                onValueChange = { viewModel.dashboardColumns = it.toIntOrNull() ?: 2 },
                label = { Text("Dashboard Columns") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Global Notifications
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Global Notifications", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.globalNotificationsEnabled,
                    onCheckedChange = { viewModel.globalNotificationsEnabled = it }
                )
            }

            // Floating Windows
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Floating Windows", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.floatingWindowsEnabled,
                    onCheckedChange = { viewModel.floatingWindowsEnabled = it }
                )
            }

            if (viewModel.floatingWindowsEnabled) {
                OutlinedTextField(
                    value = viewModel.maxFloatingWindows.toString(),
                    onValueChange = { viewModel.maxFloatingWindows = it.toIntOrNull() ?: 5 },
                    label = { Text("Max Simultaneous Floating Windows") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Global Clipboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Global Clipboard View", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.globalClipboardEnabled,
                    onCheckedChange = { viewModel.globalClipboardEnabled = it }
                )
            }

            // Global Vault
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Global Credential Vault", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.globalVaultEnabled,
                    onCheckedChange = { viewModel.globalVaultEnabled = it }
                )
            }

            // Auto-lock Timeout
            OutlinedTextField(
                value = viewModel.globalAutoLockTimeout.toString(),
                onValueChange = { viewModel.globalAutoLockTimeout = it.toLongOrNull() ?: 300000 },
                label = { Text("Global Auto-lock Timeout (ms)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Screen Orientation
            var orientationExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = orientationExpanded,
                onExpandedChange = { orientationExpanded = it }
            ) {
                OutlinedTextField(
                    value = viewModel.screenOrientation,
                    onValueChange = {},
                    label = { Text("Screen Orientation") },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = orientationExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = orientationExpanded,
                    onDismissRequest = { orientationExpanded = false }
                ) {
                    listOf("Portrait", "Landscape", "Auto").forEach { orientation ->
                        DropdownMenuItem(
                            text = { Text(orientation) },
                            onClick = {
                                viewModel.screenOrientation = orientation
                                orientationExpanded = false
                            }
                        )
                    }
                }
            }

            // Developer Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Developer Mode", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.developerModeEnabled,
                    onCheckedChange = { viewModel.developerModeEnabled = it }
                )
            }

            // Backup Section
            Spacer(modifier = Modifier.height(16.dp))
            Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = { viewModel.exportData() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export All Data")
            }

            Button(
                onClick = { viewModel.importData() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import Data")
            }

            // About Section
            Spacer(modifier = Modifier.height(16.dp))
            Text("About", style = MaterialTheme.typography.titleMedium)

            Text(
                text = "WAOS - Web App Operating System\nVersion 1.0.0",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}