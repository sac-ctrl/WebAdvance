package com.cylonid.nativealpha.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cylonid.nativealpha.ui.theme.*
import com.cylonid.nativealpha.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(CardSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Rounded.ArrowBack, "Back", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Settings", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Customize your WAOS experience", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                SettingsSectionCard(title = "Appearance", icon = Icons.Rounded.Palette) {
                    SettingsToggleRow("Dark Mode (WAOS Default)", Icons.Rounded.DarkMode, true) {}
                    var themeExpanded by remember { mutableStateOf(false) }
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(expanded = themeExpanded, onExpandedChange = { themeExpanded = it }) {
                        OutlinedTextField(
                            value = viewModel.appTheme,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("App Theme", color = TextSecondary, fontSize = 12.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(themeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(14.dp),
                            colors = settingsTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false },
                            modifier = Modifier.background(CardSurface)
                        ) {
                            listOf("System", "Dark", "Light").forEach { theme ->
                                DropdownMenuItem(
                                    text = { Text(theme, color = TextPrimary) },
                                    onClick = { viewModel.appTheme = theme; themeExpanded = false }
                                )
                            }
                        }
                    }
                }

                SettingsSectionCard(title = "Dashboard", icon = Icons.Rounded.Dashboard) {
                    SettingsInfoRow("Grid Columns", "2 columns (auto-adaptive)")
                    Spacer(Modifier.height(8.dp))
                    SettingsToggleRow("Show Category Chips", Icons.Rounded.Category, true) {}
                    SettingsToggleRow("Show Status Indicators", Icons.Rounded.Circle, true) {}
                    SettingsToggleRow("Animated Cards", Icons.Rounded.AutoAwesome, true) {}
                }

                SettingsSectionCard(title = "Notifications", icon = Icons.Rounded.Notifications) {
                    SettingsToggleRow("Global Notifications", Icons.Rounded.NotificationsActive, viewModel.globalNotificationsEnabled) {
                        viewModel.globalNotificationsEnabled = it
                    }
                    SettingsToggleRow("Badge Count", Icons.Rounded.Label, viewModel.globalNotificationsEnabled) {}
                }

                SettingsSectionCard(title = "Floating Windows", icon = Icons.Rounded.OpenInNew) {
                    SettingsToggleRow("Enable Floating Windows", Icons.Rounded.OpenWith, viewModel.floatingWindowsEnabled) {
                        viewModel.floatingWindowsEnabled = it
                    }
                    AnimatedVisibility(visible = viewModel.floatingWindowsEnabled) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            SettingsInfoRow("Max Windows", "${viewModel.maxFloatingWindows} simultaneous windows")
                        }
                    }
                }

                SettingsSectionCard(title = "Privacy & Security", icon = Icons.Rounded.Security) {
                    SettingsToggleRow("Global Credential Vault", Icons.Rounded.Lock, viewModel.globalVaultEnabled) {
                        viewModel.globalVaultEnabled = it
                    }
                    SettingsToggleRow("Clipboard Manager", Icons.Rounded.ContentPaste, viewModel.globalClipboardEnabled) {
                        viewModel.globalClipboardEnabled = it
                    }
                    SettingsInfoRow("Auto-lock Timeout", "5 minutes")
                }

                SettingsSectionCard(title = "Developer", icon = Icons.Rounded.Code) {
                    SettingsToggleRow("Developer Mode", Icons.Rounded.BugReport, viewModel.developerModeEnabled) {
                        viewModel.developerModeEnabled = it
                    }
                    SettingsInfoRow("JS Console", if (viewModel.developerModeEnabled) "Enabled in WebView" else "Disabled")
                }

                SettingsSectionCard(title = "Backup & Restore", icon = Icons.Rounded.Backup) {
                    Button(
                        onClick = { viewModel.exportData() },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary.copy(0.12f), contentColor = CyanPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Export All Data", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.importData() },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Import Data", fontWeight = FontWeight.SemiBold)
                    }
                }

                SettingsSectionCard(title = "About", icon = Icons.Rounded.Info) {
                    SettingsInfoRow("App", "WAOS - Web App Operating System")
                    SettingsInfoRow("Version", "1.5.2")
                    SettingsInfoRow("Engine", "Android System WebView")
                    SettingsInfoRow("Architecture", "Multi-process Sandboxed WebView")
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(VioletSecondary.copy(0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = VioletSecondary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BgDeep,
                checkedTrackColor = CyanPrimary,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CardBorder
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VioletSecondary.copy(0.7f),
    unfocusedBorderColor = CardBorder,
    focusedContainerColor = BgMedium,
    unfocusedContainerColor = BgMedium,
    cursorColor = VioletSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
