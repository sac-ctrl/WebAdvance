package com.cylonid.nativealpha.ui.screens

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.cylonid.nativealpha.manager.FileViewerManager
import com.cylonid.nativealpha.ui.theme.*
import com.github.barteksc.pdfviewer.PDFView
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ============================================================
// Viewer "theater mode" palette — locked to dark regardless of
// the app theme, mirroring how Photos / VLC / PDF readers behave.
// This guarantees readable controls and fixes the previous
// "white toolbar in PDF viewer" bug that occurred whenever the
// app theme was Light (theme-driven surface tints went white,
// blending into white PDF pages).
// ============================================================
private val VBg          = Color(0xFF0A0E17)
private val VBgGradient  = Color(0xFF101727)
private val VSurface     = Color(0xFF161F33)
private val VSurfaceHi   = Color(0xFF1F2A44)
private val VSurfaceGlow = Color(0xFF263354)
private val VBorder      = Color(0xFF2A3756)
private val VBorderHi    = Color(0xFF3B4B72)
private val VAccent      = Color(0xFF22D3EE)
private val VAccent2     = Color(0xFFA78BFA)
private val VAccent3     = Color(0xFFF472B6)
private val VOk          = Color(0xFF34D399)
private val VWarn        = Color(0xFFFBBF24)
private val VTextHi      = Color(0xFFF1F5F9)
private val VTextMd      = Color(0xFFCBD5E1)
private val VTextLo      = Color(0xFF8B97B5)

private val ViewerBgBrush: Brush
    get() = Brush.verticalGradient(listOf(VBg, VBgGradient, VBg))

@Composable
private fun Modifier.viewerIconButtonStyle(enabled: Boolean = true, selected: Boolean = false): Modifier =
    this
        .size(36.dp)
        .background(
            when {
                selected -> VAccent.copy(alpha = 0.18f)
                enabled  -> VSurfaceHi
                else     -> VSurface.copy(alpha = 0.6f)
            },
            RoundedCornerShape(10.dp)
        )
        .border(
            1.dp,
            when {
                selected -> VAccent.copy(alpha = 0.55f)
                enabled  -> VBorder
                else     -> VBorder.copy(alpha = 0.4f)
            },
            RoundedCornerShape(10.dp)
        )

@Composable
private fun Modifier.audioControlButtonStyle(selected: Boolean = false): Modifier =
    this
        .size(48.dp)
        .background(
            if (selected) VAccent.copy(alpha = 0.18f) else VSurfaceHi,
            RoundedCornerShape(16.dp)
        )
        .border(
            1.dp,
            if (selected) VAccent.copy(alpha = 0.55f) else VBorder,
            RoundedCornerShape(16.dp)
        )

