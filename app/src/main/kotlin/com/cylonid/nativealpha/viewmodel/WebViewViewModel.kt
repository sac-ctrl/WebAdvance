package com.cylonid.nativealpha.viewmodel

import com.cylonid.nativealpha.manager.ClipboardManager
import com.cylonid.nativealpha.model.WebApp

@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val repository: WebAppRepository,
    private val downloadManager: DownloadManager,
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
        loadWebApp(webAppId)
    }

    fun loadWebApp(id: Long) {
        viewModelScope.launch {
            repository.getWebAppById(id).collect { app ->
                _webApp.value = app
                _isLoading.value = false
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

    fun showFindInPage() {
        _webViewState.value = _webViewState.value.copy(
            shouldShowFindInPage = true
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

    fun takeScreenshot() {
        _webViewState.value = _webViewState.value.copy(
            shouldTakeScreenshot = true
        )
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

    fun updateNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        _webViewState.value = _webViewState.value.copy(
            canGoBack = canGoBack,
            canGoForward = canGoForward
        )
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