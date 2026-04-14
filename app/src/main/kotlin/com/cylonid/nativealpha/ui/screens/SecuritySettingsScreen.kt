package com.cylonid.nativealpha.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cylonid.nativealpha.viewmodel.SecurityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val securitySettings by viewModel.securitySettings.collectAsState()
    val biometricAvailable by viewModel.biometricAvailable.collectAsState()
    val pinSet by viewModel.pinSet.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Settings") },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Authentication",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // PIN Lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PIN Lock")
                                Text(
                                    text = if (pinSet) "PIN is set" else "No PIN set",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { viewModel.showPinSetup() }
                            ) {
                                Text(if (pinSet) "Change PIN" else "Set PIN")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Biometric Authentication
                        if (biometricAvailable) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Biometric Authentication")
                                    Text(
                                        text = "Use fingerprint or face unlock",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = securitySettings.biometricEnabled,
                                    onCheckedChange = { viewModel.toggleBiometric(it) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Auto-lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Auto-lock after")
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = securitySettings.autoLockTime.displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    AutoLockTime.values().forEach { time ->
                                        DropdownMenuItem(
                                            text = { Text(time.displayName) },
                                            onClick = {
                                                viewModel.setAutoLockTime(time)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Data Protection",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Encrypt Credentials
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Encrypt Credentials")
                                Text(
                                    text = "Securely store login information",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = securitySettings.encryptCredentials,
                                onCheckedChange = { viewModel.toggleCredentialEncryption(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Clear Data on Failed Attempts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Clear Data on Failed Attempts")
                                Text(
                                    text = "Erase all data after 10 failed unlocks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = securitySettings.clearDataOnFail,
                                onCheckedChange = { viewModel.toggleClearDataOnFail(it) }
                            )
                        }
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Privacy",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Block Screenshots
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Block Screenshots")
                                Text(
                                    text = "Prevent screenshots in web apps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = securitySettings.blockScreenshots,
                                onCheckedChange = { viewModel.toggleBlockScreenshots(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Incognito Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Incognito Mode")
                                Text(
                                    text = "Don't save browsing history",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = securitySettings.incognitoMode,
                                onCheckedChange = { viewModel.toggleIncognitoMode(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class AutoLockTime(val minutes: Int, val displayName: String) {
    IMMEDIATE(0, "Immediately"),
    ONE_MINUTE(1, "1 minute"),
    FIVE_MINUTES(5, "5 minutes"),
    FIFTEEN_MINUTES(15, "15 minutes"),
    ONE_HOUR(60, "1 hour"),
    NEVER(-1, "Never")
}

data class SecuritySettings(
    val biometricEnabled: Boolean = false,
    val autoLockTime: AutoLockTime = AutoLockTime.FIFTEEN_MINUTES,
    val encryptCredentials: Boolean = true,
    val clearDataOnFail: Boolean = false,
    val blockScreenshots: Boolean = false,
    val incognitoMode: Boolean = false
)