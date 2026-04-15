package com.cylonid.nativealpha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cylonid.nativealpha.ui.theme.*
import java.io.File

/**
 * Text/Document viewer with search, font size adjustment, and night mode
 */
@Composable
fun TextViewer(
    file: File,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fontSize by remember { mutableStateOf(14f) }
    var isDarkMode by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showFindBar by remember { mutableStateOf(false) }
    
    val content = remember {
        try {
            file.readText()
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkMode) BgDeep else androidx.compose.ui.graphics.Color.White)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(CardSurface, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
            }
            
            Column {
                Text(
                    file.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${content.lines().size} lines • ${StorageUtil.formatFileSize(file.length())}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showFindBar = !showFindBar },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Search, "Find", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                
                IconButton(
                    onClick = { isDarkMode = !isDarkMode },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        "Theme",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Find bar
        if (showFindBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search...", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        cursorColor = CyanPrimary
                    )
                )
                IconButton(onClick = { showFindBar = false }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = TextSecondary)
                }
            }
        }

        // Font controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = { if (fontSize > 10f) fontSize -= 2f },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.TextDecrease, "Smaller", tint = TextSecondary)
            }
            
            Text(
                "${fontSize.toInt()}sp",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp)
            )
            
            IconButton(
                onClick = { if (fontSize < 28f) fontSize += 2f },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.TextIncrease, "Larger", tint = TextSecondary)
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                "${content.lines().size} lines",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Divider(color = CardBorder, modifier = Modifier.fillMaxWidth())

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkMode) BgDeep else androidx.compose.ui.graphics.Color.White)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                content,
                color = if (isDarkMode) TextPrimary else BgDeep,
                fontSize = fontSize.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = (fontSize + 4).sp
            )
        }
    }
}
