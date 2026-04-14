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
                    .padding(16.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            )

            if (credentials.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No credentials saved yet")
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
                            onAutoFill = {
                                onAutoFill?.invoke(credential.username, credential.password)
                            },
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
                onAuthenticate = { pin -> viewModel.authenticateWithPin(pin) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialCard(
    credential: Credential,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit,
    onAutoFill: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title and URL
            Text(
                text = credential.title,
                style = MaterialTheme.typography.titleMedium
            )
            credential.url?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Username
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Username",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = credential.username,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = onCopyUsername) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy username")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Password (masked)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "••••••••",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = onCopyPassword) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy password")
                }
            }

            // Notes
            credential.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAutoFill) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-fill")
                }
                Row {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCredentialDialog(
    credential: Credential?,
    onDismiss: () -> Unit,
    onSave: (Credential) -> Unit
) {
    var title by remember { mutableStateOf(credential?.title ?: "") }
    var username by remember { mutableStateOf(credential?.username ?: "") }
    var password by remember { mutableStateOf(credential?.password ?: "") }
    var url by remember { mutableStateOf(credential?.url ?: "") }
    var notes by remember { mutableStateOf(credential?.notes ?: "") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (credential == null) "Add Credential" else "Edit Credential") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { password = generatePassword() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Generate password")
                            }
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        }
                    }
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                        val newCredential = Credential(
                            id = credential?.id ?: 0,
                            webAppId = credential?.webAppId,
                            title = title,
                            username = username,
                            password = password,
                            url = url.takeIf { it.isNotBlank() },
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                        onSave(newCredential)
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinDialog(
    onDismiss: () -> Unit,
    onAuthenticate: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter PIN") },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pin.isNotBlank()) {
                        onAuthenticate(pin)
                    }
                }
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun generatePassword(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
    return (1..12)
        .map { chars.random() }
        .joinToString("")
}
