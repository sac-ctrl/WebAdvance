package com.cylonid.nativealpha.viewmodel

import androidx.work.*
import com.cylonid.nativealpha.worker.RefreshWorker
import java.util.concurrent.TimeUnit

@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val repository: WebAppRepository,
    private val downloadManager: DownloadManager,
    private val linkSystem: LinkManagementSystem,
    private val historyTracker: LinkHistoryTracker,
    private val workManager: WorkManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val webAppId: Long = savedStateHandle.get<Long>("webAppId") ?: 0L

    private val _webApp = MutableStateFlow<WebApp?>(null)
    val webApp: StateFlow<WebApp?> = _webApp

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _webViewState = MutableStateFlow(WebViewState())
    val webViewState: StateFlow<WebViewState> = _webViewState.asStateFlow()

    private val _consoleMessages = MutableStateFlow<List<ConsoleMessageData>>(emptyList())
    val consoleMessages: StateFlow<List<ConsoleMessageData>> = _consoleMessages.asStateFlow()

    private val _showConsole = MutableStateFlow(false)
    val showConsole: StateFlow<Boolean> = _showConsole.asStateFlow()

    private val _isDesktopMode = MutableStateFlow(false)
    val isDesktopMode: StateFlow<Boolean> = _isDesktopMode.asStateFlow()

    private val _isAdblockEnabled = MutableStateFlow(true)
    val isAdblockEnabled: StateFlow<Boolean> = _isAdblockEnabled.asStateFlow()

    private val _isAutoScrollEnabled = MutableStateFlow(false)
    val isAutoScrollEnabled: StateFlow<Boolean> = _isAutoScrollEnabled.asStateFlow()

    private val _isAutoClickEnabled = MutableStateFlow(false)
    val isAutoClickEnabled: StateFlow<Boolean> = _isAutoClickEnabled.asStateFlow()

    init {
        // Don't load automatically, wait for explicit call
    }

    fun loadWebApp(id: Long) {
        viewModelScope.launch {
            repository.getWebAppById(id).collect { app ->
                _webApp.value = app
                _isLoading.value = false
                
                // Schedule auto-refresh if enabled
                if (app?.refreshInterval ?: 0 > 0) {
                    scheduleAutoRefresh()
                }
            }
        }
    }

    fun refreshWebApp() {
        // TODO: Implement refresh logic
    }

    fun updateScrollPosition(position: Int) {
        _webApp.value?.let { app ->
            val updatedApp = app.copy(scrollPosition = position)
            viewModelScope.launch {
                repository.updateWebApp(updatedApp)
            }
        }
    }

    fun onPageStarted(url: String) {
        _webViewState.value = _webViewState.value.copy(
            isLoading = true,
            currentUrl = url,
            error = null
        )
    }

    fun onPageFinished(url: String) {
        _webViewState.value = _webViewState.value.copy(
            isLoading = false,
            currentUrl = url,
            progress = 100
        )
    }

    fun onProgressChanged(progress: Int) {
        _webViewState.value = _webViewState.value.copy(progress = progress)
    }

    fun onError(error: String) {
        _webViewState.value = _webViewState.value.copy(
            isLoading = false,
            error = error
        )
    }

    fun refresh() {
        _webViewState.value = _webViewState.value.copy(shouldRefresh = true)
    }

    fun toggleConsole() {
        _showConsole.value = !_showConsole.value
    }

    fun addConsoleMessage(message: ConsoleMessageData) {
        val currentMessages = _consoleMessages.value.toMutableList()
        currentMessages.add(message)
        // Keep only last 100 messages
        if (currentMessages.size > 100) {
            currentMessages.removeAt(0)
        }
        _consoleMessages.value = currentMessages
    }

    fun executeJavaScript(command: String) {
        // This would be called from the WebViewActivity to execute JS
        _webViewState.value = _webViewState.value.copy(
            javaScriptToExecute = command
        )
    }

    fun clearJavaScriptCommand() {
        _webViewState.value = _webViewState.value.copy(
            javaScriptToExecute = null
        )
    }

    // Toolbar action methods
    fun goBack() {
        _webViewState.value = _webViewState.value.copy(shouldGoBack = true)
    }

    fun goForward() {
        _webViewState.value = _webViewState.value.copy(shouldGoForward = true)
    }

    fun goHome() {
        webApp.value?.let { app ->
            _webViewState.value = _webViewState.value.copy(
                shouldLoadUrl = app.url
            )
        }
    }

    fun toggleDesktopMode() {
        _isDesktopMode.value = !_isDesktopMode.value
        _webViewState.value = _webViewState.value.copy(
            shouldReload = true
        )
    }

    fun toggleAdblock() {
        _isAdblockEnabled.value = !_isAdblockEnabled.value
        _webViewState.value = _webViewState.value.copy(
            shouldReload = true
        )
    }

    fun toggleAutoScroll() {
        _isAutoScrollEnabled.value = !_isAutoScrollEnabled.value
        if (_isAutoScrollEnabled.value) {
            executeJavaScript("window.waosAutoScroll.start(2);")
        } else {
            executeJavaScript("window.waosAutoScroll.stop();")
        }
    }

    fun toggleAutoClick() {
        _isAutoClickEnabled.value = !_isAutoClickEnabled.value
        // Note: This would need a selector to be configured
        // For now, just toggle the state
    }

    fun sharePage() {
        _webViewState.value = _webViewState.value.copy(
            shouldShareUrl = _webViewState.value.currentUrl
        )
    }

    fun copyUrl() {
        _webViewState.value = _webViewState.value.copy(
            shouldCopyUrl = _webViewState.value.currentUrl
        )
    }

    fun copyUrlWithFormat(format: LinkManagementSystem.LinkFormat) {
        val currentUrl = _webViewState.value.currentUrl
        val pageTitle = webApp.value?.name ?: "Web Page"

        viewModelScope.launch {
            val formattedUrl = linkSystem.copyUrl(currentUrl, pageTitle, format)
            historyTracker.recordAction(
                url = currentUrl,
                pageTitle = pageTitle,
                action = "copy",
                format = format.name
            )
        }
    }

    fun saveCurrentLink() {
        val currentUrl = _webViewState.value.currentUrl
        val pageTitle = webApp.value?.name ?: "Web Page"

        linkSystem.saveLink(currentUrl, pageTitle)
        viewModelScope.launch {
            historyTracker.recordAction(
                url = currentUrl,
                pageTitle = pageTitle,
                action = "save"
            )
        }
    }

    fun shareCurrentPage() {
        val currentUrl = _webViewState.value.currentUrl
        val pageTitle = webApp.value?.name ?: "Web Page"

        linkSystem.shareLink(currentUrl, pageTitle)
        viewModelScope.launch {
            historyTracker.recordAction(
                url = currentUrl,
                pageTitle = pageTitle,
                action = "share"
            )
        }
    }

    fun getCurrentPageInfo(callback: (String, String) -> Unit) {
        val currentUrl = _webViewState.value.currentUrl
        val pageTitle = webApp.value?.name ?: "Web Page"
        callback(currentUrl, pageTitle)
    }

    fun printPage() {
        _webViewState.value = _webViewState.value.copy(
            shouldPrint = true
        )
    }

    fun printPage() {
        _webViewState.value = _webViewState.value.copy(
            shouldPrint = true
        )
    }

    fun zoomIn() {
        _webViewState.value = _webViewState.value.copy(
            shouldZoomIn = true
        )
    }

    fun zoomOut() {
        _webViewState.value = _webViewState.value.copy(
            shouldZoomOut = true
        )
    }

    fun addToFloatingWindow() {
        _webViewState.value = _webViewState.value.copy(
            shouldAddToFloatingWindow = true
        )
    }

    fun openCredentialKeeper() {
        _webViewState.value = _webViewState.value.copy(
            shouldOpenCredentialKeeper = true
        )
    }

    fun openClipboardManager() {
        _webViewState.value = _webViewState.value.copy(
            shouldOpenClipboardManager = true
        )
    }

    fun openDownloadHistory() {
        _webViewState.value = _webViewState.value.copy(
            shouldOpenDownloadHistory = true
        )
    }

    fun captureScreenshot(webView: WebView, context: Context) {
        viewModelScope.launch {
            try {
                webView.buildDrawingCache()
                val bitmap = webView.drawingCache
                if (bitmap != null) {
                    // Save bitmap to file
                    val filename = "screenshot_${System.currentTimeMillis()}.png"
                    val file = java.io.File(context.getExternalFilesDir(null), filename)
                    val stream = java.io.FileOutputStream(file)
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                    stream.close()
                    
                    // Update webApp with thumbnail
                    webApp.value?.let { app ->
                        val updatedApp = app.copy(thumbnail = bitmap)
                        repository.updateWebApp(updatedApp)
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun shouldHandleDownload(url: String): Boolean {
        // Check if URL is a downloadable file
        val downloadExtensions = listOf(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".zip", ".rar", ".7z", ".tar", ".gz",
            ".mp3", ".mp4", ".avi", ".mkv", ".mov", ".wmv",
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
            ".apk", ".exe", ".dmg"
        )
        return downloadExtensions.any { url.lowercase().endsWith(it) }
    }

    fun handleDownload(url: String, webApp: WebApp? = null) {
        downloadManager.downloadFile(webApp?.id ?: 0L, url, null, webApp)
        _webViewState.value = _webViewState.value.copy(
            shouldDownloadUrl = null
        )
    }

    // Clear action flags after handling
    fun clearActionFlags() {
        _webViewState.value = _webViewState.value.copy(
            shouldGoBack = false,
            shouldGoForward = false,
            shouldLoadUrl = null,
            shouldReload = false,
            shouldShareUrl = null,
            shouldCopyUrl = null,
            shouldShowFindInPage = false,
            shouldPrint = false,
            shouldZoomIn = false,
            shouldZoomOut = false,
            shouldAddToFloatingWindow = false,
            shouldOpenCredentialKeeper = false,
            shouldOpenClipboardManager = false,
            shouldOpenDownloadHistory = false,
            shouldTakeScreenshot = false,
            shouldDownloadUrl = null
        )
    }

    fun scheduleAutoRefresh() {
        webApp.value?.let { app ->
            if (app.refreshInterval > 0) {
                val refreshWork = PeriodicWorkRequestBuilder<RefreshWorker>(
                    app.refreshInterval.toLong(),
                    TimeUnit.MINUTES
                ).setInputData(
                    workDataOf("webAppId" to app.id)
                ).build()

                workManager.enqueueUniquePeriodicWork(
                    "refresh_${app.id}",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    refreshWork
                )
            }
        }
    }

    fun cancelAutoRefresh() {
        webApp.value?.let { app ->
            workManager.cancelUniqueWork("refresh_${app.id}")
        }
    }

    fun scheduleScreenshotCapture(intervalHours: Int = 24) {
        webApp.value?.let { app ->
            val screenshotWork = PeriodicWorkRequestBuilder<ScreenshotWorker>(
                intervalHours.toLong(),
                TimeUnit.HOURS
            ).setInputData(
                workDataOf("webAppId" to app.id)
            ).build()

            workManager.enqueueUniquePeriodicWork(
                "screenshot_${app.id}",
                ExistingPeriodicWorkPolicy.REPLACE,
                screenshotWork
            )
        }
    }

    fun cancelScreenshotCapture() {
        webApp.value?.let { app ->
            workManager.cancelUniqueWork("screenshot_${app.id}")
        }
    }

    fun autoFillCredentials(username: String, password: String) {
        val js = """
            (function() {
                const inputs = document.querySelectorAll('input[type="text"], input[type="email"], input:not([type])');
                const passwords = document.querySelectorAll('input[type="password"]');
                
                if (inputs.length > 0) {
                    inputs[0].value = '$username';
                    inputs[0].dispatchEvent(new Event('input', { bubbles: true }));
                    inputs[0].dispatchEvent(new Event('change', { bubbles: true }));
                }
                
                if (passwords.length > 0) {
                    passwords[0].value = '$password';
                    passwords[0].dispatchEvent(new Event('input', { bubbles: true }));
                    passwords[0].dispatchEvent(new Event('change', { bubbles: true }));
                }
            })();
        """.trimIndent()
        
        executeJavaScript(js)
    }
}

data class WebViewState(
    val isLoading: Boolean = false,
    val currentUrl: String = "",
    val progress: Int = 0,
    val error: String? = null,
    val shouldRefresh: Boolean = false,
    val javaScriptToExecute: String? = null,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    // Action flags
    val shouldGoBack: Boolean = false,
    val shouldGoForward: Boolean = false,
    val shouldLoadUrl: String? = null,
    val shouldReload: Boolean = false,
    val shouldShareUrl: String? = null,
    val shouldCopyUrl: String? = null,
    val shouldShowFindInPage: Boolean = false,
    val shouldPrint: Boolean = false,
    val shouldZoomIn: Boolean = false,
    val shouldZoomOut: Boolean = false,
    val shouldAddToFloatingWindow: Boolean = false,
    val shouldOpenCredentialKeeper: Boolean = false,
    val shouldOpenClipboardManager: Boolean = false,
    val shouldOpenDownloadHistory: Boolean = false,
    val shouldTakeScreenshot: Boolean = false,
    val shouldDownloadUrl: String? = null
)