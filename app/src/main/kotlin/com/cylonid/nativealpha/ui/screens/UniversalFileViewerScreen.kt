package com.cylonid.nativealpha.ui.screens

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.widget.MediaController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.cylonid.nativealpha.manager.FileViewerManager
import com.github.barteksc.pdfviewer.PDFView
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
                }                mimeType == "text/html" || file.extension.lowercase() == "htm" -> {
                    HtmlViewer(file = file)
                }
                mimeType == "application/zip" || mimeType == "application/x-rar-compressed" || mimeType == "application/x-7z-compressed" -> {
                    ArchiveViewer(file = file)
                }
                mimeType == "application/vnd.android.package-archive" -> {
                    ApkInfoViewer(file = file)
                }                else -> {
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
                            try {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val openIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, mimeType ?: "*/*")
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(openIntent)
                            } catch (e: Exception) { e.printStackTrace() }
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
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offset += pan
                    }
                }
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Image",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )

            // Zoom controls
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { scale = (scale * 1.2f).coerceAtMost(5f) },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = { scale = (scale / 1.2f).coerceAtLeast(0.5f) },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.BrokenImage,
                contentDescription = "Broken image",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Failed to load image",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun VideoViewer(file: File) {
    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                android.widget.VideoView(ctx).apply {
                    setVideoURI(file.toUri())
                    setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                    setOnPreparedListener { mp ->
                        duration = mp.duration
                        mp.isLooping = false
                        start()
                        isPlaying = true
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        currentPosition = 0
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Video controls overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    // Play/pause logic would be handled by MediaController
                }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }

                Text(
                    text = formatDuration(currentPosition),
                    style = MaterialTheme.typography.bodySmall
                )

                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { /* Seek functionality */ },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.bodySmall
                )

                IconButton(onClick = { isFullscreen = !isFullscreen }) {
                    Icon(
                        if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "Exit fullscreen" else "Enter fullscreen"
                    )
                }
            }
        }
    }
}

private fun formatDuration(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
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
    AndroidView(
        factory = { context ->
            PDFView(context, null).apply {
                fromFile(file)
                    .defaultPage(0)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .load()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
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

@Composable
fun HtmlViewer(file: File) {
    val context = LocalContext.current
    val htmlContent = remember(file) {
        try {
            file.readText()
        } catch (e: Exception) {
            "<html><body><h1>Error</h1><p>Failed to read file: ${e.message}</p></body></html>"
        }
    }

    AndroidView(
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ArchiveViewer(file: File) {
    val context = LocalContext.current
    val archiveContents = remember(file) {
        try {
            getArchiveContents(file)
        } catch (e: Exception) {
            emptyList<String>()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Archive Contents: ${file.name}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (archiveContents.isEmpty()) {
            Text("Failed to read archive or archive is empty")
        } else {
            LazyColumn {
                items(archiveContents) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            contentDescription = "File",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                // Extract archive
                extractArchive(file, context)
            }) {
                Text("Extract Archive")
            }
        }
    }
}

@Composable
fun ApkInfoViewer(file: File) {
    val context = LocalContext.current
    val apkInfo = remember(file) {
        try {
            getApkInfo(context, file)
        } catch (e: Exception) {
            ApkInfo("Failed to read APK", "", "", 0L, emptyList())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "APK Information",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Package Name: ${apkInfo.packageName}", style = MaterialTheme.typography.bodyLarge)
                Text("Version: ${apkInfo.versionName} (${apkInfo.versionCode})", style = MaterialTheme.typography.bodyMedium)
                Text("Size: ${formatFileSize(apkInfo.size)}", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Permissions:", style = MaterialTheme.typography.titleMedium)
                apkInfo.permissions.forEach { permission ->
                    Text("• $permission", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                // Install APK
                installApk(context, file)
            }) {
                Text("Install APK")
            }
            Button(onClick = {
                // Share APK
                shareApk(context, file)
            }) {
                Text("Share APK")
            }
        }
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

private fun getArchiveContents(file: File): List<String> {
    return try {
        when (file.extension.lowercase()) {
            "zip" -> {
                java.util.zip.ZipFile(file).use { zip ->
                    zip.entries().asSequence().map { it.name }.toList()
                }
            }
            else -> emptyList() // For now, only ZIP supported
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun extractArchive(file: File, context: Context) {
    // Simple extraction to downloads folder
    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
    val extractDir = File(downloadsDir, "WAOS_Extracted/${file.nameWithoutExtension}")

    try {
        extractDir.mkdirs()
        when (file.extension.lowercase()) {
            "zip" -> {
                java.util.zip.ZipFile(file).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        if (!entry.isDirectory) {
                            val outputFile = File(extractDir, entry.name)
                            outputFile.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                outputFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            }
        }
        // Show success message
        android.widget.Toast.makeText(context, "Archive extracted to ${extractDir.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Failed to extract archive: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

data class ApkInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val size: Long,
    val permissions: List<String>
)

private fun getApkInfo(context: Context, file: File): ApkInfo {
    val pm = context.packageManager
    val packageInfo = pm.getPackageArchiveInfo(file.absolutePath, android.content.pm.PackageManager.GET_PERMISSIONS)

    return if (packageInfo != null) {
        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        ApkInfo(
            packageName = packageInfo.packageName ?: "Unknown",
            versionName = packageInfo.versionName ?: "Unknown",
            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            },
            size = file.length(),
            permissions = permissions
        )
    } else {
        ApkInfo("Failed to read APK info", "", "", file.length(), emptyList())
    }
}

private fun installApk(context: Context, file: File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun shareApk(context: Context, file: File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/vnd.android.package-archive"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share APK"))
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