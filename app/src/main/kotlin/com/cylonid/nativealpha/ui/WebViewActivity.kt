package com.cylonid.nativealpha.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CodeOff
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DoNotTouch
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cylonid.nativealpha.viewmodel.ConsoleMessageData
import com.cylonid.nativealpha.viewmodel.WebViewViewModel
import com.cylonid.nativealpha.waos.ui.DownloadHistoryActivity
import com.cylonid.nativealpha.webview.WebViewClientWithDownload
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WebViewActivity : ComponentActivity() {

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
        if (requestCode == REQUEST_CODE_CREDENTIALS && resultCode == RESULT_OK && data != null) {
            val username = data.getStringExtra("CREDENTIAL_USERNAME")
            val password = data.getStringExtra("CREDENTIAL_PASSWORD")
            if (username != null && password != null) {
                pendingAutoFill = Pair(username, password)
            }
        }
    }

    companion object {
        const val REQUEST_CODE_CREDENTIALS = 4231
        var pendingAutoFill: Pair<String, String>? = null
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

    var showFindDialog by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }

    LaunchedEffect(webAppId) {
        viewModel.loadWebApp(webAppId)
    }

    LaunchedEffect(Unit) {
        val autoFill = WebViewActivity.pendingAutoFill
        if (autoFill != null) {
            viewModel.autoFillCredentials(autoFill.first, autoFill.second)
            WebViewActivity.pendingAutoFill = null
        }
    }

    LaunchedEffect(webViewState.shouldOpenCredentialKeeper) {
        if (webViewState.shouldOpenCredentialKeeper) {
            val intent = Intent(context, CredentialVaultActivity::class.java).apply {
                putExtra("WEB_APP_ID", webAppId)
            }
            (context as? ComponentActivity)?.startActivityForResult(
                intent,
                WebViewActivity.REQUEST_CODE_CREDENTIALS
            )
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
                    IconButton(onClick = { viewModel.goBack() }, enabled = webViewState.canGoBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    IconButton(onClick = { viewModel.goForward() }, enabled = webViewState.canGoForward) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { viewModel.toggleDesktopMode() }) {
                        Icon(
                            if (isDesktopMode) Icons.Default.Computer else Icons.Default.Smartphone,
                            contentDescription = if (isDesktopMode) "Desktop mode" else "Mobile mode"
                        )
                    }
                    IconButton(onClick = { viewModel.goHome() }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                    IconButton(onClick = { viewModel.sharePage() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { viewModel.copyUrl() }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL")
                    }
                    IconButton(onClick = { showFindDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Find in page")
                    }
                    IconButton(onClick = { viewModel.printPage() }) {
                        Icon(Icons.Default.Print, contentDescription = "Print")
                    }
                    IconButton(onClick = { viewModel.zoomIn() }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in")
                    }
                    IconButton(onClick = { viewModel.zoomOut() }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out")
                    }
                    IconButton(onClick = { viewModel.toggleConsole() }) {
                        Icon(
                            if (showConsole) Icons.Default.CodeOff else Icons.Default.Code,
                            contentDescription = "Toggle console"
                        )
                    }
                    IconButton(onClick = { viewModel.toggleAdblock() }) {
                        Icon(
                            if (isAdblockEnabled) Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = "Toggle adblock"
                        )
                    }
                    IconButton(onClick = { viewModel.toggleAutoScroll() }) {
                        Icon(
                            if (isAutoScrollEnabled) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Toggle auto-scroll"
                        )
                    }
                    IconButton(onClick = { viewModel.toggleAutoClick() }) {
                        Icon(
                            if (isAutoClickEnabled) Icons.Default.TouchApp else Icons.Default.DoNotTouch,
                            contentDescription = "Toggle auto-click"
                        )
                    }
                    IconButton(onClick = {
                        val currentUrl = webViewState.currentUrl ?: webApp?.url ?: return@IconButton
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                    }
                    IconButton(onClick = {
                        webApp?.let { app ->
                            val intent = Intent(
                                context,
                                com.cylonid.nativealpha.service.FloatingWindowService::class.java
                            ).apply {
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
                    IconButton(onClick = { viewModel.openCredentialKeeper() }) {
                        Icon(Icons.Default.Lock, contentDescription = "Credentials")
                    }
                    IconButton(onClick = { viewModel.openClipboardManager() }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Clipboard")
                    }
                    IconButton(onClick = { viewModel.openDownloadHistory() }) {
                        Icon(Icons.Default.History, contentDescription = "Downloads")
                    }
                    IconButton(onClick = { viewModel.takeScreenshot() }) {
                        Icon(Icons.Default.Camera, contentDescription = "Screenshot")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (webViewState.isLoading) {
                LinearProgressIndicator(
                    progress = { webViewState.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
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
                            context = ctx,
                            onPageStarted = { url -> viewModel.onPageStarted(url) },
                            onPageFinished = { url ->
                                viewModel.onPageFinished(url)
                                webApp?.let { app ->
                                    if (app.isDarkModeEnabled) {
                                        injectDarkMode(this)
                                    }
                                }
                            },
                            onDownloadStart = { _, downloadUrl ->
                                viewModel.handleDownload(downloadUrl, webApp)
                            }
                        )
                        setDownloadListener { url, _, _, _, _ ->
                            viewModel.handleDownload(url, webApp)
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                consoleMessage?.let {
                                    viewModel.addConsoleMessage(
                                        ConsoleMessageData(
                                            message = it.message(),
                                            level = it.messageLevel().ordinal,
                                            sourceId = it.sourceId() ?: "",
                                            lineNumber = it.lineNumber()
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
                                fileChooserParams: WebChromeClient.FileChooserParams?
                            ): Boolean = true
                        }
                        loadUrl(webApp?.url ?: "")
                    }
                },
                update = { webView ->
                    if (webViewState.shouldGoBack) {
                        webView.goBack(); viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldGoForward) {
                        webView.goForward(); viewModel.clearActionFlags()
                    }
                    webViewState.shouldLoadUrl?.let { url ->
                        webView.loadUrl(url); viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldReload) {
                        webView.reload(); viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldRefresh) {
                        webView.reload(); viewModel.refresh()
                    }
                    webViewState.javaScriptToExecute?.let { js ->
                        webView.evaluateJavascript(js, null)
                        viewModel.clearJavaScriptCommand()
                    }
                    viewModel.updateNavigationState(webView.canGoBack(), webView.canGoForward())
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
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldPrint) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                            val printAdapter = webView.createPrintDocumentAdapter("WAOS Print")
                            printManager.print("WAOS Document", printAdapter, null)
                        }
                        viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldZoomIn) {
                        webView.zoomIn(); viewModel.clearActionFlags()
                    }
                    if (webViewState.shouldTakeScreenshot) {
                        viewModel.clearActionFlags()
                    }
                },
                modifier = Modifier.weight(1f)
            )

            if (showConsole) {
                ConsolePanel(
                    messages = consoleMessages,
                    onExecuteCommand = { command -> viewModel.executeJavaScript(command) },
                    modifier = Modifier.height(200.dp)
                )
            }

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

            webViewState.error?.let { error ->
                ErrorPanel(error = error, onRetry = { viewModel.refresh() })
            }
        }
    }
}

private fun injectDarkMode(webView: WebView) {
    webView.evaluateJavascript(
        """
        (function() {
            const style = document.createElement('style');
            style.textContent = `
                html { filter: invert(1) hue-rotate(180deg); }
                img, video, canvas, svg { filter: invert(1) hue-rotate(180deg); }
            `;
            document.head.appendChild(style);
        })();
        """.trimIndent(),
        null
    )
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
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(messages) { message ->
                ConsoleMessageItem(message)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
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
    val levelName = when (message.level) {
        ConsoleMessage.MessageLevel.ERROR.ordinal -> "ERROR"
        ConsoleMessage.MessageLevel.WARNING.ordinal -> "WARNING"
        else -> "LOG"
    }
    val color = when (message.level) {
        ConsoleMessage.MessageLevel.ERROR.ordinal -> MaterialTheme.colorScheme.error
        ConsoleMessage.MessageLevel.WARNING.ordinal -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = "[$levelName] ${message.sourceId}:${message.lineNumber} ${message.message}",
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
                    OutlinedButton(onClick = onFindPrevious, modifier = Modifier.weight(1f)) {
                        Text("Previous")
                    }
                    OutlinedButton(onClick = onFindNext, modifier = Modifier.weight(1f)) {
                        Text("Next")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ErrorPanel(error: String, onRetry: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
            Text(text = "Failed to load page", style = MaterialTheme.typography.titleMedium)
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
