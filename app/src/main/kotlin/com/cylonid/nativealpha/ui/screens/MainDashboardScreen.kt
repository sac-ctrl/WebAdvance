package com.cylonid.nativealpha.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.ui.WebViewActivity
import com.cylonid.nativealpha.ui.theme.*
import com.cylonid.nativealpha.viewmodel.MainViewModel
import com.cylonid.nativealpha.viewmodel.SettingsViewModel
import com.cylonid.nativealpha.service.FloatingWindowService
import com.cylonid.nativealpha.ui.ClipboardManagerActivity
import com.cylonid.nativealpha.ui.CredentialVaultActivity
import com.cylonid.nativealpha.ui.DownloadHistoryActivity
import com.cylonid.nativealpha.waos.util.WaosConstants

enum class SortOption(val displayName: String) {
    NAME("Name"),
    LAST_USED("Last Used"),
    MOST_ACTIVE("Most Active"),
    CUSTOM("Custom Order")
}

enum class GroupOption(val displayName: String) {
    NONE("No Grouping"),
    CATEGORY("Category"),
    STATUS("Status"),
    CUSTOM("Custom Groups")
}

private val categoryColors = mapOf(
    "Social" to Pair(GradPinkStart, GradPinkEnd),
    "Work" to Pair(GradBlueStart, GradBlueEnd),
    "News" to Pair(GradOrangeStart, GradOrangeEnd),
    "Entertainment" to Pair(GradVioletStart, GradVioletEnd),
    "Tools" to Pair(GradCyanStart, GradCyanEnd),
    "Shopping" to Pair(GradYellowStart, GradYellowEnd),
    "Finance" to Pair(GradGreenStart, GradGreenEnd),
    "General" to Pair(GradBlueStart, GradVioletEnd)
)

private fun getAppGradient(webApp: WebApp): Pair<Color, Color> {
    val cat = webApp.category ?: "General"
    return categoryColors[cat] ?: (GradCyanStart to GradCyanEnd)
}

