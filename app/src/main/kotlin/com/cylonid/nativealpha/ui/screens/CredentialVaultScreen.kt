package com.cylonid.nativealpha.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.cylonid.nativealpha.manager.Credential
import com.cylonid.nativealpha.ui.components.*
import com.cylonid.nativealpha.ui.theme.*
import com.cylonid.nativealpha.viewmodel.CredentialViewModel

private fun generatePassword(): String {
    val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lower = "abcdefghijklmnopqrstuvwxyz"
    val digits = "0123456789"
    val special = "!@#\$%^&*"
    val all = upper + lower + digits + special
    return buildString {
        append(upper.random())
        append(lower.random())
        append(digits.random())
        append(special.random())
        repeat(12) { append(all.random()) }
    }.toList().shuffled().joinToString("")
}

private fun passwordStrength(pwd: String): Float {
    var score = 0f
    if (pwd.length >= 8) score += 0.2f
    if (pwd.length >= 12) score += 0.2f
    if (pwd.any { it.isUpperCase() }) score += 0.2f
    if (pwd.any { it.isDigit() }) score += 0.2f
    if (pwd.any { !it.isLetterOrDigit() }) score += 0.2f
    return score
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
        VaultAuthScreen(
            showBiometricButton = showBiometricButton,
            onBiometric = {
                val activity = context as? androidx.activity.ComponentActivity
                activity?.let { viewModel.authenticateWithBiometric(it as androidx.fragment.app.FragmentActivity) }
            },
            onPinUnlock = { viewModel.showPinDialog() },
            onBack = onBackPressed
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDeep)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(BgDark, BgDeep)))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        Brush.radialGradient(listOf(GradGreenStart.copy(0.4f), Color.Transparent)),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(1.dp, GradGreenEnd.copy(0.3f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, null, tint = GradGreenEnd, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Credential Vault", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text("${credentials.size} saved credentials", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = { viewModel.showAddCredentialDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = GradGreenEnd.copy(0.15f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.border(1.dp, GradGreenEnd.copy(0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.Add, null, tint = GradGreenEnd, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add", color = GradGreenEnd, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search credentials…", color = TextMuted, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GradGreenEnd,
                            unfocusedBorderColor = CardBorder,
                            cursorColor = GradGreenEnd,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp)
                    )
                }
            }

            if (credentials.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(CardSurface, RoundedCornerShape(20.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("No credentials stored", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("Tap + to add usernames, passwords", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(credentials, key = { it.id }) { credential ->
                        CredentialCard(
                            credential = credential,
                            onCopyUsername = { viewModel.copyUsername(credential, context) },
                            onCopyPassword = { viewModel.copyPassword(credential, context) },
                            onAutoFill = { onAutoFill?.invoke(credential.username, credential.password) },
                            onEdit = { viewModel.editCredential(credential) },
                            onDelete = { viewModel.deleteCredential(credential) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddCredentialDialog(
                credential = editingCredential,
                onDismiss = { viewModel.hideAddCredentialDialog() },
                onSave = { credential -> viewModel.saveCredential(webAppId, credential) }
            )
        }

        if (showPinDialog) {
            PinDialog(
                onDismiss = { viewModel.hidePinDialog() },
                onAuthenticate = { pin: String -> viewModel.authenticateWithPin(pin) }
            )
        }
    }
}

@Composable
private fun VaultAuthScreen(
    showBiometricButton: Boolean,
    onBiometric: () -> Unit,
    onPinUnlock: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        Brush.radialGradient(listOf(GradGreenStart.copy(0.4f), Color.Transparent)),
                        RoundedCornerShape(24.dp)
                    )
                    .border(2.dp, GradGreenEnd.copy(0.5f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = GradGreenEnd, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Credential Vault", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Authenticate to access your saved credentials", color = TextSecondary, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(32.dp))

            if (showBiometricButton) {
                OutlinedButton(
                    onClick = onBiometric,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GradGreenEnd),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GradGreenEnd.copy(0.5f))
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = GradGreenEnd, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Fingerprint / Face ID", color = GradGreenEnd, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f), color = CardBorder)
                    Text("  or  ", color = TextMuted, fontSize = 12.sp)
                    Divider(modifier = Modifier.weight(1f), color = CardBorder)
                }
                Spacer(Modifier.height(14.dp))
            }

            OutlinedButton(
                onClick = onPinUnlock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VioletSecondary),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VioletSecondary.copy(0.5f))
            ) {
                Icon(Icons.Default.VpnKey, null, tint = VioletSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Unlock with PIN", color = VioletSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onBack) {
                Text("Go back", color = TextMuted)
            }
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
    var expanded by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.radialGradient(listOf(GradGreenStart.copy(0.35f), Color.Transparent)),
                                RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, GradGreenStart.copy(0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            credential.title.firstOrNull()?.uppercase() ?: "?",
                            color = GradGreenEnd,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            credential.title,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (credential.username.isNotBlank()) {
                            Text(
                                credential.username,
                                color = TextMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onAutoFill, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Input, null, tint = GradGreenEnd, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Divider(color = CardBorder, thickness = 0.5.dp)
                    Spacer(Modifier.height(12.dp))

                    if (credential.username.isNotBlank()) {
                        VaultField("Username", credential.username, false, {})
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            CredSmallBtn("Copy User", Icons.Default.Person, CyanPrimary, onCopyUsername)
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    if (credential.password.isNotBlank()) {
                        VaultField(
                            "Password",
                            if (showPassword) credential.password else "•".repeat(credential.password.length),
                            showPassword,
                            { showPassword = !showPassword }
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            CredSmallBtn("Copy Pass", Icons.Default.Lock, VioletSecondary, onCopyPassword)
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    if (credential.url?.isNotBlank() == true) {
                        VaultField("URL", credential.url ?: "", false, {})
                        Spacer(Modifier.height(10.dp))
                    }

                    if (credential.notes?.isNotBlank() == true) {
                        VaultField("Notes", credential.notes ?: "", false, {})
                        Spacer(Modifier.height(10.dp))
                    }

                    Divider(color = CardBorder, thickness = 0.5.dp)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                            Icon(Icons.Default.Edit, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit", color = TextSecondary, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(4.dp))
                        TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                            Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", color = ErrorRed, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultField(label: String, value: String, isPassword: Boolean, onToggle: () -> Unit) {
    Column {
        Text(label, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(3.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BgDark)
                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                color = TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
                maxLines = if (isPassword) 1 else 3,
                overflow = TextOverflow.Ellipsis,
                fontFamily = if (isPassword) androidx.compose.ui.text.font.FontFamily.Monospace else androidx.compose.ui.text.font.FontFamily.Default
            )
            if (isPassword) {
                IconButton(onClick = onToggle, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (isPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CredSmallBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(0.12f))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Surface(modifier = Modifier.matchParentSize(), color = Color.Transparent, onClick = onClick) {}
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

    val strength = passwordStrength(password)
    val strengthColor = when {
        strength < 0.4f -> ErrorRed
        strength < 0.7f -> Color(0xFFFFB800)
        else -> StatusActive
    }
    val strengthLabel = when {
        strength < 0.4f -> "Weak"
        strength < 0.7f -> "Fair"
        else -> "Strong"
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(GradGreenEnd.copy(0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (credential == null) Icons.Default.Add else Icons.Default.Edit, null, tint = GradGreenEnd, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    if (credential == null) "Add Credential" else "Edit Credential",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))

            VaultInputField("Title", title, Icons.Default.Label, false, {}) { title = it }
            Spacer(Modifier.height(10.dp))
            VaultInputField("Username / Email", username, Icons.Default.Person, false, {}) { username = it }
            Spacer(Modifier.height(10.dp))

            VaultInputField("Password", password, Icons.Default.Lock, !showPassword, { showPassword = !showPassword }) { password = it }
            if (password.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { strength },
                            modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = strengthColor,
                            trackColor = CardBorder
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(strengthLabel, color = strengthColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { password = generatePassword() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, null, tint = VioletSecondary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Generate", color = VioletSecondary, fontSize = 11.sp)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { password = generatePassword() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, null, tint = VioletSecondary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Generate", color = VioletSecondary, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            VaultInputField("URL (optional)", url, Icons.Default.Link, false, {}) { url = it }
            Spacer(Modifier.height(10.dp))
            VaultInputField("Notes (optional)", notes, Icons.Default.Notes, false, {}) { notes = it }
            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(
                                Credential(
                                    id = credential?.id ?: 0L,
                                    webAppId = credential?.webAppId ?: 0L,
                                    title = title,
                                    username = username,
                                    password = password,
                                    url = url.ifBlank { null },
                                    notes = notes.ifBlank { null }
                                )
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = GradGreenEnd),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultInputField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    obscure: Boolean,
    onToggleVisibility: () -> Unit,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextMuted, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = label != "Notes (optional)",
        leadingIcon = { Icon(icon, null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
        trailingIcon = if (label == "Password") ({
            IconButton(onClick = onToggleVisibility, modifier = Modifier.size(24.dp)) {
                Icon(if (obscure) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }) else null,
        visualTransformation = if (obscure) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (label == "Password") KeyboardType.Password else KeyboardType.Text
        ),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GradGreenEnd,
            unfocusedBorderColor = CardBorder,
            cursorColor = GradGreenEnd,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = BgDark,
            unfocusedContainerColor = BgDark,
            focusedLabelColor = GradGreenEnd
        ),
        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinDialog(
    onDismiss: () -> Unit,
    onAuthenticate: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(VioletSecondary.copy(0.15f), RoundedCornerShape(14.dp))
                    .border(1.dp, VioletSecondary.copy(0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Password, null, tint = VioletSecondary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Enter PIN", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Enter your PIN to access the vault", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it; error = false },
                placeholder = { Text("PIN", color = TextMuted) },
                isError = error,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VioletSecondary,
                    unfocusedBorderColor = CardBorder,
                    cursorColor = VioletSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary)
            )
            if (error) {
                Spacer(Modifier.height(6.dp))
                Text("Incorrect PIN", color = ErrorRed, fontSize = 12.sp)
            }
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                ) { Text("Cancel") }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { onAuthenticate(pin) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletSecondary)
                ) { Text("Unlock", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
