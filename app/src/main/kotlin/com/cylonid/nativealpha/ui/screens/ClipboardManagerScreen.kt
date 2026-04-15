package com.cylonid.nativealpha.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.cylonid.nativealpha.manager.ClipboardItem
import com.cylonid.nativealpha.ui.theme.*
import com.cylonid.nativealpha.viewmodel.ClipboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardManagerScreen(
    webAppId: Long,
    onBackPressed: () -> Unit,
    viewModel: ClipboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardItems by viewModel.clipboardItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var filterType by remember { mutableStateOf<ClipboardItem.Type?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(setOf<String>()) }
    var editingItem by remember { mutableStateOf<ClipboardItem?>(null) }

    LaunchedEffect(webAppId) { viewModel.loadClipboardItems(webAppId) }

    val filtered = clipboardItems
        .filter { filterType == null || it.type == filterType }
        .filter { searchQuery.isBlank() || it.content.contains(searchQuery, ignoreCase = true) }
    val pinned = filtered.filter { it.isPinned }
    val unpinned = filtered.filter { !it.isPinned }

    if (editingItem != null) {
        ClipboardEditDialog(
            item = editingItem!!,
            onDismiss = { editingItem = null },
            onSave = { newContent ->
                viewModel.updateItemContent(editingItem!!, newContent)
                editingItem = null
            }
        )
    }

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
                                    Brush.radialGradient(listOf(GradVioletStart.copy(0.4f), Color.Transparent)),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, GradVioletEnd.copy(0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ContentPaste, null, tint = GradVioletEnd, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Clipboard", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("${filtered.size} items  ·  ${pinned.size} pinned", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Row {
                        IconButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(CardSurface, RoundedCornerShape(10.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.Share, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.clearAllItems(webAppId) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(ErrorRed.copy(0.1f), RoundedCornerShape(10.dp))
                                .border(1.dp, ErrorRed.copy(0.3f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.DeleteSweep, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search clipboard…", color = TextMuted, fontSize = 13.sp) },
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
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = VioletSecondary,
                        unfocusedBorderColor = CardBorder,
                        cursorColor = VioletSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        containerColor = CardSurface
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp)
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgDark)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val types = listOf(null to "All", ClipboardItem.Type.TEXT to "Text", ClipboardItem.Type.URL to "Links", ClipboardItem.Type.IMAGE to "Images")
            items(types) { (type, label) ->
                val isSelected = filterType == type
                val color = if (isSelected) VioletSecondary else CardSurface
                val borderColor by animateColorAsState(
                    if (isSelected) VioletSecondary else CardBorder, tween(200)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) VioletSecondary.copy(0.2f) else CardSurface)
                        .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        label,
                        color = if (isSelected) VioletSecondary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Surface(modifier = Modifier.matchParentSize(), color = Color.Transparent, onClick = { filterType = type }) {}
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(CardSurface, RoundedCornerShape(20.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ContentPaste, null, tint = TextMuted, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Nothing copied yet", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("Items you copy in the WebView appear here", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (pinned.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PushPin, null, tint = VioletSecondary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pinned", color = VioletSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    items(pinned, key = { it.id }) { item ->
                        ClipboardItemCard(
                            item = item,
                            onCopy = { viewModel.copyToSystemClipboard(item, context) },
                            onPin = { viewModel.togglePin(item) },
                            onDelete = { viewModel.deleteItem(item) },
                            onEdit = { editingItem = item }
                        )
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
                if (unpinned.isNotEmpty()) {
                    if (pinned.isNotEmpty()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Recent", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    items(unpinned, key = { it.id }) { item ->
                        ClipboardItemCard(
                            item = item,
                            onCopy = { viewModel.copyToSystemClipboard(item, context) },
                            onPin = { viewModel.togglePin(item) },
                            onDelete = { viewModel.deleteItem(item) },
                            onEdit = { editingItem = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClipboardItemCard(
    item: ClipboardItem,
    onCopy: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val typeColor = when (item.type) {
        ClipboardItem.Type.URL -> GradCyanEnd
        ClipboardItem.Type.IMAGE -> GradPinkEnd
        else -> GradVioletEnd
    }
    val typeIcon = when (item.type) {
        ClipboardItem.Type.URL -> Icons.Default.Link
        ClipboardItem.Type.IMAGE -> Icons.Default.Image
        else -> Icons.Default.TextFields
    }
    val typeLabel = when (item.type) {
        ClipboardItem.Type.URL -> "Link"
        ClipboardItem.Type.IMAGE -> "Image"
        else -> "Text"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(
                1.dp,
                if (item.isPinned) VioletSecondary.copy(0.4f) else CardBorder,
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(typeColor.copy(0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(typeIcon, null, tint = typeColor, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(typeLabel, color = typeColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        formatClipTimestamp(item.timestamp),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                if (item.isPinned) {
                    Icon(Icons.Default.PushPin, null, tint = VioletSecondary, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            val preview = when (item.type) {
                ClipboardItem.Type.IMAGE -> "📷 Image"
                else -> item.content.take(240).let { if (item.content.length > 240) "$it…" else it }
            }
            Text(
                preview,
                color = TextPrimary,
                fontSize = 13.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(10.dp))
            Divider(color = CardBorder, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClipSmallBtn("Copy", Icons.Default.ContentCopy, CyanPrimary, onCopy)
                Spacer(Modifier.width(6.dp))
                ClipSmallBtn("Edit", Icons.Default.Edit, TextSecondary, onEdit)
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = onPin, modifier = Modifier.size(30.dp)) {
                    Icon(
                        if (item.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                        null,
                        tint = if (item.isPinned) VioletSecondary else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, null, tint = ErrorRed.copy(0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ClipSmallBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
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
private fun ClipboardEditDialog(
    item: ClipboardItem,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(item.content) }
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
                Icon(Icons.Default.Edit, null, tint = VioletSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit Clip", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = VioletSecondary,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = VioletSecondary,
                    containerColor = BgDark
                ),
                textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 13.sp)
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(text) },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletSecondary)
                ) { Text("Save", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private fun formatClipTimestamp(date: Date): String {
    val now = System.currentTimeMillis()
    val diff = now - date.time
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
}
