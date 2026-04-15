package com.cylonid.nativealpha.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cylonid.nativealpha.ui.theme.*
import com.cylonid.nativealpha.viewmodel.FloatingWindowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingWindowManagerScreen(
    viewModel: FloatingWindowViewModel = hiltViewModel()
) {
    val openWindows by viewModel.openWindows.collectAsState()
    val windowPresets by viewModel.windowPresets.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgShift by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bgShift"
    )

    Box(modifier = Modifier.fillMaxSize().background(BgDeep)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(VioletSecondary.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.1f + size.height * 0.15f * bgShift),
                    radius = size.width * 0.6f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(CyanPrimary.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.7f),
                    radius = size.width * 0.5f
                )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(
                        Brush.verticalGradient(listOf(BgDeep, BgDeep.copy(0.95f), Color.Transparent))
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.radialGradient(listOf(VioletSecondary.copy(0.25f), Color.Transparent)),
                                RoundedCornerShape(14.dp)
                            )
                            .border(1.dp, VioletSecondary.copy(0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.OpenWith,
                            contentDescription = null,
                            tint = VioletSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "WAOS",
                            color = VioletSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )
                        Text(
                            "Floating Windows",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (openWindows.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.closeAllWindows() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(ErrorRed.copy(0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, ErrorRed.copy(0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Rounded.Clear,
                                contentDescription = "Close all",
                                tint = ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(CyanPrimary, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Active Windows",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(CyanPrimary.copy(0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${openWindows.size}",
                                color = CyanPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (openWindows.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(CardSurface)
                                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                                0.95f, 1.05f,
                                infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "p"
                            )
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .scale(pulse)
                                    .background(
                                        Brush.radialGradient(listOf(VioletSecondary.copy(0.2f), Color.Transparent)),
                                        CircleShape
                                    )
                                    .border(1.dp, VioletSecondary.copy(0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Web,
                                    contentDescription = null,
                                    tint = VioletSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No floating windows open",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Float any web app from its context menu or the WebView toolbar",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(openWindows) { window ->
                        WaosFloatingWindowCard(
                            window = window,
                            onClose = { viewModel.closeWindow(window.id) },
                            onMinimize = { viewModel.minimizeWindow(window.id) },
                            onMaximize = { viewModel.maximizeWindow(window.id) }
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(VioletSecondary, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Saved Layouts",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(VioletSecondary.copy(0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${windowPresets.size}",
                                color = VioletSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (windowPresets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardSurface)
                                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No saved layouts yet. Save a layout to quickly restore window arrangements.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(windowPresets) { preset ->
                        WaosPresetCard(
                            preset = preset,
                            onLoad = { viewModel.loadPreset(preset.id) },
                            onDelete = { viewModel.deletePreset(preset.id) }
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            FloatingActionButton(
                onClick = { viewModel.saveCurrentLayout() },
                containerColor = VioletSecondary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Rounded.Save, contentDescription = "Save layout", modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun WaosFloatingWindowCard(
    window: FloatingWindowInfo,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "win${window.id}")
    val glowAlpha by infiniteTransition.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow${window.id}"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .border(1.dp, CyanPrimary.copy(0.2f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        Brush.linearGradient(listOf(GradCyanStart.copy(0.3f), GradCyanEnd.copy(0.15f))),
                        RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, CyanPrimary.copy(0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Web, null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        window.appName,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, false)
                    )
                    if (window.isMinimized) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(VioletSecondary.copy(0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Minimized", color = VioletSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${window.width} × ${window.height}  •  pos (${window.x}, ${window.y})",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier
                        .size(34.dp)
                        .background(CardSurface, RoundedCornerShape(10.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Rounded.Minimize, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onMaximize,
                    modifier = Modifier
                        .size(34.dp)
                        .background(CyanPrimary.copy(0.1f), RoundedCornerShape(10.dp))
                        .border(1.dp, CyanPrimary.copy(0.3f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Rounded.Fullscreen, null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(34.dp)
                        .background(ErrorRed.copy(0.12f), RoundedCornerShape(10.dp))
                        .border(1.dp, ErrorRed.copy(0.3f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Rounded.Close, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = CardBorder)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(StatusActive.copy(alpha = glowAlpha), CircleShape)
            )
            Text(
                "Active",
                color = StatusActive,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Drag corner to resize",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun WaosPresetCard(
    preset: WindowPreset,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onLoad() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(VioletSecondary.copy(0.12f), RoundedCornerShape(12.dp))
                .border(1.dp, VioletSecondary.copy(0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.GridView, null, tint = VioletSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(preset.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                "${preset.windows.size} window${if (preset.windows.size != 1) "s" else ""}",
                color = TextMuted,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
                onClick = onLoad,
                modifier = Modifier
                    .size(34.dp)
                    .background(VioletSecondary.copy(0.1f), RoundedCornerShape(10.dp))
                    .border(1.dp, VioletSecondary.copy(0.3f), RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = VioletSecondary, modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(34.dp)
                    .background(ErrorRed.copy(0.1f), RoundedCornerShape(10.dp))
                    .border(1.dp, ErrorRed.copy(0.3f), RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Rounded.Delete, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}

data class FloatingWindowInfo(
    val id: Long,
    val appName: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val isMinimized: Boolean = false
)

data class WindowPreset(
    val id: Long,
    val name: String,
    val windows: List<FloatingWindowInfo>
)
