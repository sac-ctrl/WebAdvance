package com.cylonid.nativealpha.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.viewmodel.AddWebAppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWebAppScreen(
    onWebAppAdded: () -> Unit,
    viewModel: AddWebAppViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Web App") },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveWebApp(); onWebAppAdded() }) {
                        Text("Save")
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
            // URL Input
            OutlinedTextField(
                value = viewModel.url,
                onValueChange = { viewModel.url = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Name Input
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Custom Group
            OutlinedTextField(
                value = viewModel.customGroup,
                onValueChange = { viewModel.customGroup = it },
                label = { Text("Custom Group (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Desktop/Mobile User Agent Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Desktop User Agent", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.useDesktopUserAgent,
                    onCheckedChange = { viewModel.useDesktopUserAgent = it }
                )
            }

            // User Agent Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Custom User Agent", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.useCustomUserAgent,
                    onCheckedChange = { viewModel.useCustomUserAgent = it }
                )
            }

            if (viewModel.useCustomUserAgent) {
                OutlinedTextField(
                    value = viewModel.userAgent,
                    onValueChange = { viewModel.userAgent = it },
                    label = { Text("User Agent") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // JavaScript Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable JavaScript", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.isJavaScriptEnabled,
                    onCheckedChange = { viewModel.isJavaScriptEnabled = it }
                )
            }

            // Adblock Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Adblock", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.isAdblockEnabled,
                    onCheckedChange = { viewModel.isAdblockEnabled = it }
                )
            }

            // Dark Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Dark Mode", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.isDarkModeEnabled,
                    onCheckedChange = { viewModel.isDarkModeEnabled = it }
                )
            }

            // Refresh Interval
            Text("Refresh Interval", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Manual", "5s", "15s", "30s", "1min", "5min").forEach { interval ->
                    FilterChip(
                        selected = viewModel.refreshIntervalText == interval,
                        onClick = { viewModel.refreshIntervalText = interval },
                        label = { Text(interval) }
                    )
                }
            }

            // Smart Refresh Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Smart Refresh (DOM changes)", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.isSmartRefreshEnabled,
                    onCheckedChange = { viewModel.isSmartRefreshEnabled = it }
                )
            }

            // Lock Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lock with PIN", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.isLocked,
                    onCheckedChange = { viewModel.isLocked = it }
                )
            }

            if (viewModel.isLocked) {
                OutlinedTextField(
                    value = viewModel.pin,
                    onValueChange = { viewModel.pin = it },
                    label = { Text("PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Custom Download Folder
            OutlinedTextField(
                value = viewModel.customDownloadFolder,
                onValueChange = { viewModel.customDownloadFolder = it },
                label = { Text("Custom Download Folder (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Clipboard Max Items
            OutlinedTextField(
                value = viewModel.clipboardMaxItems.toString(),
                onValueChange = { viewModel.clipboardMaxItems = it.toIntOrNull() ?: 50 },
                label = { Text("Max Clipboard Items") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Credential Auto-lock Timeout
            OutlinedTextField(
                value = viewModel.credentialAutoLockTimeout.toString(),
                onValueChange = { viewModel.credentialAutoLockTimeout = it.toLongOrNull() ?: 300000 },
                label = { Text("Credential Auto-lock Timeout (ms)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Floating Window Settings
            Text("Floating Window Defaults", style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.floatingWindowDefaultWidth.toString(),
                    onValueChange = { viewModel.floatingWindowDefaultWidth = it.toIntOrNull() ?: 800 },
                    label = { Text("Width") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = viewModel.floatingWindowDefaultHeight.toString(),
                    onValueChange = { viewModel.floatingWindowDefaultHeight = it.toIntOrNull() ?: 600 },
                    label = { Text("Height") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = viewModel.floatingWindowDefaultOpacity.toString(),
                onValueChange = { viewModel.floatingWindowDefaultOpacity = it.toFloatOrNull() ?: 1.0f },
                label = { Text("Opacity (0.0-1.0)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Screenshot Save Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Screenshot Save Location", modifier = Modifier.weight(1f))
                Text(viewModel.screenshotSaveLocation)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = viewModel.screenshotSaveLocation == "global",
                    onCheckedChange = {
                        viewModel.screenshotSaveLocation = if (it) "global" else "app"
                    }
                )
            }

            // Link Copier Default Format
            var formatExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = formatExpanded,
                onExpandedChange = { formatExpanded = it }
            ) {
                OutlinedTextField(
                    value = viewModel.linkCopierDefaultFormat,
                    onValueChange = {},
                    label = { Text("Link Copier Default Format") },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = formatExpanded,
                    onDismissRequest = { formatExpanded = false }
                ) {
                    listOf("url", "url_title", "markdown", "html").forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format) },
                            onClick = {
                                viewModel.linkCopierDefaultFormat = format
                                formatExpanded = false
                            }
                        )
                    }
                }
            }

            // User Agent Override
            OutlinedTextField(
                value = viewModel.userAgentOverride ?: "",
                onValueChange = { viewModel.userAgentOverride = it.takeIf { it.isNotBlank() } },
                label = { Text("User Agent Override (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Cache Mode
            var cacheExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = cacheExpanded,
                onExpandedChange = { cacheExpanded = it }
            ) {
                OutlinedTextField(
                    value = viewModel.cacheMode,
                    onValueChange = {},
                    label = { Text("Cache Mode") },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = cacheExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = cacheExpanded,
                    onDismissRequest = { cacheExpanded = false }
                ) {
                    listOf("default", "no_cache", "cache_only").forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode) },
                            onClick = {
                                viewModel.cacheMode = mode
                                cacheExpanded = false
                            }
                        )
                    }
                }
            }

            // Additional Permissions
            Text("Permissions", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Keep Screen Awake", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.isKeepAwake,
                    onCheckedChange = { viewModel.isKeepAwake = it }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Camera Permission", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.isCameraPermission,
                    onCheckedChange = { viewModel.isCameraPermission = it }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Microphone Permission", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.isMicrophonePermission,
                    onCheckedChange = { viewModel.isMicrophonePermission = it }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Zooming", modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.isEnableZooming,
                    onCheckedChange = { viewModel.isEnableZooming = it }
                )
            }
        }
    }
}