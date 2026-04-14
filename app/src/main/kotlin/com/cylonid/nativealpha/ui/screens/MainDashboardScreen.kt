package com.cylonid.nativealpha.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.hilt.navigation.compose.hiltViewModel
import com.cylonid.nativealpha.ui.WebViewActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val webApps by viewModel.filteredWebApps.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val groupBy by viewModel.groupBy.collectAsState()
    val groupedWebApps by viewModel.groupedWebApps.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WAOS Dashboard") },
                actions = {
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                            contentDescription = "Toggle view"
                        )
                    }
                    IconButton(onClick = { /* TODO: Open settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Add new webapp */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add webapp")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search web apps...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // Sort and Group Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var sortExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = sortExpanded,
                    onExpandedChange = { sortExpanded = it }
                ) {
                    OutlinedTextField(
                        value = sortBy.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sort by") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sortExpanded) },
                        modifier = Modifier.menuAnchor().weight(1f)
                    )
                    ExposedDropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    viewModel.updateSortBy(option)
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }

                var groupExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = groupExpanded,
                    onExpandedChange = { groupExpanded = it }
                ) {
                    OutlinedTextField(
                        value = groupBy.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Group by") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupExpanded) },
                        modifier = Modifier.menuAnchor().weight(1f)
                    )
                    ExposedDropdownMenu(
                        expanded = groupExpanded,
                        onDismissRequest = { groupExpanded = false }
                    ) {
                        GroupOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    viewModel.updateGroupBy(option)
                                    groupExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Web Apps List/Grid
            if (groupBy == GroupOption.NONE) {
                // Ungrouped view
                AnimatedContent(
                    targetState = isGridView,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) + scaleIn() togetherWith
                        fadeOut(animationSpec = tween(300)) + scaleOut()
                    }
                ) { gridView ->
                    if (gridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(webApps, key = { it.id }) { webApp ->
                                AnimatedWebAppCard(
                                    webApp = webApp,
                                    onClick = {
                                        val intent = Intent(context, WebViewActivity::class.java).apply {
                                            putExtra("webAppId", webApp.id)
                                        }
                                        context.startActivity(intent)
                                    },
                                    onLongClick = { /* TODO: Show context menu */ }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(webApps, key = { it.id }) { webApp ->
                                AnimatedWebAppListItem(
                                    webApp = webApp,
                                    onClick = {
                                        val intent = Intent(context, WebViewActivity::class.java).apply {
                                            putExtra("webAppId", webApp.id)
                                        }
                                        context.startActivity(intent)
                                    },
                                    onLongClick = { /* TODO: Show context menu */ }
                                )
                            }
                        }
                    }
                }
            } else {
                // Grouped view
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedWebApps.forEach { (groupName, apps) ->
                        item {
                            Text(
                                text = groupName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        if (isGridView) {
                            item {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 120.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    userScrollEnabled = false
                                ) {
                                    items(apps, key = { it.id }) { webApp ->
                                        AnimatedWebAppCard(
                                            webApp = webApp,
                                            onClick = { /* TODO: Open webapp */ },
                                            onLongClick = { /* TODO: Show context menu */ },
                                            modifier = Modifier.aspectRatio(1f)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(apps, key = { it.id }) { webApp ->
                                AnimatedWebAppListItem(
                                    webApp = webApp,
                                    onClick = { /* TODO: Open webapp */ },
                                    onLongClick = { /* TODO: Show context menu */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedWebAppCard(
    webApp: WebApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // We'll handle the animation ourselves
                onClick = onClick
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = if (isPressed) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon (placeholder)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = webApp.name.first().toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = webApp.name,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            // Status indicator
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Green) // TODO: Dynamic status
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Active", // TODO: Dynamic status
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Green
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedWebAppListItem(
    webApp: WebApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = webApp.name.first().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = webApp.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = webApp.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Green) // TODO: Dynamic status
            )
        }
    }
}

enum class SortOption(val displayName: String) {
    NAME("Name"),
    LAST_USED("Last Used"),
    MOST_ACTIVE("Most Active"),
    CUSTOM("Custom Order")
}

enum class GroupOption(val displayName: String) {
    NONE("No Grouping"),
    CATEGORY("Category"),
    STATUS("Status"),
    CUSTOM("Custom Groups")
}

@Composable
fun WebAppListItem(
    webApp: WebApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.Web,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = webApp.name, style = MaterialTheme.typography.titleMedium)
                Text(text = webApp.url, style = MaterialTheme.typography.bodySmall)
            }
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = when (webApp.status) {
                            WebApp.Status.ACTIVE -> Color.Green
                            WebApp.Status.BACKGROUND -> Color.Yellow
                            WebApp.Status.ERROR -> Color.Red
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}