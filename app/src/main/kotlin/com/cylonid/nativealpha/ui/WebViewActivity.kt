package com.cylonid.nativealpha.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.cylonid.nativealpha.webview.SessionManager

@AndroidEntryPoint
class WebViewActivity : ComponentActivity() {

    private lateinit var viewModel: WebViewViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val webAppId = intent.getLongExtra("webAppId", 0L)

        setContent {
            WebViewScreen(
                webAppId = webAppId,
                onBackPressed = { finish() }
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 4231 && resultCode == RESULT_OK && data != null) {
            val username = data.getStringExtra("CREDENTIAL_USERNAME")
            val password = data.getStringExtra("CREDENTIAL_PASSWORD")
            if (username != null && password != null) {
                // Store for the Composable to handle
                pendingAutoFill = Pair(username, password)
            }
        }
    }

    companion object {
        var pendingAutoFill: Pair<String, String>? = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 4231 && resultCode == RESULT_OK && data != null) {
            val username = data.getStringExtra("CREDENTIAL_USERNAME")
            val password = data.getStringExtra("CREDENTIAL_PASSWORD")
            if (username != null && password != null) {
                // Get the ViewModel and call autoFill
                // Since ViewModel is created in Composable, we need another way
                // For now, we'll handle it in the Composable
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    webAppId: Long,
    onBackPressed: () -> Unit,
    viewModel: WebViewViewModel = viewModel()
) {
    val context = LocalContext.current
    val webViewState by viewModel.webViewState.collectAsState()
    val consoleMessages by viewModel.consoleMessages.collectAsState()
    val showConsole by viewModel.showConsole.collectAsState()
    val isDesktopMode by viewModel.isDesktopMode.collectAsState()
    val isAdblockEnabled by viewModel.isAdblockEnabled.collectAsState()
    val isAutoScrollEnabled by viewModel.isAutoScrollEnabled.collectAsState()
    val isAutoClickEnabled by viewModel.isAutoClickEnabled.collectAsState()
    val webApp by viewModel.webApp.collectAsState()

    LaunchedEffect(webAppId) {
        viewModel.loadWebApp(webAppId)
    }

    // Handle pending auto-fill
    LaunchedEffect(Unit) {
        val autoFill = pendingAutoFill
        if (autoFill != null) {
            viewModel.autoFillCredentials(autoFill.first, autoFill.second)
            pendingAutoFill = null
        }
    }

    // Handle opening activities
    LaunchedEffect(webViewState.shouldOpenCredentialKeeper) {
        if (webViewState.shouldOpenCredentialKeeper) {
            val intent = Intent(context, CredentialVaultActivity::class.java).apply {
                putExtra("WEB_APP_ID", webAppId)
            }
            (context as? ComponentActivity)?.startActivityForResult(intent, 4231)
            viewModel.clearActionFlags()
        }
    }

    LaunchedEffect(webViewState.shouldOpenClipboardManager) {
        if (webViewState.shouldOpenClipboardManager) {
            val intent = Intent(context, ClipboardManagerActivity::class.java).apply {
                putExtra("WEB_APP_ID", webAppId)
            }
            context.startActivity(intent)
            viewModel.clearActionFlags()
        }
    }

    LaunchedEffect(webViewState.shouldOpenDownloadHistory) {
        if (webViewState.shouldOpenDownloadHistory) {
            val intent = Intent(context, DownloadHistoryActivity::class.java).apply {
                putExtra("WEB_APP_ID", webAppId)
            }
            context.startActivity(intent)
            viewModel.clearActionFlags()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(webApp?.name ?: "Web App") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Link Management Button
                    IconButton(onClick = {
                        linkExample.showLinkOptions()
                    }) {
                        Icon(Icons.Default.Link, contentDescription = "Link options")
                    }
                    // Back/Forward Navigation
                    IconButton(
                        onClick = { viewModel.goBack() },
                        enabled = webViewState.canGoBack
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    IconButton(
                        onClick = { viewModel.goForward() },
                        enabled = webViewState.canGoForward
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                    }

                    // Refresh
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }

                    // Desktop/Mobile Toggle
                    IconButton(onClick = { viewModel.toggleDesktopMode() }) {
                        Icon(
                            if (isDesktopMode) Icons.Default.Computer else Icons.Default.Smartphone,
                            contentDescription = if (isDesktopMode) "Desktop mode" else "Mobile mode"
                        )
                    }

                    // Home
                    IconButton(onClick = { viewModel.goHome() }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }

                    // Share
                    IconButton(onClick = { viewModel.sharePage() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }

                    // Copy URL
                    IconButton(onClick = { viewModel.copyUrl() }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL")
                    }

                    // Find in Page
                    IconButton(onClick = { showFindDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Find in page")
                    }

                    // Print
                    IconButton(onClick = { viewModel.printPage() }) {
                        Icon(Icons.Default.Print, contentDescription = "Print")
                    }

                    // Zoom Controls
                    IconButton(onClick = { viewModel.zoomIn() }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in")
                    }
                    IconButton(onClick = { viewModel.zoomOut() }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out")
                    }

                    // JavaScript Console
                    IconButton(onClick = { viewModel.toggleConsole() }) {
                        Icon(
                            if (showConsole) Icons.Default.CodeOff else Icons.Default.Code,
                            contentDescription = "Toggle console"
                        )
                    }

                    // Adblock Toggle
                    IconButton(onClick = { viewModel.toggleAdblock() }) {
                        Icon(
                            if (isAdblockEnabled) Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = "Toggle adblock"
                        )
                    }

                    // Auto-scroll Toggle
                    IconButton(onClick = { viewModel.toggleAutoScroll() }) {
                        Icon(
                            if (isAutoScrollEnabled) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Toggle auto-scroll"
                        )
                    }

                    // Auto-click Toggle
                    IconButton(onClick = { viewModel.toggleAutoClick() }) {
                        Icon(
                            if (isAutoClickEnabled) Icons.Default.TouchApp else Icons.Default.DoNotTouch,
                            contentDescription = "Toggle auto-click"
                        )
                    }

                    // Download Handler
                    IconButton(onClick = { /* Download handler */ }) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                    }

                    // Open in External Browser
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                    }

                    // Add to Floating Window
                    IconButton(onClick = {
                        webApp?.let { app ->
                            val intent = Intent(context, com.cylonid.nativealpha.service.FloatingWindowService::class.java).apply {
                                action = com.cylonid.nativealpha.service.FloatingWindowService.ACTION_ADD_WINDOW
                                putExtra("webAppId", app.id)
                                putExtra("webAppUrl", app.url)
                                putExtra("webAppName", app.name)
                            }
                            context.startService(intent)
                        }
                        viewModel.clearActionFlags()
                    }) {
                        Icon(Icons.Default.Launch, contentDescription = "Add to floating window")
                    }

                    // Open Credential Keeper
                    IconButton(onClick = { viewModel.openCredentialKeeper() }) {
                        Icon(Icons.Default.Lock, contentDescription = "Credentials")
                    }

                    // Open Clipboard Manager
                    IconButton(onClick = { viewModel.openClipboardManager() }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Clipboard")
                    }

                    // Open Download History
                    IconButton(onClick = { viewModel.openDownloadHistory() }) {
                        Icon(Icons.Default.History, contentDescription = "Downloads")
                    }

                    // Screenshot
                    IconButton(onClick = { viewModel.takeScreenshot() }) {
                        Icon(Icons.Default.Camera, contentDescription = "Screenshot")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Loading Progress Bar
            if (webViewState.isLoading) {
                LinearProgressIndicator(
                    progress = webViewState.progress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // WebView
            AndroidView(
                factory = { context ->
                    sessionManager?.createIsolatedWebView(context, url) ?: WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = webApp?.isJavaScriptEnabled ?: true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = webApp?.userAgent ?: userAgentString
                            builtInZoomControls = webApp?.isEnableZooming ?: true
                            displayZoomControls = false
                            setSupportZoom(true)
                        }

                        webViewClient = WebViewClientWithDownload(
                            context = context,
                            onPageStarted = { url -> viewModel.onPageStarted(url) },
                            onPageFinished = { url -> 
                                viewModel.onPageFinished(url)
                                // Inject JavaScript automation
                                injectAutomationScripts(webView)
                                // Setup link management
                                linkExample.setupWebViewLinkHandling(webView)
                                // Setup notification monitoring
                                notificationSystem?.setupDOMMonitoring(webView)
                                // Inject dark mode if enabled
                                webApp?.let { app ->
                                    if (app.isDarkModeEnabled) {
                                        injectDarkMode(webView)
                                    }
                                }
                            },
                            onDownloadStart = { filename, url ->
                                // Handle download through our DownloadManager
                                viewModel.handleDownload(url, webApp)
                            }
                        )

                        // Add download listener
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            viewModel.handleDownload(url, webApp)
                        }

                        // Add JavaScript interface for notifications
                        notificationSystem?.let { system ->
                            addJavascriptInterface(object {
                                @android.webkit.JavascriptInterface
                                fun reportContentChange(trigger: String) {
                                    system.reportContentChange(trigger)
                                }
                            }, "WebApp")
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                consoleMessage?.let {
                                    viewModel.addConsoleMessage(
                                        ConsoleMessageData(
                                            message = it.message(),
                                            level = it.messageLevel().name,
                                            source = it.sourceId() ?: "",
                                            line = it.lineNumber()
                                        )
                                    )
                                }
                                return super.onConsoleMessage(consoleMessage)
                            }

                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                viewModel.onProgressChanged(newProgress)
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                // Handle file chooser
                                return true
                            }
                        }

                        // Load the initial URL
                        loadUrl(webApp?.url ?: "")
                    }
                },
                update = { webView ->
                    // Handle action flags from ViewModel
                    if (webViewState.shouldGoBack) {
                        webView.goBack()
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldGoForward) {
                        webView.goForward()
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldLoadUrl != null) {
                        webView.loadUrl(webViewState.shouldLoadUrl!!)
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldReload) {
                        webView.reload()
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldRefresh) {
                        webView.reload()
                        viewModel.refresh()
                    }
                    if (webViewState.javaScriptToExecute != null) {
                        webView.evaluateJavascript(webViewState.javaScriptToExecute!!, null)
                        viewModel.clearJavaScriptCommand()
                    }

                    // Update navigation state
                    viewModel.updateNavigationState(webView.canGoBack(), webView.canGoForward())

                    // Handle other actions that need context
                    webViewState.shouldShareUrl?.let { urlToShare ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, urlToShare)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share URL"))
                        viewModel.clearActionFlags()
                    }

                    webViewState.shouldCopyUrl?.let { urlToCopy ->
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        val clip = android.content.ClipData.newPlainText("URL", urlToCopy)
                        clipboard.setPrimaryClip(clip)
                        // TODO: Show toast
                        viewModel.clearActionFlags()
                    }

                    if (webViewState.shouldPrint) {
                        // Print functionality
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                            val printAdapter = webView.createPrintDocumentAdapter("WAOS Print")
                            val printJob = printManager.print("WAOS Document", printAdapter, null)
                        }
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldZoomIn) {
                        webView.zoomIn()
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldTakeScreenshot) {
                        // Screenshot functionality
                        webView.evaluateJavascript("""
                            (function() {
                                const canvas = document.createElement('canvas');
                                const ctx = canvas.getContext('2d');
                                canvas.width = window.innerWidth;
                                canvas.height = window.innerHeight;
                                
                                // Draw the current page
                                const data = '<svg xmlns="http://www.w3.org/2000/svg" width="' + canvas.width + '" height="' + canvas.height + '">' +
                                    '<foreignObject width="100%" height="100%">' +
                                    '<div xmlns="http://www.w3.org/1999/xhtml">' +
                                    document.documentElement.innerHTML +
                                    '</div></foreignObject></svg>';
                                
                                // This is a simplified version - real screenshot would use WebView.capturePicture()
                                // or PixelCopy for API 24+
                                return 'screenshot_taken';
                            })();
                        """.trimIndent()) { result ->
                            // Handle screenshot result
                        }
                        viewModel.clearActionFlags()
                    }
                },
                modifier = Modifier.weight(1f)
            )

