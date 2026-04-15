package com.cylonid.nativealpha.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cylonid.nativealpha.ui.theme.*
import java.io.File

/**
 * Image viewer with zoom, rotate, and filter capabilities
 */
@Composable
fun ImageViewer(
    file: File,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var zoom by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var rotation by remember { mutableStateOf(0f) }
    var brightness by remember { mutableStateOf(1f) }
    var showControls by remember { mutableStateOf(true) }

    val bitmap = remember {
        try {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BgDeep),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.BrokenImage,
                    contentDescription = "Failed to load image",
                    tint = ErrorRed,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Failed to load image", color = TextPrimary, fontSize = 16.sp)
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BgDeep)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom *= gestureZoom
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            // Image display
            Image(
                bitmap = bitmap,
                contentDescription = "Viewing: ${file.name}",
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = zoom,
                        scaleY = zoom,
                        translationX = offsetX,
                        translationY = offsetY,
                        rotationZ = rotation,
                        alpha = brightness
                    ),
                contentScale = ContentScale.Fit
            )

            // Top bar with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(CardSurface.copy(0.8f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        file.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Zoom: ${String.format("%.0f%%", zoom * 100)}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Bottom controls
            if (showControls) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .align(Alignment.BottomCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(
                                    androidx.compose.ui.graphics.Color.Transparent,
                                    BgDeep.copy(0.9f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    // Brightness slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.BrightnessLow, "Brightness", tint = TextMuted, modifier = Modifier.size(20.dp))
                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            valueRange = 0.3f..2f,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.BrightnessHigh, "Brightness", tint = TextMuted, modifier = Modifier.size(20.dp))
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { zoom = 1f },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GradCyanStart.copy(0.2f)),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FitScreen, "Reset", tint = GradCyanEnd, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fit", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { rotation = (rotation + 90) % 360 },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GradVioletStart.copy(0.2f)),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.RotateRight, "Rotate", tint = GradVioletEnd, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rotate", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { zoom = 1f; offsetX = 0f; offsetY = 0f; rotation = 0f; brightness = 1f },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(0.2f)),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, "Reset", tint = ErrorRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 10.sp)
                        }
                    }
                }
            }

            // Tap to toggle controls
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            showControls = !showControls
                        }
                    }
            )
        }
    }
}

@Composable
private fun Image(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    androidx.compose.foundation.Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

@Composable
private fun detectTapGestures(onTap: () -> Unit) {
    Modifier.pointerInput(Unit) {
        androidx.compose.foundation.gestures.detectTapGestures {
            onTap()
        }
    }
}

private fun androidx.compose.ui.graphics.GraphicsLayerScope.graphicsLayer(
    scaleX: Float,
    scaleY: Float,
    translationX: Float,
    translationY: Float,
    rotationZ: Float,
    alpha: Float
) {
    this.scaleX = scaleX
    this.scaleY = scaleY
    this.translationX = translationX
    this.translationY = translationY
    this.rotationZ = rotationZ
    this.alpha = alpha
}
