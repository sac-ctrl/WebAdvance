package com.cylonid.nativealpha.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cylonid.nativealpha.manager.Credential
import com.cylonid.nativealpha.viewmodel.CredentialViewModel
import com.cylonid.nativealpha.ui.components.*

private fun generatePassword(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
    return (1..12)
        .map { chars.random() }
        .joinToString("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialVaultScreen(
    webAppId: Long,
    onBackPressed: () -> Unit,
    onAutoFill: ((String, String) -> Unit)? = null,
    viewModel: CredentialViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val credentials by viewModel.credentials.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showBiometricButton by viewModel.showBiometricButton.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val showPinDialog by viewModel.showPinDialog.collectAsState()
    val editingCredential by viewModel.editingCredential.collectAsState()

    LaunchedEffect(webAppId) {
        viewModel.loadCredentials(webAppId)
        viewModel.checkAuthentication(webAppId)
    }

    if (!isAuthenticated) {
        // Show authentication required screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Credential Vault") },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Authentication required")
                    Spacer(modifier = Modifier.height(16.dp))
                    if (showBiometricButton) {
                        Button(onClick = {
                            val activity = context as? androidx.activity.ComponentActivity
                            activity?.let { viewModel.authenticateWithBiometric(it as androidx.fragment.app.FragmentActivity) }
                        }) {
                            Text("Unlock with Biometric")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("or")
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(onClick = { viewModel.showPinDialog() }) {
                        Text("Unlock with PIN")
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Credential Vault") },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Add credential button
                        IconButton(onClick = { viewModel.showAddCredentialDialog() }) {
                            Icon(Icons.Default.Add, contentDescription = "Add credential")
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
                    label = { Text("Search credentials") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                if (credentials.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No credentials stored")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(credentials) { credential ->
                            CredentialCard(
                                credential = credential,
                                onCopyUsername = { viewModel.copyUsername(credential, context) },
                                onCopyPassword = { viewModel.copyPassword(credential, context) },
                                onAutoFill = {},
                                onEdit = { viewModel.editCredential(credential) },
                                onDelete = { viewModel.deleteCredential(credential) }
                            )
                        }
                    }
                }
            }

            // Add/Edit Credential Dialog
            if (showAddDialog) {
                AddCredentialDialog(
                    credential = editingCredential,
                    onDismiss = { viewModel.hideAddCredentialDialog() },
                    onSave = { credential ->
                        viewModel.saveCredential(webAppId, credential)
                    }
                )
            }

            // PIN Dialog
            if (showPinDialog) {
                PinDialog(
                    onDismiss = { viewModel.hidePinDialog() },
                    onAuthenticate = { pin: String -> viewModel.authenticateWithPin(pin) }
                )
            }
        }
    }
}