@Composable
private fun ChipPill(label: String, value: String, accent: Color = VAccent) {
    Row(
        modifier = Modifier
            .background(VSurface, RoundedCornerShape(999.dp))
            .border(1.dp, VBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(accent, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 10.sp, color = VTextLo, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(6.dp))
        Text(value, fontSize = 11.sp, color = VTextHi, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalFileViewerScreen(
    filePath: String,
    onBackPressed: () -> Unit,
    viewModel: UniversalFileViewerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val initialFile = remember(filePath) { File(filePath) }
    val folderFiles = remember(filePath) { getFolderFiles(initialFile) }
    var activeFile by remember(filePath) { mutableStateOf(initialFile) }
    var activeFileIndex by remember(filePath) {
        mutableStateOf(
            folderFiles.indexOfFirst { it.absolutePath == initialFile.absolutePath }.coerceAtLeast(0)
        )
    }
    var darkMode by remember { mutableStateOf(false) }
    var showInfoPanel by remember { mutableStateOf(true) }
    val mimeType = getMimeType(activeFile.extension)

    LaunchedEffect(activeFile) {
        viewModel.loadFile(activeFile)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ViewerBgBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ---------- Modern top app bar (two rows) ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(
                        Brush.verticalGradient(
                            listOf(VBg, VBg.copy(alpha = 0.96f), VBg.copy(alpha = 0.88f))
                        )
                    )
            ) {
                Column {
                    // Row 1: Back · File title + meta · Info toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackPressed,
                            modifier = Modifier
                                .size(40.dp)
                                .background(VSurfaceHi, RoundedCornerShape(12.dp))
                                .border(1.dp, VBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                null,
                                tint = VTextHi,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))

                        // File-type indicator dot — color-coded per kind
                        val typeColor = when {
                            mimeType?.startsWith("image/") == true -> VAccent
                            mimeType?.startsWith("video/") == true -> VAccent3
                            mimeType?.startsWith("audio/") == true -> VAccent2
                            mimeType == "application/pdf"          -> VWarn
                            mimeType?.startsWith("text/") == true  -> VOk
                            else                                   -> VTextLo
                        }
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(typeColor, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                activeFile.name,
                                color = VTextHi,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${activeFileIndex + 1}/${folderFiles.size}",
                                    color = typeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "  •  ${
                                        getMimeType(activeFile.extension)?.substringAfter("/")?.uppercase()
                                            ?: activeFile.extension.uppercase().ifBlank { "FILE" }
                                    }  •  ${formatFileSize(activeFile.length())}",
                                    color = VTextLo,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = { showInfoPanel = !showInfoPanel },
                            modifier = Modifier.viewerIconButtonStyle(selected = showInfoPanel)
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                null,
                                tint = if (showInfoPanel) VAccent else VTextMd,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Row 2: horizontally scrollable action pill bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (activeFileIndex > 0) {
                                    activeFileIndex -= 1
                                    activeFile = folderFiles[activeFileIndex]
                                }
                            },
                            enabled = activeFileIndex > 0,
                            modifier = Modifier.viewerIconButtonStyle(activeFileIndex > 0)
                        ) {
                            Icon(
                                Icons.Rounded.SkipPrevious, null,
                                tint = if (activeFileIndex > 0) VAccent else VTextLo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (activeFileIndex < folderFiles.lastIndex) {
                                    activeFileIndex += 1
                                    activeFile = folderFiles[activeFileIndex]
                                }
                            },
                            enabled = activeFileIndex < folderFiles.lastIndex,
                            modifier = Modifier.viewerIconButtonStyle(activeFileIndex < folderFiles.lastIndex)
                        ) {
                            Icon(
                                Icons.Rounded.SkipNext, null,
                                tint = if (activeFileIndex < folderFiles.lastIndex) VAccent else VTextLo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        // subtle vertical divider
                        Box(
                            Modifier
                                .height(22.dp)
                                .width(1.dp)
                                .background(VBorder)
                        )
                        IconButton(
                            onClick = {
                                val uri = FileProvider.getUriForFile(
                                    context, "${context.packageName}.fileprovider", activeFile
                                )
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = mimeType ?: "*/*"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Share file"))
                            },
                            modifier = Modifier.viewerIconButtonStyle()
                        ) {
                            Icon(Icons.Rounded.Share, null, tint = VAccent2, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { FileViewerManager(context).openWithExternalApp(activeFile, mimeType) },
                            modifier = Modifier.viewerIconButtonStyle()
                        ) {
                            Icon(Icons.Rounded.OpenInNew, null, tint = VTextMd, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("File Path", activeFile.absolutePath)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "Path copied", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.viewerIconButtonStyle()
                        ) {
                            Icon(Icons.Rounded.ContentCopy, null, tint = VTextMd, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                viewModel.duplicateFile(activeFile)?.let {
                                    android.widget.Toast.makeText(context, "Duplicated: ${it.name}", android.widget.Toast.LENGTH_SHORT).show()
                                } ?: android.widget.Toast.makeText(context, "Failed to duplicate", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.viewerIconButtonStyle()
                        ) {
                            Icon(Icons.Rounded.FileCopy, null, tint = VTextMd, modifier = Modifier.size(16.dp))
                        }
                        Box(
                            Modifier
                                .height(22.dp)
                                .width(1.dp)
                                .background(VBorder)
                        )
                        IconButton(
                            onClick = { darkMode = !darkMode },
                            modifier = Modifier.viewerIconButtonStyle(selected = darkMode)
                        ) {
                            Icon(
                                if (darkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                null,
                                tint = if (darkMode) VAccent else VTextMd,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Hairline accent divider
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        VBorder.copy(alpha = 0.0f),
                                        VAccent.copy(alpha = 0.45f),
                                        VAccent2.copy(alpha = 0.45f),
                                        VBorder.copy(alpha = 0.0f)
                                    )
                                )
                            )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (darkMode) Color.Black else VBg)
            ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    mimeType == "image/svg+xml" -> {
                        SvgViewer(file = activeFile)
                    }
                    mimeType?.startsWith("image/") == true -> {
                        ImageViewer(
                            file = activeFile,
                            folderFiles = folderFiles,
                            onOpenPrevious = {
                                if (activeFileIndex > 0) {
                                    activeFileIndex -= 1
                                    activeFile = folderFiles[activeFileIndex]
                                }
                            },
                            onOpenNext = {
                                if (activeFileIndex < folderFiles.lastIndex) {
                                    activeFileIndex += 1
                                    activeFile = folderFiles[activeFileIndex]
                                }
                            }
                        )
                    }
                    mimeType?.startsWith("video/") == true -> {
                        VideoViewer(
                            file = activeFile,
                            folderFiles = folderFiles,
                            onOpenPrevious = {
                                if (activeFileIndex > 0) {
                                    activeFileIndex -= 1
                                    activeFile = folderFiles[activeFileIndex]
                                }
                            },
                            onOpenNext = {
                                if (activeFileIndex < folderFiles.lastIndex) {
                                    activeFileIndex += 1
                                    activeFile = folderFiles[activeFileIndex]
                                }
                            },
                            viewModel = viewModel
                        )
                    }
                    mimeType?.startsWith("audio/") == true -> {
                        AudioPlayer(
                            file = activeFile,
                            folderFiles = folderFiles,
                            onOpenPrevious = {
                                if (activeFileIndex > 0) {
                                    activeFileIndex -= 1
                                    activeFile = folderFiles[activeFileIndex]
                                }
                            },
                            onOpenNext = {
                                if (activeFileIndex < folderFiles.lastIndex) {
                                    activeFileIndex += 1
                                    activeFile = folderFiles[activeFileIndex]
                                }
                            },
                            viewModel = viewModel
                        )
                    }
                    mimeType == "application/pdf" -> {
                        PdfViewer(file = activeFile)
                    }
                    mimeType == "text/html" || mimeType == "multipart/related" || activeFile.extension.lowercase() == "htm" -> {
                        HtmlViewer(file = activeFile)
                    }
                    mimeType?.startsWith("text/") == true || activeFile.extension.lowercase() in listOf("txt", "log", "csv", "json", "xml", "md") -> {
                        TextViewer(file = activeFile)
                    }
                    mimeType == "application/zip" || mimeType == "application/x-rar-compressed" || mimeType == "application/x-7z-compressed" || mimeType == "application/x-tar" -> {
                        ArchiveViewer(file = activeFile)
                    }
                    mimeType == "application/vnd.android.package-archive" -> {
                        ApkInfoViewer(file = activeFile)
                    }
                    else -> {
                        UnsupportedFileViewer(activeFile, mimeType)
                    }
                }

                if (showInfoPanel) {
                    FileInfoPanel(
                        file = activeFile,
                        mimeType = mimeType,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }
            }
        }
    }
}

@Composable
fun UnsupportedFileViewer(file: File, mimeType: String?) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ViewerBgBrush)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Brush.radialGradient(listOf(VAccent.copy(alpha = 0.18f), Color.Transparent)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.InsertDriveFile,
                contentDescription = "File",
                modifier = Modifier.size(72.dp),
                tint = VAccent
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "No internal preview available",
            color = VTextHi,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "This file type doesn't have a built-in viewer yet. Open it with an external app instead.",
            color = VTextLo,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VSurface, RoundedCornerShape(16.dp))
                .border(1.dp, VBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoRow("Name", file.name)
            InfoRow("Type", mimeType ?: "Unknown")
            InfoRow("Size", formatFileSize(file.length()))
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                try {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val openIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType ?: "*/*")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(openIntent)
                } catch (e: Exception) { e.printStackTrace() }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = VAccent,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open with External App", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label.uppercase(),
            color = VTextLo,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(96.dp)
        )
        Text(
            value,
            color = VTextHi,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FileInfoPanel(file: File, mimeType: String?, modifier: Modifier = Modifier) {
    val permissions = remember(file) { getFilePermissions(file) }
    val exifData = remember(file) { if (mimeType?.startsWith("image/") == true) getImageExifData(file) else emptyMap() }

    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(VSurface.copy(alpha = 0.98f), VSurfaceHi.copy(alpha = 0.99f))
                ),
                RoundedCornerShape(18.dp)
            )
            .border(1.dp, VBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(VAccent.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                    .border(1.dp, VAccent.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Info, null, tint = VAccent, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("File Information", color = VTextHi, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(10.dp))

        // Quick chip strip
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChipPill("SIZE", formatFileSize(file.length()), VAccent)
            ChipPill("TYPE", mimeType?.substringAfter("/")?.uppercase() ?: file.extension.uppercase().ifBlank { "—" }, VAccent2)
            ChipPill("PERM", permissions, VOk)
        }

        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoRow("Name", file.name)
            InfoRow("Mime", mimeType ?: "Unknown")
            InfoRow("Modified", formatModifiedDate(file.lastModified()))
            InfoRow("Path", file.absolutePath)
        }

        if (exifData.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(VBorder)
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CameraAlt, null, tint = VAccent3, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text("EXIF Metadata", color = VTextHi, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                exifData.forEach { (key, value) ->
                    InfoRow(key, value)
                }
            }
        }
    }
}

private fun getFolderFiles(file: File): List<File> {
    return file.parentFile?.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))?.toList()
        ?: emptyList()
}

