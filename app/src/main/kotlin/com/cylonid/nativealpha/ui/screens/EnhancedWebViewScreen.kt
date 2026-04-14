package com.cylonid.nativealpha.ui.screens

import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.viewmodel.EnhancedWebViewViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedWebViewScreen(
    webAppId: Long,
    onBackPressed: () -> Unit,
    viewModel: EnhancedWebViewViewModel = hiltViewModel()
) {
    val webApp by viewModel.webApp.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingProgress by viewModel.loadingProgress.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val canGoBack by viewModel.canGoBack.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()
    val pageTitle by viewModel.pageTitle.collectAsState()
    val downloads by viewModel.downloadedFiles.collectAsState()
    
    var showToolbar by remember { mutableStateOf(true) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSettingsPanel by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    
    // WebView reference
    var webViewRef: WebView? by remember { mutableStateOf(null) }

    LaunchedEffect(webAppId) {
        viewModel.loadWebApp(webAppId)
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(showToolbar) {
                TopAppBar(
                    title = { Text(pageTitle.ifEmpty { webApp?.name ?: "Web App" }) },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to apps")
                        }
                    },
                    actions = {
                        // Back button
                        IconButton(
                            onClick = { viewModel.goBack() },
                            enabled = canGoBack
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                        }
                        
                        // Forward button
                        IconButton(
                            onClick = { viewModel.goForward() },
                            enabled = canGoForward
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Go forward")
                        }
                        
                        // Home button
                        IconButton(onClick = { viewModel.goHome() }) {
                            Icon(Icons.Default.Home, contentDescription = "Home")
                        }
                        
                        // Refresh button
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }

                        // Find button
                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                            Icon(Icons.Default.Search, contentDescription = "Find in page")
                        }

                        // More options
                        Box {
                            IconButton(onClick = { showMoreMenu = !showMoreMenu }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Copy URL") },
                                    onClick = {
                                        val clipboard = androidx.compose.ui.platform.LocalContext.current
                                            .getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clipboard.setPrimaryClip(
                                            android.content.ClipData.newPlainText("URL", currentUrl)
                                        )
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share Page") },
                                    onClick = {
                                        // Share intent
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Find in Page") },
                                    onClick = {
                                        showSearchBar = true
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Zoom In") },
                                    onClick = {
                                        viewModel.zoomIn()
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Zoom Out") },
                                    onClick = {
                                        viewModel.zoomOut()
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Cache") },
                                    onClick = {
                                        viewModel.clearCache()
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Cookies") },
                                    onClick = {
                                        viewModel.clearCookies()
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Take Screenshot") },
                                    onClick = {
                                        viewModel.takeScreenshot()
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Screenshot") },
                                    onClick = {
                                        // Handle screenshot
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        showSettingsPanel = !showSettingsPanel
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clipboard Manager") },
                                    onClick = {
                                        // Open clipboard manager
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Download History") },
                                    onClick = {
                                        // Open download history
                                        showMoreMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Credentials") },
                                    onClick = {
                                        // Open credential keeper
                                        showMoreMenu = false
                                    }
                                )
                            }
                        }
                    },
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            AnimatedVisibility(showSearchBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { newText ->
                            searchText = newText
                            viewModel.findInPage(newText)
                        },
                        placeholder = { Text("Find in page...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    IconButton(onClick = { showSearchBar = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close search")
                    }
                }
            }

            // URL bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = currentUrl,
                    onValueChange = { urlInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    singleLine = true,
                    placeholder = { Text("URL...") },
                    textStyle = MaterialTheme.typography.bodySmall,
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSend = { viewModel.loadUrl(urlInput) }
                    )
                )
                IconButton(onClick = { viewModel.loadUrl(urlInput) }) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Load URL")
                }
            }

            // Loading progress bar
            if (isLoading) {
                LinearProgressIndicator(
                    progress = loadingProgress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Main WebView container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                webApp?.let { app ->
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewRef = this
                                viewModel.setupWebView(this, app)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Loading indicator overlay
                if (isLoading && loadingProgress < 100) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Bottom toolbar
            NavigationBar(
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.goBack() },
                    icon = { Icon(Icons.Default.ArrowBack, contentDescription = null) },
                    label = { Text("Back") },
                    enabled = canGoBack
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showSearchBar = !showSearchBar },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Find") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.refresh() },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    label = { Text("Refresh") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showMoreMenu = !showMoreMenu },
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                    label = { Text("More") }
                )
            }
        }
    }

    // Settings panel drawer
    if (showSettingsPanel) {
        WebViewSettingsPanel(
            app = webApp,
            onDismiss = { showSettingsPanel = false }
        )
    }
}

@Composable
fun WebViewSettingsPanel(
    app: WebApp?,
    onDismiss: () -> Unit
) {
    if (app == null) return
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("WebView Settings", style = MaterialTheme.typography.headlineSmall)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("JavaScript Enabled")
                Checkbox(checked = app.isJavaScriptEnabled, onCheckedChange = {})
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Adblock Enabled")
                Checkbox(checked = app.isAdblockEnabled, onCheckedChange = {})
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark Mode")
                Checkbox(checked = app.isDarkModeEnabled, onCheckedChange = {})
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}