            // Console Panel (if enabled)
            if (showConsole) {
                ConsolePanel(
                    messages = consoleMessages,
                    onExecuteCommand = { command ->
                        // Execute JavaScript command
                        // This would be implemented to run JS in the WebView
                        viewModel.executeJavaScript(command)
                    },
                    modifier = Modifier.height(200.dp)
                )
            }

            // Find in Page Dialog
            if (showFindDialog) {
                FindInPageDialog(
                    query = findQuery,
                    onQueryChange = { findQuery = it },
                    onFindNext = {
                        viewModel.executeJavaScript("window.waosFind?.findNext('$findQuery');")
                    },
                    onFindPrevious = {
                        viewModel.executeJavaScript("window.waosFind?.findPrevious('$findQuery');")
                    },
                    onDismiss = { showFindDialog = false }
                )
            }

            // Error State
            if (webViewState.error != null) {
                ErrorPanel(
                    error = webViewState.error,
                    onRetry = { viewModel.refresh() }
                )
            }
        }
    }
}

private fun injectDarkMode(webView: WebView?) {
    webView?.evaluateJavascript("""
        (function() {
            const style = document.createElement('style');
            style.textContent = `
                html {
                    filter: invert(1) hue-rotate(180deg);
                }
                img, video, canvas, svg {
                    filter: invert(1) hue-rotate(180deg);
                }
                * {
                    background-color: inherit;
                }
            `;
            document.head.appendChild(style);
        })();
    """.trimIndent(), null)
}

// TODO: WAOS automation scripts need to be implemented properly in a separate function

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsolePanel(
    messages: List<ConsoleMessageData>,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var command by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        // Console messages
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(messages) { message ->
                ConsoleMessageItem(message)
            }
        }

        // Command input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                placeholder = { Text("Enter JavaScript command...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (command.isNotBlank()) {
                    onExecuteCommand(command)
                    command = ""
                }
            }) {
                Text("Execute")
            }
        }
    }
}

@Composable
fun ConsoleMessageItem(message: ConsoleMessageData) {
    val color = when (message.level) {
        "ERROR" -> MaterialTheme.colorScheme.error
        "WARNING" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Text(
        text = "[${message.level}] ${message.source}:${message.line} ${message.message}",
        color = color,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindInPageDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Find in Page") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search text...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onFindPrevious,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Previous")
                    }
                    OutlinedButton(
                        onClick = onFindNext,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ErrorPanel(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Failed to load page",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}