package com.cylonid.nativealpha.fileviewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.webkit.MimeTypeMap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.io.File

sealed class FileViewerState {
    object Loading : FileViewerState()
    data class ImageViewer(val bitmap: Bitmap) : FileViewerState()
    data class TextViewer(val content: String) : FileViewerState()
    data class VideoViewer(val file: File) : FileViewerState()
    data class AudioViewer(val file: File) : FileViewerState()
    data class PdfViewer(val file: File) : FileViewerState()
    data class Error(val message: String) : FileViewerState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalFileViewerComposable(
    file: File,
    onBackPressed: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    var viewerState by remember { mutableStateOf<FileViewerState>(FileViewerState.Loading) }
    val context = LocalContext.current

    LaunchedEffect(file) {
        viewerState = loadFileContent(context, file)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
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
            when (val state = viewerState) {
                is FileViewerState.Loading -> {
                    CircularProgressIndicator()
                }
                is FileViewerState.ImageViewer -> {
                    ImageViewerContent(state.bitmap)
                }
                is FileViewerState.TextViewer -> {
                    TextViewerContent(state.content)
                }
                is FileViewerState.VideoViewer -> {
                    VideoViewerContent(state.file)
                }
                is FileViewerState.AudioViewer -> {
                    AudioViewerContent(state.file)
                }
                is FileViewerState.PdfViewer -> {
                    PdfViewerContent(state.file)
                }
                is FileViewerState.Error -> {
                    ErrorViewerContent(state.message, onBackPressed)
                }
            }
        }
    }

    // File info footer
    FileInfoPanel(file)
}

@Composable
fun ImageViewerContent(bitmap: Bitmap) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale *= zoomChange
        offset += offsetChange
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y,
                transformOrigin = TransformOrigin.Center
            )
            .transformable(transformableState),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun TextViewerContent(content: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Line numbers and code
        val lines = content.split("\n")
        lines.forEachIndexed { index, line ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${index + 1}",
                    modifier = Modifier.width(30.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    line,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun VideoViewerContent(file: File) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PlayCircle,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "Video Player",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            file.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = { /* TODO: Open in external player */ },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Open in Player")
        }
    }
}

@Composable
fun AudioViewerContent(file: File) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "Audio File",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            file.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = { /* TODO: Open in external player */ },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Open in Player")
        }
    }
}

@Composable
fun PdfViewerContent(file: File) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "PDF Document",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            file.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = { /* TODO: Open in external PDF viewer */ },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Open PDF")
        }
    }
}

@Composable
fun ErrorViewerContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            "Cannot Open File",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Retry")
        }
    }
}

@Composable
fun FileInfoPanel(file: File) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Name:", style = MaterialTheme.typography.bodySmall)
            Text(file.name, style = MaterialTheme.typography.bodySmall)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Size:", style = MaterialTheme.typography.bodySmall)
            Text(formatFileSize(file.length()), style = MaterialTheme.typography.bodySmall)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Type:", style = MaterialTheme.typography.bodySmall)
            Text(file.extension.ifEmpty { "Unknown" }, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun loadFileContent(context: Context, file: File): FileViewerState {
    return try {
        when {
            file.extension.matches("""(?i)(jpg|jpeg|png|gif|webp)""".toRegex()) -> {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    FileViewerState.ImageViewer(bitmap)
                } else {
                    FileViewerState.Error("Cannot load image")
                }
            }
            file.extension.matches("""(?i)(txt|log|csv|json|xml|html|css|js|kt|java)""".toRegex()) -> {
                val content = file.readText()
                FileViewerState.TextViewer(content)
            }
            file.extension.matches("""(?i)(mp4|avi|mkv|mov|flv|webm)""".toRegex()) -> {
                FileViewerState.VideoViewer(file)
            }
            file.extension.matches("""(?i)(mp3|wav|aac|flac|ogg|m4a)""".toRegex()) -> {
                FileViewerState.AudioViewer(file)
            }
            file.extension.matches("""(?i)(pdf)""".toRegex()) -> {
                FileViewerState.PdfViewer(file)
            }
            else -> {
                FileViewerState.Error("File type not supported for preview")
            }
        }
    } catch (e: Exception) {
        FileViewerState.Error(e.message ?: "Unknown error")
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
