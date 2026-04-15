package com.cylonid.nativealpha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cylonid.nativealpha.manager.Credential

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

@Composable
fun LockingDialog(
    showBiometric: Boolean,
    onBiometricClick: () -> Unit,
    onPinSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    autoLockMinutes: Int = 5,
    isLoading: Boolean = false
) {
    var pinInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.fillMaxWidth(0.9f),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Lock",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Credential Vault Locked",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Authenticate to access your saved credentials",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Biometric Button
                if (showBiometric) {
                    OutlinedButton(
                        onClick = onBiometricClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = "Biometric",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fingerprint / Face ID")
                    }

                    Divider(modifier = Modifier.padding(8.dp))
                    Text(
                        "Or unlock with PIN:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // PIN Input
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        pinError = ""
                    },
                    label = { Text("PIN") },
                    placeholder = { Text("Enter 4-6 digits") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle PIN visibility"
                            )
                        }
                    },
                    isError = pinError.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )

                if (pinError.isNotEmpty()) {
                    Text(
                        pinError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Auto-locks in $autoLockMinutes minutes of inactivity",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinInput.isNotBlank()) {
                        onPinSubmit(pinInput)
                    } else {
                        pinError = "PIN cannot be empty"
                    }
                },
                modifier = Modifier.height(40.dp),
                enabled = !isLoading && pinInput.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Unlock")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

fun generatePassword(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
    return (1..12)
        .map { chars.random() }
        .joinToString("")
}