private fun getFilePermissions(file: File): String {
    return buildString {
        append(if (file.canRead()) "r" else "-")
        append(if (file.canWrite()) "w" else "-")
        append(if (file.canExecute()) "x" else "-")
    }
}

private fun getImageExifData(file: File): Map<String, String> {
    return try {
        ExifInterface(file.absolutePath).run {
            listOf(
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LONGITUDE
            ).mapNotNull { tag ->
                getAttribute(tag)?.let { value ->
                    tag to value
                }
            }.toMap()
        }
    } catch (e: Exception) {
        emptyMap()
    }
}

private fun createColorMatrix(filterMode: String, brightness: Float, contrast: Float): ColorMatrix {
    val baseMatrix = floatArrayOf(
        contrast, 0f, 0f, 0f, brightness * 255f,
        0f, contrast, 0f, 0f, brightness * 255f,
        0f, 0f, contrast, 0f, brightness * 255f,
        0f, 0f, 0f, 1f, 0f
    )

    val matrix = when (filterMode) {
        "Grayscale" -> {
            val gray = ColorMatrix().apply { setToSaturation(0f) }
            gray
        }
        "Sepia" -> ColorMatrix(floatArrayOf(
            0.393f * contrast, 0.769f * contrast, 0.189f * contrast, 0f, brightness * 255f,
            0.349f * contrast, 0.686f * contrast, 0.168f * contrast, 0f, brightness * 255f,
            0.272f * contrast, 0.534f * contrast, 0.131f * contrast, 0f, brightness * 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        else -> ColorMatrix(baseMatrix)
    }

    return matrix
}

@Composable
fun ImageViewer(
    file: File,
    folderFiles: List<File>,
    onOpenPrevious: () -> Unit,
    onOpenNext: () -> Unit
) {
    val bitmap = remember(file) { BitmapFactory.decodeFile(file.absolutePath) }
    val exifData = remember(file) { getImageExifData(file) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableStateOf(0f) }
    var brightness by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(1f) }
    var filterMode by remember { mutableStateOf("Normal") }
    var slideshowActive by remember { mutableStateOf(false) }

    LaunchedEffect(slideshowActive) {
        if (slideshowActive && folderFiles.size > 1) {
            while (isActive) {
                delay(3500)
                onOpenNext()
            }
        }
    }

    if (bitmap == null) {
        BrokenImageViewer()
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Image",
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 6f)
                        offset += pan
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2f
                        offset = Offset.Zero
                    })
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                    rotationZ = rotation
                ),
            colorFilter = ColorFilter.colorMatrix(createColorMatrix(filterMode, brightness, contrast))
        )

        // Side action rail (prev / next / slideshow) — glass pills
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onOpenPrevious, modifier = Modifier.audioControlButtonStyle()) {
                Icon(Icons.Rounded.SkipPrevious, null, tint = VTextHi, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onOpenNext, modifier = Modifier.audioControlButtonStyle()) {
                Icon(Icons.Rounded.SkipNext, null, tint = VTextHi, modifier = Modifier.size(22.dp))
            }
            IconButton(
                onClick = { slideshowActive = !slideshowActive },
                modifier = Modifier.audioControlButtonStyle(selected = slideshowActive)
            ) {
                Icon(
                    Icons.Rounded.Slideshow, null,
                    tint = if (slideshowActive) VAccent else VTextHi,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        var controlsExpanded by remember { mutableStateOf(false) }
        val controlPanelShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VSurface.copy(alpha = 0.96f), controlPanelShape)
                    .border(1.dp, VBorder, controlPanelShape)
                    .clickable { controlsExpanded = !controlsExpanded }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Tune, null, tint = VAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (controlsExpanded) "Hide image controls" else "Image controls",
                            color = VTextHi,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        if (controlsExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                        contentDescription = null,
                        tint = VTextMd,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (controlsExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VSurfaceHi.copy(alpha = 0.98f))
                        .border(1.dp, VBorder)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(VSurfaceGlow, RoundedCornerShape(12.dp))
                                .border(1.dp, VBorder, RoundedCornerShape(12.dp))
                                .clickable { rotation = (rotation + 90f) % 360f }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.ScreenRotation, null, tint = VAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Rotate", color = VTextHi, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(VSurfaceGlow, RoundedCornerShape(12.dp))
                                .border(1.dp, VBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    filterMode = when (filterMode) {
                                        "Normal" -> "Grayscale"
                                        "Grayscale" -> "Sepia"
                                        else -> "Normal"
                                    }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Tune, null, tint = VAccent2, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(filterMode, color = VTextHi, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Brightness6, null, tint = VWarn, modifier = Modifier.size(16.dp))
                        Text("Brightness", color = VTextMd, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            valueRange = -0.5f..0.5f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = VWarn,
                                activeTrackColor = VWarn,
                                inactiveTrackColor = VSurfaceGlow
                            )
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Contrast, null, tint = VAccent3, modifier = Modifier.size(16.dp))
                        Text("Contrast", color = VTextMd, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                        Slider(
                            value = contrast,
                            onValueChange = { contrast = it },
                            valueRange = 0.5f..2f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = VAccent3,
                                activeTrackColor = VAccent3,
                                inactiveTrackColor = VSurfaceGlow
                            )
                        )
                    }

                    if (exifData.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "EXIF • ${exifData.entries.joinToString { "${it.key}=${it.value}" }}",
                            color = VTextLo,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrokenImageViewer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ViewerBgBrush)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Brush.radialGradient(listOf(VAccent3.copy(alpha = 0.2f), Color.Transparent)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.BrokenImage,
                contentDescription = "Broken image",
                modifier = Modifier.size(72.dp),
                tint = VAccent3
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Failed to load image",
            color = VTextHi,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "The file may be corrupted, in an unsupported format, or unreadable.",
            color = VTextLo,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SvgViewer(file: File) {
    val svgContent = remember(file) {
        try {
            file.readText()
        } catch (e: Exception) {
            "<svg><text x=\"10\" y=\"20\" fill=\"red\">Failed to load SVG</text></svg>"
        }
    }

    AndroidView(
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                loadDataWithBaseURL(null, svgContent, "image/svg+xml", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun VideoViewer(
    file: File,
    folderFiles: List<File>,
    onOpenPrevious: () -> Unit,
    onOpenNext: () -> Unit,
    viewModel: UniversalFileViewerViewModel
) {
    val context = LocalContext.current
    var videoView by remember { mutableStateOf<android.widget.VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(1) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var screenshotMessage by remember { mutableStateOf<String?>(null) }
    val savedPosition = remember(file) { viewModel.getSavedPosition(file) }

    LaunchedEffect(videoView, isPlaying) {
        if (isPlaying) {
            videoView?.start()
        } else {
            videoView?.pause()
        }
    }

    LaunchedEffect(videoView) {
        while (isActive) {
            delay(250)
            val position = videoView?.currentPosition ?: 0
            if (position != currentPosition) currentPosition = position
            duration = videoView?.duration ?: duration
        }
    }

    DisposableEffect(videoView) {
        onDispose {
            if (currentPosition > 0) {
                viewModel.savePosition(file, currentPosition)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                android.widget.VideoView(ctx).apply {
                    videoView = this
                    setVideoURI(file.toUri())
                    setOnPreparedListener { mp ->
                        duration = mp.duration
                        mp.isLooping = false
                        if (savedPosition > 0 && savedPosition < duration) {
                            seekTo(savedPosition)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                        }
                        start()
                        isPlaying = true
                    }
                    setOnCompletionListener {
                        isPlaying = false
                    }
                }
            },
            update = { view ->
                if (isPlaying && !view.isPlaying) view.start()
                if (!isPlaying && view.isPlaying) view.pause()
                // Playback speed not supported for VideoView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top scrim for visual depth above the video
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )

        // ---------- Modern bottom control overlay ----------
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), VBg.copy(alpha = 0.96f))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Time + scrub row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    formatDuration(currentPosition),
                    color = VTextHi,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = currentPosition.toFloat().coerceIn(0f, duration.toFloat()),
                    onValueChange = {
                        currentPosition = it.toInt()
                        videoView?.seekTo(currentPosition)
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = VAccent,
                        activeTrackColor = VAccent,
                        inactiveTrackColor = VSurfaceGlow
                    )
                )
                Text(
                    formatDuration(duration),
                    color = VTextLo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Main playback row: prev / -10 / play-pause / +10 / next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (folderFiles.isNotEmpty()) onOpenPrevious() },
                    modifier = Modifier.audioControlButtonStyle()
                ) {
                    Icon(Icons.Rounded.SkipPrevious, null, tint = VTextHi, modifier = Modifier.size(22.dp))
                }
                IconButton(
                    onClick = { videoView?.seekTo((currentPosition - 10000).coerceAtLeast(0)) },
                    modifier = Modifier.audioControlButtonStyle()
                ) {
                    Icon(Icons.Rounded.Replay10, null, tint = VTextMd, modifier = Modifier.size(22.dp))
                }
                // Big play/pause
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.radialGradient(listOf(VAccent, VAccent2)),
                            CircleShape
                        )
                        .border(2.dp, VAccent.copy(alpha = 0.6f), CircleShape)
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                IconButton(
                    onClick = { videoView?.seekTo((currentPosition + 10000).coerceAtMost(duration)) },
                    modifier = Modifier.audioControlButtonStyle()
                ) {
                    Icon(Icons.Rounded.Forward10, null, tint = VTextMd, modifier = Modifier.size(22.dp))
                }
                IconButton(
                    onClick = { if (folderFiles.isNotEmpty()) onOpenNext() },
                    modifier = Modifier.audioControlButtonStyle()
                ) {
                    Icon(Icons.Rounded.SkipNext, null, tint = VTextHi, modifier = Modifier.size(22.dp))
                }
            }

            // Speed + capture row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.Speed, null, tint = VAccent2, modifier = Modifier.size(16.dp))
                Slider(
                    value = playbackSpeed,
                    onValueChange = {
                        playbackSpeed = it
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                val params = PlaybackParams().setSpeed(it)
                                // VideoView doesn't expose its MediaPlayer; speed mostly applies on prepare
                            } catch (_: Exception) { }
                        }
                    },
                    valueRange = 0.5f..2f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = VAccent2,
                        activeTrackColor = VAccent2,
                        inactiveTrackColor = VSurfaceGlow
                    )
                )
                Box(
                    modifier = Modifier
                        .background(VAccent2.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .border(1.dp, VAccent2.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        String.format("%.1fx", playbackSpeed),
                        color = VAccent2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(
                    onClick = {
                        captureVideoFrame(context, file, currentPosition)?.let {
                            screenshotMessage = "Frame saved: ${it.name}"
                        } ?: run { screenshotMessage = "Capture failed" }
                    },
                    modifier = Modifier.viewerIconButtonStyle()
                ) {
                    Icon(Icons.Rounded.PhotoCamera, null, tint = VAccent3, modifier = Modifier.size(16.dp))
                }
            }

            screenshotMessage?.let { message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VOk.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                        .border(1.dp, VOk.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = VOk, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(message, color = VTextHi, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun captureVideoFrame(context: Context, file: File, positionMs: Int): File? {
    return try {
        val retriever = MediaMetadataRetriever().apply { setDataSource(file.absolutePath) }
        val frame = retriever.getFrameAtTime(positionMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        retriever.release()
        frame?.let {
            val output = File(context.cacheDir, "video_frame_${file.nameWithoutExtension}_${positionMs}.png")
            output.outputStream().use { stream -> it.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, stream) }
            output
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun AudioPlayer(
    file: File,
    folderFiles: List<File>,
    onOpenPrevious: () -> Unit,
    onOpenNext: () -> Unit,
    viewModel: UniversalFileViewerViewModel
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var duration by remember { mutableStateOf(0) }
    var currentPosition by remember { mutableStateOf(0) }
    var shuffle by remember { mutableStateOf(false) }
    var repeatOne by remember { mutableStateOf(false) }
    val metadata = remember(file) { loadAudioMetadata(file) }
    val albumArt = remember(file) { loadAudioAlbumArt(file) }

    DisposableEffect(file) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, file.toUri())
            prepare()
            duration = this.duration
            setOnCompletionListener {
                isPlaying = false
                if (repeatOne) {
                    seekTo(0)
                    start()
                    isPlaying = true
                }
            }
        }

        onDispose {
            viewModel.savePosition(file, currentPosition)
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(mediaPlayer, isPlaying) {
        while (isActive) {
            delay(200)
            currentPosition = mediaPlayer?.currentPosition ?: 0
            if (mediaPlayer?.isPlaying == true && !isPlaying) {
                isPlaying = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(VBg, VBgGradient, VBg)
                )
            )
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // ---------- Hero album art ----------
        Box(
            modifier = Modifier
                .size(220.dp)
                .background(
                    Brush.radialGradient(
                        listOf(VAccent2.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (albumArt != null) {
                Image(
                    bitmap = albumArt.asImageBitmap(),
                    contentDescription = "Album art",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(2.dp, VBorderHi, RoundedCornerShape(28.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(VAccent2.copy(alpha = 0.4f), VAccent.copy(alpha = 0.3f))
                            ),
                            RoundedCornerShape(28.dp)
                        )
                        .border(2.dp, VBorderHi, RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = "Audio",
                        modifier = Modifier.size(96.dp),
                        tint = VTextHi.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // ---------- Title / artist ----------
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                metadata.title ?: file.nameWithoutExtension,
                color = VTextHi,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                metadata.artist ?: metadata.album ?: "Unknown artist",
                color = VTextMd,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        // ---------- Meta chips ----------
        if (metadata.album != null || metadata.genre != null || metadata.year != null) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metadata.album?.let { ChipPill("ALBUM", it, VAccent) }
                metadata.genre?.let { ChipPill("GENRE", it, VAccent2) }
                metadata.year?.let { ChipPill("YEAR", it, VAccent3) }
            }
        }

        // ---------- Progress slider + time labels ----------
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = currentPosition.toFloat().coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
                onValueChange = {
                    currentPosition = it.toInt()
                    mediaPlayer?.seekTo(currentPosition)
                },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = VAccent,
                    activeTrackColor = VAccent,
                    inactiveTrackColor = VSurfaceGlow
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(currentPosition), color = VTextHi, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(formatDuration(duration), color = VTextLo, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }

        // ---------- Main control row ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = { shuffle = !shuffle },
                modifier = Modifier.audioControlButtonStyle(selected = shuffle)
            ) {
                Icon(Icons.Rounded.Shuffle, null,
                    tint = if (shuffle) VAccent else VTextMd, modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = onOpenPrevious,
                modifier = Modifier.audioControlButtonStyle()
            ) {
                Icon(Icons.Rounded.SkipPrevious, null, tint = VTextHi, modifier = Modifier.size(24.dp))
            }
            // Big circular play/pause
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(listOf(VAccent, VAccent2)),
                        CircleShape
                    )
                    .border(2.dp, VAccent.copy(alpha = 0.6f), CircleShape)
                    .clickable {
                        if (isPlaying) mediaPlayer?.pause() else mediaPlayer?.start()
                        isPlaying = !isPlaying
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
            IconButton(
                onClick = onOpenNext,
                modifier = Modifier.audioControlButtonStyle()
            ) {
                Icon(Icons.Rounded.SkipNext, null, tint = VTextHi, modifier = Modifier.size(24.dp))
            }
            IconButton(
                onClick = { repeatOne = !repeatOne },
                modifier = Modifier.audioControlButtonStyle(selected = repeatOne)
            ) {
                Icon(Icons.Rounded.Repeat, null,
                    tint = if (repeatOne) VAccent else VTextMd, modifier = Modifier.size(20.dp))
            }
        }

        // ---------- Secondary control row (-10 / +10 / equalizer placeholder) ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = { mediaPlayer?.seekTo((currentPosition - 10000).coerceAtLeast(0)) },
                modifier = Modifier.audioControlButtonStyle()
            ) {
                Icon(Icons.Rounded.Replay10, null, tint = VTextMd, modifier = Modifier.size(22.dp))
            }
            IconButton(
                onClick = { mediaPlayer?.seekTo((currentPosition + 10000).coerceAtMost(duration)) },
                modifier = Modifier.audioControlButtonStyle()
            ) {
                Icon(Icons.Rounded.Forward10, null, tint = VTextMd, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

private data class AudioMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val genre: String?,
    val year: String?
)

private fun loadAudioMetadata(file: File): AudioMetadata {
    return try {
        val retriever = MediaMetadataRetriever().apply { setDataSource(file.absolutePath) }
        AudioMetadata(
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE),
            year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
        ).also { retriever.release() }
    } catch (e: Exception) {
        AudioMetadata(null, null, null, null, null)
    }
}

private fun loadAudioAlbumArt(file: File): android.graphics.Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever().apply { setDataSource(file.absolutePath) }
        val art = retriever.embeddedPicture
        retriever.release()
        art?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun PdfViewer(file: File) {
    var currentPage by remember(file) { mutableStateOf(0) }
    var pageCount by remember(file) { mutableStateOf(0) }
    var zoomFactor by remember(file) { mutableStateOf(1f) }
    var pdfView by remember(file) { mutableStateOf<PDFView?>(null) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpInput by remember { mutableStateOf("") }
    var nightMode by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1F2E))) {
        AndroidView(
            factory = { context ->
                PDFView(context, null).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#1A1F2E"))
                    pdfView = this
                    fromFile(file)
                        .defaultPage(currentPage)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .nightMode(nightMode)
                        .spacing(8)
                        .onLoad { pageCount = it }
                        .onPageChange { page, _ -> currentPage = page }
                        .load()
                }
            },
            update = { view ->
                if (view.currentPage != currentPage) view.jumpTo(currentPage, true)
                if (kotlin.math.abs(view.zoom - zoomFactor) > 0.01f) view.zoomTo(zoomFactor)
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                }
        )

        // Top floating "page X / Y" pill — gives the user instant context
        // even when the bottom controls are hidden via tap-to-toggle.
        if (controlsVisible) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .background(VSurface.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
                    .border(1.dp, VBorder, RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Description,
                    contentDescription = null,
                    tint = VWarn,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Page ${currentPage + 1} / ${pageCount.coerceAtLeast(1)}",
                    color = VTextHi,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(4.dp).background(VBorder, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(
                    String.format("%.0f%%", zoomFactor * 100),
                    color = VAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Modern bottom control card (glass surface)
        if (controlsVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(VSurface.copy(alpha = 0.95f), VSurfaceHi.copy(alpha = 0.97f))
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .border(1.dp, VBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Page navigation row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { currentPage = 0 },
                        enabled = currentPage > 0,
                        modifier = Modifier.viewerIconButtonStyle(currentPage > 0)
                    ) {
                        Icon(Icons.Rounded.FirstPage, null,
                            tint = if (currentPage > 0) VAccent else VTextLo,
                            modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { if (currentPage > 0) currentPage -= 1 },
                        enabled = currentPage > 0,
                        modifier = Modifier.viewerIconButtonStyle(currentPage > 0)
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, null,
                            tint = if (currentPage > 0) VAccent else VTextLo,
                            modifier = Modifier.size(20.dp))
                    }
                    // Tappable page-jump pill (centered, takes remaining width)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(VSurfaceGlow, RoundedCornerShape(12.dp))
                            .border(1.dp, VBorderHi, RoundedCornerShape(12.dp))
                            .clickable {
                                jumpInput = (currentPage + 1).toString()
                                showJumpDialog = true
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Page ${currentPage + 1} of ${pageCount.coerceAtLeast(1)}",
                            color = VTextHi,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = { if (currentPage < pageCount - 1) currentPage += 1 },
                        enabled = currentPage < pageCount - 1,
                        modifier = Modifier.viewerIconButtonStyle(currentPage < pageCount - 1)
                    ) {
                        Icon(Icons.Rounded.ChevronRight, null,
                            tint = if (currentPage < pageCount - 1) VAccent else VTextLo,
                            modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { currentPage = (pageCount - 1).coerceAtLeast(0) },
                        enabled = currentPage < pageCount - 1,
                        modifier = Modifier.viewerIconButtonStyle(currentPage < pageCount - 1)
                    ) {
                        Icon(Icons.Rounded.LastPage, null,
                            tint = if (currentPage < pageCount - 1) VAccent else VTextLo,
                            modifier = Modifier.size(18.dp))
                    }
                }

                // Page progress bar
                LinearProgressIndicator(
                    progress = if (pageCount > 0) (currentPage + 1).toFloat() / pageCount else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = VAccent,
                    trackColor = VSurfaceGlow
                )

                // Zoom row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { zoomFactor = (zoomFactor - 0.25f).coerceIn(1f, 4f) },
                        modifier = Modifier.viewerIconButtonStyle()
                    ) {
                        Icon(Icons.Rounded.ZoomOut, null, tint = VTextMd, modifier = Modifier.size(18.dp))
                    }
                    Slider(
                        value = zoomFactor,
                        onValueChange = { zoomFactor = it.coerceIn(1f, 4f) },
                        valueRange = 1f..4f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = VAccent,
                            activeTrackColor = VAccent,
                            inactiveTrackColor = VSurfaceGlow
                        )
                    )
                    IconButton(
                        onClick = { zoomFactor = (zoomFactor + 0.25f).coerceIn(1f, 4f) },
                        modifier = Modifier.viewerIconButtonStyle()
                    ) {
                        Icon(Icons.Rounded.ZoomIn, null, tint = VTextMd, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        modifier = Modifier
                            .background(VAccent.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                            .border(1.dp, VAccent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            String.format("%.1fx", zoomFactor),
                            color = VAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Quick action chips: Fit, Reset, Night mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PdfActionChip(
                        icon = Icons.Rounded.FitScreen,
                        label = "Fit",
                        onClick = { zoomFactor = 1f }
                    )
                    PdfActionChip(
                        icon = Icons.Rounded.Refresh,
                        label = "Reset",
                        onClick = {
                            zoomFactor = 1f
                            currentPage = 0
                        }
                    )
                    PdfActionChip(
                        icon = if (nightMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        label = if (nightMode) "Day" else "Night",
                        accent = if (nightMode) VAccent else VTextMd,
                        onClick = {
                            nightMode = !nightMode
                            pdfView?.let { view ->
                                view.fromFile(file)
                                    .defaultPage(currentPage)
                                    .enableSwipe(true)
                                    .swipeHorizontal(false)
                                    .enableDoubletap(true)
                                    .nightMode(nightMode)
                                    .spacing(8)
                                    .onLoad { pageCount = it }
                                    .onPageChange { page, _ -> currentPage = page }
                                    .load()
                            }
                        }
                    )
                }
            }
        }
    }

    // Jump-to-page dialog
    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            containerColor = VSurface,
            title = {
                Text("Jump to page", color = VTextHi, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Column {
                    Text(
                        "Enter a page number between 1 and ${pageCount.coerceAtLeast(1)}",
                        color = VTextLo, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = jumpInput,
                        onValueChange = { jumpInput = it.filter { c -> c.isDigit() }.take(6) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VAccent,
                            unfocusedBorderColor = VBorder,
                            focusedTextColor = VTextHi,
                            unfocusedTextColor = VTextHi,
                            cursorColor = VAccent,
                            focusedContainerColor = VSurfaceHi,
                            unfocusedContainerColor = VSurfaceHi
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = jumpInput.toIntOrNull()
                    if (target != null && pageCount > 0) {
                        currentPage = (target - 1).coerceIn(0, pageCount - 1)
                    }
                    showJumpDialog = false
                }) { Text("Go", color = VAccent, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel", color = VTextLo)
                }
            }
        )
    }
}

@Composable
private fun RowScope.PdfActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color = VTextMd,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .background(VSurfaceHi, RoundedCornerShape(12.dp))
            .border(1.dp, VBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = VTextHi, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TextViewer(file: File) {
    val text = remember(file) { readTextSafe(file) }
    var searchQuery by remember { mutableStateOf("") }
    var fontSize by remember { mutableStateOf(15f) }
    var wrapLines by remember { mutableStateOf(true) }
    val lineCount = remember(text) { text.count { it == '\n' } + 1 }
    val charCount = remember(text) { text.length }

    Column(modifier = Modifier.fillMaxSize().background(VBg)) {

        // Search + actions card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VSurface)
                .border(1.dp, VBorder)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search…", color = VTextLo, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = VTextMd, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, null, tint = VTextLo, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VAccent,
                        unfocusedBorderColor = VBorder,
                        focusedTextColor = VTextHi,
                        unfocusedTextColor = VTextHi,
                        cursorColor = VAccent,
                        focusedContainerColor = VSurfaceHi,
                        unfocusedContainerColor = VSurfaceHi
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                )
                IconButton(
                    onClick = { wrapLines = !wrapLines },
                    modifier = Modifier.viewerIconButtonStyle(selected = wrapLines)
                ) {
                    Icon(
                        Icons.Rounded.WrapText, null,
                        tint = if (wrapLines) VAccent else VTextMd,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.FormatSize, null, tint = VAccent2, modifier = Modifier.size(16.dp))
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 10f..28f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = VAccent2,
                        activeTrackColor = VAccent2,
                        inactiveTrackColor = VSurfaceGlow
                    )
                )
                Box(
                    modifier = Modifier
                        .background(VAccent2.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .border(1.dp, VAccent2.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("${fontSize.toInt()}px", color = VAccent2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Stats chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipPill("LINES", lineCount.toString(), VAccent)
                ChipPill("CHARS", charCount.toString(), VAccent2)
                ChipPill("SIZE", formatFileSize(file.length()), VAccent3)
            }
        }

        val annotatedText = remember(text, searchQuery) {
            if (searchQuery.isBlank()) {
                androidx.compose.ui.text.AnnotatedString(text)
            } else {
                val query = searchQuery.lowercase(Locale.getDefault())
                val source = text
                val builder = androidx.compose.ui.text.AnnotatedString.Builder()
                var searchStart = 0
                val lowerText = source.lowercase(Locale.getDefault())
                while (searchStart < lowerText.length) {
                    val index = lowerText.indexOf(query, searchStart)
                    if (index < 0) {
                        builder.append(source.substring(searchStart))
                        break
                    }
                    builder.append(source.substring(searchStart, index))
                    builder.pushStyle(
                        androidx.compose.ui.text.SpanStyle(
                            background = VWarn.copy(alpha = 0.45f),
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    builder.append(source.substring(index, index + query.length))
                    builder.pop()
                    searchStart = index + query.length
                }
                builder.toAnnotatedString()
            }
        }

        SelectionContainer(modifier = Modifier.fillMaxSize()) {
            val baseModifier = Modifier
                .fillMaxSize()
                .background(VBg)
                .verticalScroll(rememberScrollState())
            val finalModifier =
                if (wrapLines) baseModifier.padding(14.dp)
                else baseModifier.horizontalScroll(rememberScrollState()).padding(14.dp)

            Text(
                text = annotatedText,
                color = VTextHi,
                fontSize = fontSize.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                softWrap = wrapLines,
                modifier = finalModifier
            )
        }
    }
}

private fun readTextSafe(file: File): String {
    return try {
        file.readText()
    } catch (e: Exception) {
        "Failed to read file: ${e.message}"
    }
}

@Composable
fun HtmlViewer(file: File) {
    val htmlContent = remember(file) { readTextSafe(file) }
    Column(modifier = Modifier.fillMaxSize().background(VBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VSurface)
                .border(1.dp, VBorder)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Lock, null, tint = VWarn, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Web Viewer • JavaScript disabled for security",
                color = VTextMd,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    settings.javaScriptEnabled = false
                    settings.domStorageEnabled = false
                    webViewClient = android.webkit.WebViewClient()
                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ArchiveViewer(file: File) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val archiveContents = remember(file) {
        try { getArchiveContents(file) } catch (e: Exception) { emptyList() }
    }
    val filtered = remember(archiveContents, searchQuery) {
        archiveContents.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ViewerBgBrush)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(VAccent2.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                    .border(1.dp, VAccent2.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.FolderZip, null, tint = VAccent2, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Archive Contents", color = VTextHi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${archiveContents.size} entries • ${formatFileSize(file.length())}",
                    color = VTextLo, fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search entries…", color = VTextLo, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = VTextMd, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VAccent,
                unfocusedBorderColor = VBorder,
                focusedTextColor = VTextHi,
                unfocusedTextColor = VTextHi,
                cursorColor = VAccent,
                focusedContainerColor = VSurfaceHi,
                unfocusedContainerColor = VSurfaceHi
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching entries", color = VTextLo, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(VSurface, RoundedCornerShape(10.dp))
                            .border(1.dp, VBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.InsertDriveFile, null,
                            tint = VAccent, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            item, color = VTextHi, fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { extractArchive(file, context) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Unarchive, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Extract Archive", fontWeight = FontWeight.SemiBold)
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
            ApkInfo("Failed to read APK", "", 0L, file.length(), emptyList())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ViewerBgBrush)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(VOk.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                    .border(1.dp, VOk.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Android, null, tint = VOk, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("APK Package", color = VTextHi, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(apkInfo.packageName, color = VTextLo, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChipPill("VERSION", apkInfo.versionName.ifBlank { "—" }, VAccent)
            ChipPill("CODE", apkInfo.versionCode.toString(), VAccent2)
            ChipPill("SIZE", formatFileSize(apkInfo.size), VAccent3)
            ChipPill("PERMS", apkInfo.permissions.size.toString(), VWarn)
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VSurface, RoundedCornerShape(16.dp))
                .border(1.dp, VBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, null, tint = VAccent2, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text("Permissions", color = VTextHi, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text("(${apkInfo.permissions.size})", color = VTextLo, fontSize = 11.sp)
            }
            Spacer(Modifier.height(4.dp))
            if (apkInfo.permissions.isEmpty()) {
                Text("No declared permissions", color = VTextLo, fontSize = 12.sp)
            } else {
                apkInfo.permissions.forEach { permission ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            Modifier
                                .padding(top = 6.dp)
                                .size(4.dp)
                                .background(VAccent2, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            permission.removePrefix("android.permission."),
                            color = VTextMd, fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { installApk(context, file) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = VOk, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Install", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = { shareApk(context, file) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, VAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun getMimeType(extension: String): String? {
    return when (extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        "bmp" -> "image/bmp"
        "tiff", "tif" -> "image/tiff"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "avi" -> "video/avi"
        "mov" -> "video/quicktime"
        "flv" -> "video/x-flv"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "ogg", "oga" -> "audio/ogg"
        "aac" -> "audio/aac"
        "flac" -> "audio/flac"
        "m4a" -> "audio/mp4"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "html", "htm" -> "text/html"
        "mhtml" -> "multipart/related"
        "json" -> "application/json"
        "xml" -> "application/xml"
        "csv" -> "text/csv"
        "md" -> "text/markdown"
        "zip" -> "application/zip"
        "rar" -> "application/x-rar-compressed"
        "7z" -> "application/x-7z-compressed"
        "tar" -> "application/x-tar"
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

private fun formatDuration(millis: Int): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
    } else {
        String.format("%d:%02d", minutes, seconds % 60)
    }
}

private fun formatModifiedDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
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
    // Extract into the app-private WAOS_Extracted folder so the contents
    // stay invisible to Gallery / device file manager.
    val baseDir = context.getExternalFilesDir(null)
    val extractDir = File(baseDir, "WAOS_Extracted/${file.nameWithoutExtension}")

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
        ApkInfo("Failed to read APK info", "", 0L, file.length(), emptyList())
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

    private val savedPositions = mutableMapOf<String, Int>()

    fun loadFile(file: File) {
        currentFile = file
        // Additional loading logic can be added here
    }

    fun savePosition(file: File, position: Int) {
        savedPositions[file.absolutePath] = position
    }

    fun getSavedPosition(file: File): Int {
        return savedPositions[file.absolutePath] ?: 0
    }

    fun duplicateFile(file: File): File? {
        return try {
            val copyName = file.nameWithoutExtension + " (copy)" + if (file.extension.isNotBlank()) ".${file.extension}" else ""
            val duplicateFile = File(file.parentFile, copyName)
            if (!duplicateFile.exists()) {
                file.copyTo(duplicateFile)
            }
            duplicateFile
        } catch (e: Exception) {
            null
        }
    }

    fun deleteFile(file: File): Boolean {
        return try {
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        } catch (e: Exception) {
            false
        }
    }
}