package com.cylonid.nativealpha.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cylonid.nativealpha.ui.theme.*

/**
 * Quick actions bar for file operations
 */
@Composable
fun QuickActionsBar(
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = CardSurface,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickActionButton(
                icon = Icons.Default.Share,
                label = "Share",
                color = CyanPrimary,
                onClick = onShare,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.Edit,
                label = "Rename",
                color = Color(0xFFFFB800),
                onClick = onRename,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.DriveFileMove,
                label = "Move",
                color = Color(0xFF9C27B0),
                onClick = onMove,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.Info,
                label = "Info",
                color = Color(0xFF2196F3),
                onClick = onInfo,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.Delete,
                label = "Delete",
                color = ErrorRed,
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(0.15f),
            contentColor = color
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
            Text(label, fontSize = androidx.compose.ui.unit.sp(10f))
        }
    }
}
