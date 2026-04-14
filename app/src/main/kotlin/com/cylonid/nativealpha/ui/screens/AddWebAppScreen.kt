package com.cylonid.nativealpha.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cylonid.nativealpha.ui.theme.*
import com.cylonid.nativealpha.viewmodel.AddWebAppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWebAppScreen(
    navController: NavController,
    onWebAppAdded: () -> Unit,
    editWebAppId: Long = 0L,
    viewModel: AddWebAppViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val isEdit = editWebAppId > 0L
    var showValidationError by remember { mutableStateOf(false) }

    LaunchedEffect(editWebAppId) {
        if (editWebAppId > 0L) {
            viewModel.loadForEdit(editWebAppId)
        }
    }

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
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isEdit) "Edit App" else "New App",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isEdit) "Update web app settings" else "Add a website to your dashboard",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = {
                            if (viewModel.url.isBlank()) {
                                showValidationError = true
                            } else {
                                viewModel.saveWebApp()
                                onWebAppAdded()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = BgDeep),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(if (isEdit) "Update" else "Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                SectionCard(title = "App Details", icon = Icons.Rounded.Web) {
                    WAOSTextField(
                        value = viewModel.url,
                        onValueChange = { viewModel.url = it; showValidationError = false },
                        label = "Website URL",
                        placeholder = "https://example.com",
                        icon = Icons.Rounded.Link,
                        isError = showValidationError && viewModel.url.isBlank(),
                        errorText = "URL is required",
                        keyboardType = KeyboardType.Uri
                    )
                    Spacer(Modifier.height(12.dp))
                    WAOSTextField(
                        value = viewModel.name,
                        onValueChange = { viewModel.name = it },
                        label = "Display Name",
                        placeholder = "My App",
                        icon = Icons.Rounded.Label
                    )
                    Spacer(Modifier.height(12.dp))
                    var categoryExpanded by remember { mutableStateOf(false) }
                    val categories = listOf("General", "Social", "Work", "News", "Entertainment", "Tools", "Shopping", "Finance")
                    ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                        OutlinedTextField(
                            value = viewModel.category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category", color = TextSecondary, fontSize = 12.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(14.dp),
                            colors = waosTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.background(CardSurface)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = TextPrimary) },
                                    onClick = { viewModel.category = cat; categoryExpanded = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    WAOSTextField(
                        value = viewModel.customGroup,
                        onValueChange = { viewModel.customGroup = it },
                        label = "Custom Group (optional)",
                        placeholder = "e.g. Daily, Favorites",
                        icon = Icons.Rounded.Folder
                    )
                }

                SectionCard(title = "Browser Settings", icon = Icons.Rounded.Settings) {
                    WAOSToggleRow("Enable JavaScript", Icons.Rounded.Code, viewModel.isJavaScriptEnabled) { viewModel.isJavaScriptEnabled = it }
                    WAOSToggleRow("Ad Blocker", Icons.Rounded.Block, viewModel.isAdblockEnabled) { viewModel.isAdblockEnabled = it }
                    WAOSToggleRow("Dark Mode", Icons.Rounded.DarkMode, viewModel.isDarkModeEnabled) { viewModel.isDarkModeEnabled = it }
                    WAOSToggleRow("Desktop Mode", Icons.Rounded.Computer, viewModel.useDesktopUserAgent) { viewModel.useDesktopUserAgent = it }
                    WAOSToggleRow("Enable Zooming", Icons.Rounded.ZoomIn, viewModel.isEnableZooming) { viewModel.isEnableZooming = it }
                    WAOSToggleRow("Keep Screen Awake", Icons.Rounded.WbSunny, viewModel.isKeepAwake) { viewModel.isKeepAwake = it }
                }

                SectionCard(title = "Auto Refresh", icon = Icons.Rounded.Refresh) {
                    WAOSToggleRow("Smart Refresh", Icons.Rounded.Autorenew, viewModel.isSmartRefreshEnabled) { viewModel.isSmartRefreshEnabled = it }
                    Spacer(Modifier.height(8.dp))
                    Text("Refresh Interval", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    val intervals = listOf("Manual", "5s", "15s", "30s", "1min", "5min")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        intervals.forEach { interval ->
                            val selected = viewModel.refreshIntervalText == interval
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) CyanPrimary else CardBorder.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) CyanPrimary else CardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .then(
                                        Modifier.clickable { viewModel.refreshIntervalText = interval }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    interval,
                                    color = if (selected) BgDeep else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                SectionCard(title = "Security", icon = Icons.Rounded.Security) {
                    WAOSToggleRow("Lock with PIN", Icons.Rounded.Lock, viewModel.isLocked) { viewModel.isLocked = it }
                    AnimatedVisibility(visible = viewModel.isLocked) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            WAOSTextField(
                                value = viewModel.pin,
                                onValueChange = { viewModel.pin = it },
                                label = "PIN",
                                placeholder = "Enter PIN",
                                icon = Icons.Rounded.Password,
                                keyboardType = KeyboardType.NumberPassword
                            )
                        }
                    }
                }

                SectionCard(title = "Permissions", icon = Icons.Rounded.AdminPanelSettings) {
                    WAOSToggleRow("Camera Access", Icons.Rounded.Camera, viewModel.isCameraPermission) { viewModel.isCameraPermission = it }
                    WAOSToggleRow("Microphone Access", Icons.Rounded.Mic, viewModel.isMicrophonePermission) { viewModel.isMicrophonePermission = it }
                }

                SectionCard(title = "User Agent", icon = Icons.Rounded.PhoneAndroid) {
                    WAOSToggleRow("Custom User Agent", Icons.Rounded.Edit, viewModel.useCustomUserAgent) { viewModel.useCustomUserAgent = it }
                    AnimatedVisibility(visible = viewModel.useCustomUserAgent) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            WAOSTextField(
                                value = viewModel.userAgent,
                                onValueChange = { viewModel.userAgent = it },
                                label = "User Agent String",
                                placeholder = "Mozilla/5.0..."
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (viewModel.url.isBlank()) {
                            showValidationError = true
                        } else {
                            viewModel.saveWebApp()
                            onWebAppAdded()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = BgDeep),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(if (isEdit) Icons.Rounded.Check else Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isEdit) "Update Web App" else "Add Web App", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
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
                    .background(CyanPrimary.copy(0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@Composable
private fun WAOSToggleRow(
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
private fun WAOSTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    icon: ImageVector? = null,
    isError: Boolean = false,
    errorText: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = TextSecondary, fontSize = 12.sp) },
            placeholder = { Text(placeholder, color = TextMuted, fontSize = 13.sp) },
            leadingIcon = icon?.let { { Icon(it, null, tint = TextSecondary, modifier = Modifier.size(16.dp)) } },
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = waosTextFieldColors(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
        if (isError && errorText.isNotEmpty()) {
            Text(errorText, color = ErrorRed, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
    }
}

@Composable
fun waosTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanPrimary.copy(0.7f),
    unfocusedBorderColor = CardBorder,
    focusedContainerColor = BgMedium,
    unfocusedContainerColor = BgMedium,
    cursorColor = CyanPrimary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    errorBorderColor = ErrorRed,
    errorContainerColor = ErrorRedContainer.copy(0.3f)
)