private fun getAppEmoji(webApp: WebApp): String {
    val url = webApp.url.lowercase()
    return when {
        url.contains("google") -> "G"
        url.contains("youtube") -> "▶"
        url.contains("twitter") || url.contains("x.com") -> "𝕏"
        url.contains("facebook") -> "f"
        url.contains("instagram") -> "◉"
        url.contains("reddit") -> "R"
        url.contains("gmail") || url.contains("mail") -> "✉"
        url.contains("whatsapp") -> "W"
        url.contains("telegram") -> "✈"
        url.contains("github") -> "⌨"
        url.contains("linkedin") -> "in"
        url.contains("netflix") -> "N"
        url.contains("spotify") -> "♪"
        url.contains("amazon") -> "A"
        else -> webApp.name.take(1).uppercase()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainDashboardScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val webApps by viewModel.filteredWebApps.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val groupBy by viewModel.groupBy.collectAsState()
    val groupedWebApps by viewModel.groupedWebApps.collectAsState()

    val showCategoryChips = settingsViewModel.showCategoryChips
    val showStatusIndicators = settingsViewModel.showStatusIndicators
    val floatingWindowsEnabled = settingsViewModel.floatingWindowsEnabled

    var selectedCategory by remember { mutableStateOf("All") }
    var showSortSheet by remember { mutableStateOf(false) }
    var contextMenuApp by remember { mutableStateOf<WebApp?>(null) }
    var showDeleteDialog by remember { mutableStateOf<WebApp?>(null) }

    val categories = listOf("All") + listOf("Social", "Work", "News", "Entertainment", "Tools", "Shopping", "Finance", "General")

    val filteredByCategory = if (selectedCategory == "All") webApps
    else webApps.filter { (it.category ?: "General") == selectedCategory }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgShift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bgShift"
    )

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(CyanPrimary.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.1f * bgShift),
                    radius = size.width * 0.7f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(VioletSecondary.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.4f + size.height * 0.2f * bgShift),
                    radius = size.width * 0.6f
                )
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(BgDeep, BgDeep.copy(alpha = 0.95f), Color.Transparent)
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "WAOS",
                                color = CyanPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            )
                            Text(
                                "Dashboard",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { viewModel.toggleViewMode() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(CardSurface, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                                    contentDescription = "Toggle view",
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { showSortSheet = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(CardSurface, CircleShape)
                            ) {
                                Icon(
                                    Icons.Rounded.Sort,
                                    contentDescription = "Sort",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { navController.navigate("settings") },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(CardSurface, CircleShape)
                            ) {
                                Icon(
                                    Icons.Rounded.Settings,
                                    contentDescription = "Settings",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = {
                            Text(
                                "Search web apps...",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Rounded.Close, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary.copy(alpha = 0.6f),
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface,
                            cursorColor = CyanPrimary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    if (showCategoryChips) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            items(categories) { cat ->
                                val selected = selectedCategory == cat
                                val bgColor by animateColorAsState(
                                    if (selected) CyanPrimary else CardSurface, label = "cat$cat"
                                )
                                val textColor by animateColorAsState(
                                    if (selected) BgDeep else TextSecondary, label = "catTxt$cat"
                                )
                                Box(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .background(bgColor, RoundedCornerShape(50))
                                        .border(
                                            1.dp,
                                            if (selected) CyanPrimary else CardBorder,
                                            RoundedCornerShape(50)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        cat,
                                        color = textColor,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate("add_webapp")
                    },
                    containerColor = CyanPrimary,
                    contentColor = BgDeep,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add web app"
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (filteredByCategory.isEmpty() && groupedWebApps.isEmpty()) {
                    EmptyStateView(onAdd = { navController.navigate("add_webapp") })
                } else {
                    if (groupBy == GroupOption.NONE) {
                        AnimatedContent(
                            targetState = isGridView,
                            transitionSpec = {
                                fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.95f) togetherWith
                                        fadeOut(tween(200))
                            },
                            label = "viewToggle"
                        ) { gridView ->
                            if (gridView) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(
                                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredByCategory, key = { it.id }) { webApp ->
                                        AppGridCard(
                                            webApp = webApp,
                                            showStatusIndicator = showStatusIndicators,
                                            onClick = {
                                                val intent = Intent(context, WebViewActivity::class.java)
                                                    .apply {
                                                        putExtra("webAppId", webApp.id)
                                                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                                    }
                                                context.startActivity(intent)
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                contextMenuApp = webApp
                                            }
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(
                                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredByCategory, key = { it.id }) { webApp ->
                                        AppListCard(
                                            webApp = webApp,
                                            showStatusIndicator = showStatusIndicators,
                                            onClick = {
                                                val intent = Intent(context, WebViewActivity::class.java)
                                                    .apply {
                                                        putExtra("webAppId", webApp.id)
                                                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                                    }
                                                context.startActivity(intent)
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                contextMenuApp = webApp
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            groupedWebApps.forEach { (groupName, apps) ->
                                item(key = "header_$groupName") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(20.dp)
                                                .background(CyanPrimary, RoundedCornerShape(2.dp))
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            groupName,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "${apps.size}",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                items(apps, key = { it.id }) { webApp ->
                                    AppListCard(
                                        webApp = webApp,
                                        showStatusIndicator = showStatusIndicators,
                                        onClick = {
                                            val intent = Intent(context, WebViewActivity::class.java)
                                                .apply {
                                                    putExtra("webAppId", webApp.id)
                                                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                                }
                                            context.startActivity(intent)
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            contextMenuApp = webApp
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        contextMenuApp?.let { app ->
            AppContextMenu(
                webApp = app,
                onDismiss = { contextMenuApp = null },
                onOpen = {
                    contextMenuApp = null
                    val intent = Intent(context, WebViewActivity::class.java)
                        .apply { putExtra("webAppId", app.id) }
                    context.startActivity(intent)
                },
                onFloat = {
                    contextMenuApp = null
                    if (!floatingWindowsEnabled) return@AppContextMenu
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                        val permIntent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(permIntent)
                    } else {
                        val intent = Intent(context, FloatingWindowService::class.java).apply {
                            action = FloatingWindowService.ACTION_ADD_WINDOW
                            putExtra("webAppId", app.id)
                            putExtra("webAppUrl", app.url)
                            putExtra("webAppName", app.name)
                        }
                        context.startService(intent)
                    }
                },
                onDownloads = {
                    contextMenuApp = null
                    val intent = Intent(context, DownloadHistoryActivity::class.java)
                        .apply { putExtra(WaosConstants.EXTRA_DOWNLOAD_APP_ID, app.id) }
                    context.startActivity(intent)
                },
                onClipboard = {
                    contextMenuApp = null
                    val intent = Intent(context, ClipboardManagerActivity::class.java)
                        .apply { putExtra(WaosConstants.EXTRA_CLIPBOARD_APP_ID, app.id) }
                    context.startActivity(intent)
                },
                onCredentials = {
                    contextMenuApp = null
                    val intent = Intent(context, CredentialVaultActivity::class.java)
                        .apply { putExtra(WaosConstants.EXTRA_WAOS_APP_ID, app.id) }
                    context.startActivity(intent)
                },
                onEdit = {
                    contextMenuApp = null
                    navController.navigate("edit_webapp/${app.id}")
                },
                onDelete = {
                    contextMenuApp = null
                    showDeleteDialog = app
                }
            )
        }

        showDeleteDialog?.let { app ->
            DeleteConfirmDialog(
                appName = app.name,
                onConfirm = {
                    viewModel.deleteWebApp(app)
                    showDeleteDialog = null
                },
                onDismiss = { showDeleteDialog = null }
            )
        }

        if (showSortSheet) {
            SortGroupSheet(
                currentSort = sortBy,
                currentGroup = groupBy,
                onSortChange = { viewModel.updateSortBy(it); showSortSheet = false },
                onGroupChange = { viewModel.updateGroupBy(it) },
                onDismiss = { showSortSheet = false }
            )
        }
    }
}

@Composable
private fun EmptyStateView(onAdd: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty")
    val pulse by infiniteTransition.animateFloat(
        0.95f, 1.05f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(pulse)
                .background(
                    Brush.radialGradient(listOf(CyanPrimary.copy(0.2f), Color.Transparent)),
                    CircleShape
                )
                .border(2.dp, CyanPrimary.copy(0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Web,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("No Web Apps Yet", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add your first web app to get started",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = BgDeep),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Web App", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridCard(
    webApp: WebApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showStatusIndicator: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.93f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "cardScale"
    )
    val (gradStart, gradEnd) = getAppGradient(webApp)
    val emoji = getAppEmoji(webApp)
    val statusColor = when (webApp.status) {
        WebApp.Status.ACTIVE -> StatusActive
        WebApp.Status.BACKGROUND -> StatusBg
        WebApp.Status.ERROR -> StatusError
    }

    val infiniteTransition = rememberInfiniteTransition(label = "status${webApp.id}")
    val glowAlpha by infiniteTransition.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow${webApp.id}"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    Brush.linearGradient(
                        listOf(gradStart.copy(0.3f), gradEnd.copy(0.15f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(12.dp, CircleShape, spotColor = gradEnd.copy(0.4f))
                    .background(
                        Brush.linearGradient(listOf(gradStart, gradEnd)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    emoji,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                webApp.name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                webApp.url.replace("https://", "").replace("http://", "").replace("www.", ""),
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            if (showStatusIndicator) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColor.copy(alpha = glowAlpha), CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = when (webApp.status) {
                            WebApp.Status.ACTIVE -> "Active"
                            WebApp.Status.BACKGROUND -> "Background"
                            WebApp.Status.ERROR -> "Error"
                        },
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (webApp.notificationCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(ErrorRed, RoundedCornerShape(50))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "${webApp.notificationCount}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListCard(
    webApp: WebApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showStatusIndicator: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "listScale"
    )
    val (gradStart, gradEnd) = getAppGradient(webApp)
    val emoji = getAppEmoji(webApp)
    val statusColor = when (webApp.status) {
        WebApp.Status.ACTIVE -> StatusActive
        WebApp.Status.BACKGROUND -> StatusBg
        WebApp.Status.ERROR -> StatusError
    }
    val statusLabel = when (webApp.status) {
        WebApp.Status.ACTIVE -> "Active"
        WebApp.Status.BACKGROUND -> "Bg"
        WebApp.Status.ERROR -> "Error"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "listStatus${webApp.id}")
    val glowAlpha by infiniteTransition.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "listGlow${webApp.id}"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = gradEnd.copy(0.5f))
                .background(
                    Brush.linearGradient(listOf(gradStart, gradEnd)),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                webApp.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                webApp.url.replace("https://", "").replace("http://", "").replace("www.", ""),
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (webApp.notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .background(ErrorRed, RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${webApp.notificationCount}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (showStatusIndicator) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(statusColor.copy(alpha = glowAlpha), CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun AppContextMenu(
    webApp: WebApp,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onFloat: () -> Unit,
    onDownloads: () -> Unit,
    onClipboard: () -> Unit,
    onCredentials: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            val (gradStart, gradEnd) = getAppGradient(webApp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(listOf(gradStart, gradEnd)),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(getAppEmoji(webApp), fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(webApp.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        webApp.url.replace("https://", "").replace("http://", ""),
                        color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = CardBorder)
            Spacer(Modifier.height(14.dp))

            Text(
                "QUICK ACTIONS",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(10.dp))

            val quickActions = listOf(
                Triple(Icons.Rounded.OpenInNew, "Open", onOpen),
                Triple(Icons.Rounded.OpenWith, "Float", onFloat),
                Triple(Icons.Rounded.Edit, "Edit", onEdit)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickActions.forEach { (icon, label, action) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgMedium)
                            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                            .clickable { action() }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val tint = when (label) {
                            "Open" -> CyanPrimary
                            "Float" -> VioletSecondary
                            else -> TextSecondary
                        }
                        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "FEATURES",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(10.dp))

            val featureActions = listOf(
                Triple(Icons.Rounded.Download, "Downloads", onDownloads),
                Triple(Icons.Rounded.ContentPaste, "Clipboard", onClipboard),
                Triple(Icons.Rounded.Lock, "Credentials", onCredentials)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                featureActions.forEach { (icon, label, action) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgMedium)
                            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                            .clickable { action() }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = CardBorder)
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDelete() }
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Delete, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Delete App", color = ErrorRed, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    appName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Delete $appName?", fontWeight = FontWeight.Bold) },
        text = { Text("This will permanently remove this web app and all its data.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = ErrorRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortGroupSheet(
    currentSort: SortOption,
    currentGroup: GroupOption,
    onSortChange: (SortOption) -> Unit,
    onGroupChange: (GroupOption) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Text("Sort & Group", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))

            Text("Sort by", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            SortOption.values().forEach { opt ->
                val selected = opt == currentSort
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) CyanPrimary.copy(0.1f) else Color.Transparent)
                        .clickable { onSortChange(opt) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(opt.displayName, color = if (selected) CyanPrimary else TextPrimary, modifier = Modifier.weight(1f))
                    if (selected) Icon(Icons.Rounded.Check, null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = CardBorder)
            Spacer(Modifier.height(16.dp))

            Text("Group by", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            GroupOption.values().forEach { opt ->
                val selected = opt == currentGroup
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) VioletSecondary.copy(0.1f) else Color.Transparent)
                        .clickable { onGroupChange(opt) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(opt.displayName, color = if (selected) VioletSecondary else TextPrimary, modifier = Modifier.weight(1f))
                    if (selected) Icon(Icons.Rounded.Check, null, tint = VioletSecondary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = BgDeep),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}
