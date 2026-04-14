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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cylonid.nativealpha.viewmodel.WebViewViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WebViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val webAppUrl = intent.getStringExtra("WEB_APP_URL") ?: ""
        val webAppName = intent.getStringExtra("WEB_APP_NAME") ?: ""

        setContent {
            WebViewScreen(
                url = webAppUrl,
                appName = webAppName,
                onBackPressed = { finish() }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    appName: String,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appName) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
                    IconButton(onClick = { viewModel.showFindInPage() }) {
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
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = if (isDesktopMode) {
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                            } else {
                                userAgentString
                            }
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportZoom(true)
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                viewModel.onPageStarted(url ?: "")
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url, url?.let { view?.getFavicon() })
                                viewModel.onPageFinished(url ?: "")
                                // Inject JavaScript automation
                                injectAutomationScripts(view)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                viewModel.onError(error?.description?.toString() ?: "Unknown error")
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString()
                                if (url != null && viewModel.shouldHandleDownload(url)) {
                                    viewModel.handleDownload(url, webApp)
                                    return true
                                }
                                return super.shouldOverrideUrlLoading(view, request)
                            }
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
                        loadUrl(url)
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

                    // TODO: Implement other actions (find in page, print, zoom, etc.)
                    if (webViewState.shouldShowFindInPage) {
                        // Show find in page dialog
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldPrint) {
                        // Print functionality
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldZoomIn) {
                        webView.zoomIn()
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldZoomOut) {
                        webView.zoomOut()
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldTakeScreenshot) {
                        // Screenshot functionality
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

private fun injectAutomationScripts(webView: WebView?) {
    webView?.evaluateJavascript("""
        // WAOS Automation Scripts
        (function() {
            // Auto-scroll functionality
            window.waosAutoScroll = {
                interval: null,
                start: function(speed = 1) {
                    this.stop();
                    this.interval = setInterval(function() {
                        window.scrollBy(0, speed);
                    }, 50);
                },
                stop: function() {
                    if (this.interval) {
                        clearInterval(this.interval);
                        this.interval = null;
                    }
                }
            };

            // Auto-click functionality
            window.waosAutoClick = {
                interval: null,
                start: function(selector, interval = 1000) {
                    this.stop();
                    this.interval = setInterval(function() {
                        const element = document.querySelector(selector);
                        if (element) {
                            element.click();
                        }
                    }, interval);
                },
                stop: function() {
                    if (this.interval) {
                        clearInterval(this.interval);
                        this.interval = null;
                    }
                }
            };

            // Form auto-fill
            window.waosAutoFill = function(data) {
                Object.keys(data).forEach(function(selector) {
                    const element = document.querySelector(selector);
                    if (element) {
                        element.value = data[selector];
                        element.dispatchEvent(new Event('input', { bubbles: true }));
                        element.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                });
            };

            // DOM monitoring
            window.waosMonitor = {
                observers: [],
                observe: function(selector, callback) {
                    const observer = new MutationObserver(function(mutations) {
                        mutations.forEach(function(mutation) {
                            if (mutation.type === 'childList') {
                                const elements = document.querySelectorAll(selector);
                                elements.forEach(function(element) {
                                    callback(element);
                                });
                            }
                        });
                    });
                    observer.observe(document.body, {
                        childList: true,
                        subtree: true
                    });
                    this.observers.push(observer);
                },
                disconnect: function() {
                    this.observers.forEach(function(observer) {
                        observer.disconnect();
                    });
                    this.observers = [];
                }
            };

            console.log('WAOS automation scripts loaded');
        })();
    """.trimIndent(), null)
}

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