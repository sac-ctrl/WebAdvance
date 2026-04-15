package com.cylonid.nativealpha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cylonid.nativealpha.ui.theme.*
import com.cylonid.nativealpha.util.StorageUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * File information panel showing metadata and details
 */
@Composable
fun FileInfoPanel(
    file: File,
    fileType: String,
    icon: String,
    onExpanded: (Boolean) -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header - Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(icon, fontSize = 28.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            file.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            "${StorageUtil.formatFileSize(file.length())} • $fileType",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(
                    onClick = { 
                        isExpanded = !isExpanded
                        onExpanded(isExpanded)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded Details
            if (isExpanded) {
                Divider(color = CardBorder, modifier = Modifier.fillMaxWidth())
                
                Column(modifier = Modifier.padding(16.dp)) {
                    FileInfoRow("Name", file.name)
                    FileInfoRow("Size", StorageUtil.formatFileSize(file.length()))
                    FileInfoRow("Type", fileType)
                    FileInfoRow("Modified", formatDate(file.lastModified()))
                    FileInfoRow("Path", file.absolutePath)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Additional stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatItem("Created", formatDate(file.lastModified()))
                        StatItem("Readable", if (file.canRead()) "Yes" else "No")
                        StatItem("Writable", if (file.canWrite()) "Yes" else "No")
                    }
                }
            }
        }
    }
}

@Composable
private fun FileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMuted, fontSize = 12.sp)
        Text(
            value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .weight(1f, false)
                .padding(start = 8.dp)
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Box(
        modifier = Modifier
            .weight(1f)
            .background(BgDark, RoundedCornerShape(8.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = TextMuted, fontSize = 10.sp)
            Text(value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
