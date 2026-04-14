package com.cylonid.nativealpha.ui.screens

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.widget.MediaController
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.cylonid.nativealpha.manager.FileViewerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalFileViewerScreen(
    filePath: String,
    onBackPressed: () -> Unit,
    viewModel: UniversalFileViewerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val file = File(filePath)
    val mimeType = getMimeType(file.extension)

    LaunchedEffect(filePath) {
        viewModel.loadFile(file)
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
                    // Share button
                    IconButton(onClick = {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = mimeType ?: "*/*"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share file"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }

                    // Open externally button
                    IconButton(onClick = {
                        FileViewerManager(context).openWithExternalApp(file, mimeType)
                    }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open externally")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                mimeType?.startsWith("image/") == true -> {
                    ImageViewer(file = file)
                }
                mimeType?.startsWith("video/") == true -> {
                    VideoViewer(file = file)
                }
                mimeType?.startsWith("audio/") == true -> {
                    AudioPlayer(file = file)
                }
                mimeType == "application/pdf" -> {
                    PdfViewer(file = file)
                }
                mimeType?.startsWith("text/") == true || file.extension.lowercase() in listOf("txt", "log", "csv", "json", "xml", "html", "md") -> {
                    TextViewer(file = file)
                }
                else -> {
                    // Unsupported file type
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            contentDescription = "File",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cannot preview this file type internally",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "File: ${file.name}\nType: ${mimeType ?: "Unknown"}\nSize: ${formatFileSize(file.length())}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            FileViewerManager(context).openWithExternalApp(file, mimeType)
                        }) {
                            Text("Open with External App")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageViewer(file: File) {
    val bitmap = remember(file) {
        BitmapFactory.decodeFile(file.absolutePath)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Image",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Text("Failed to load image")
    }
}

@Composable
fun VideoViewer(file: File) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            android.widget.VideoView(ctx).apply {
                setVideoURI(file.toUri())
                setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                setOnPreparedListener { mp ->
                    mp.isLooping = false
                    start()
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun AudioPlayer(file: File) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(file) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, file.toUri())
            prepare()
            setOnCompletionListener {
                isPlaying = false
            }
        }

        onDispose {
            mediaPlayer?.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = "Audio",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = file.nameWithoutExtension,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    mediaPlayer?.let { mp ->
                        if (isPlaying) {
                            mp.pause()
                        } else {
                            mp.start()
                        }
                        isPlaying = !isPlaying
                    }
                }
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun PdfViewer(file: File) {
    // For now, show a placeholder. Full PDF rendering would require additional libraries
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PictureAsPdf,
            contentDescription = "PDF",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "PDF Viewer",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Full PDF rendering requires additional setup",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TextViewer(file: File) {
    val text = remember(file) {
        try {
            file.readText()
        } catch (e: Exception) {
            "Failed to read file: ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = file.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        )
    }
}

private fun getMimeType(extension: String): String? {
    return when (extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "mp4" -> "video/mp4"
        "avi" -> "video/avi"
        "mkv" -> "video/x-matroska"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "html", "htm" -> "text/html"
        "json" -> "application/json"
        "xml" -> "text/xml"
        "csv" -> "text/csv"
        "md" -> "text/markdown"
        "zip" -> "application/zip"
        "rar" -> "application/x-rar-compressed"
        "7z" -> "application/x-7z-compressed"
        "apk" -> "application/vnd.android.package-archive"
        else -> null
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes == 0L) return "0 B"
    val k = 1024.0
    val sizes = arrayOf("B", "KB", "MB", "GB", "TB")
    val i = (Math.log(bytes.toDouble()) / Math.log(k)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(k, i.toDouble()), sizes[i])
}

@HiltViewModel
class UniversalFileViewerViewModel @Inject constructor() : ViewModel() {

    var currentFile by mutableStateOf<File?>(null)
        private set

    fun loadFile(file: File) {
        currentFile = file
        // Additional loading logic can be added here
    }
}