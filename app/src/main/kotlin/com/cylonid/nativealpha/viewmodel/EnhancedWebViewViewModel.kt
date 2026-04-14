package com.cylonid.nativealpha.viewmodel

import android.content.Context
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.webview.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnhancedWebViewViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var currentWebView: WebView? = null
    private var sessionManager: SessionManager? = null
    private var jsInterface: WebAppJavaScriptInterface? = null

    private val _webApp = MutableStateFlow<WebApp?>(null)
    val webApp: StateFlow<WebApp?> = _webApp.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    private val _pageTitle = MutableStateFlow("")
    val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    private val _downloadedFiles = MutableStateFlow<List<String>>(emptyList())
    val downloadedFiles: StateFlow<List<String>> = _downloadedFiles.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    fun loadWebApp(webAppId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // In real implementation, fetch from database
                // For now, create a dummy app
                val app = WebApp(
                    id = webAppId,
                    name = "Web App $webAppId",
                    url = "https://example.com",
                    isJavaScriptEnabled = true,
                    isAdblockEnabled = false
                )
                _webApp.value = app
                setupSessionManager(app)
                _currentUrl.value = app.url
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load web app"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun setupSessionManager(app: WebApp) {
        sessionManager = SessionManager(context, app.id, app.name)
    }

    /**
     * Setup WebView with all configurations
     */
    fun setupWebView(webView: WebView, app: WebApp) {
        currentWebView = webView
        
        webView.apply {
            settings.apply {
                javaScriptEnabled = app.isJavaScriptEnabled
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                userAgentString = app.userAgent ?: settings.userAgentString
                cacheMode = when (app.cacheMode) {
                    "no-cache" -> android.webkit.WebSettings.LOAD_NO_CACHE
                    "reload" -> android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
                    else -> android.webkit.WebSettings.LOAD_DEFAULT
                }
            }

            // Setup WebViewClient with download support
            webViewClient = WebViewClientWithDownload(
                context,
                onPageStarted = { url ->
                    _currentUrl.value = url
                    _isLoading.value = true
                    _canGoBack.value = canGoBack()
                    _canGoForward.value = canGoForward()
                },
                onPageFinished = { url ->
                    _isLoading.value = false
                    _canGoBack.value = canGoBack()
                    _canGoForward.value = canGoForward()
                    injectJavaScript(webView)
                    sessionManager?.saveSessionState("page_loaded", url)
                },
                onDownloadStart = { filename, url ->
                    addDownloadedFile(filename)
                }
            )

            // Setup WebChromeClient for progress and dialogs
            webChromeClient = WebAppChromeClient(
                onProgressChanged = { progress ->
                    _loadingProgress.value = progress
                },
                onTitleReceived = { title ->
                    _pageTitle.value = title
                }
            )

            // Setup JavaScript interface
            setupJavaScriptInterface(webView, app)

            // Load initial URL
            loadUrl(app.url)
        }
    }

    private fun setupJavaScriptInterface(webView: WebView, app: WebApp) {
        jsInterface = WebAppJavaScriptInterface(
            context,
            onClipboardCopy = { text ->
                // Handle clipboard copy - integrate with ClipboardRepository
            },
            onClipboardPaste = {
                // Return pasted content from system clipboard if available
                ""
            },
            onScrollPositionChange = { position ->
                // Save scroll position
                sessionManager?.setSessionValue("scroll_position", position.toString())
            },
            onDOMChange = {
                // Handle page changes for notifications/refresh
            }
        )
        
        webView.addJavascriptInterface(jsInterface!!, "WebApp")
    }

    private fun injectJavaScript(webView: WebView) {
        if (webApp.value?.isJavaScriptEnabled == true) {
            // Inject script for clipboard detection
            webView.evaluateJavascript(
                """
                (function() {
                    // Detect page changes with MutationObserver
                    const observer = new MutationObserver(function() {
                        WebApp.reportPageChange('DOM modified');
                    });
                    
                    observer.observe(document.body, {
                        childList: true,
                        subtree: true,
                        attributes: true
                    });
                    
                    // Track scroll position
                    window.addEventListener('scroll', function() {
                        WebApp.logScrollPosition(window.scrollY);
                    });
                    
                    // Intercept copy events
                    document.addEventListener('copy', function(e) {
                        WebApp.copyToClipboard(window.getSelection().toString());
                    });
                })();
                """.trimIndent()
            ) { result ->
                // Script injected successfully
            }
        }
    }

    fun refresh() {
        currentWebView?.reload()
    }

    fun goBack() {
        if (currentWebView?.canGoBack() == true) {
            currentWebView?.goBack()
        }
    }

    fun goForward() {
        if (currentWebView?.canGoForward() == true) {
            currentWebView?.goForward()
        }
    }

    fun goHome() {
        webApp.value?.let { app ->
            currentWebView?.loadUrl(app.url)
        }
    }

    fun loadUrl(url: String) {
        currentWebView?.loadUrl(url)
    }

    fun findInPage(text: String) {
        currentWebView?.findAllAsync(text)
    }

    fun zoomIn() {
        currentWebView?.evaluateJavascript("document.body.style.zoom = (parseFloat(document.body.style.zoom || 1) + 0.1).toString();", null)
    }

    fun zoomOut() {
        currentWebView?.evaluateJavascript("document.body.style.zoom = Math.max(0.1, parseFloat(document.body.style.zoom || 1) - 0.1).toString();", null)
    }

    fun resetZoom() {
        currentWebView?.evaluateJavascript("document.body.style.zoom = '1';", null)
    }

    fun executeJavaScript(script: String) {
        currentWebView?.evaluateJavascript(script) { result ->
            // Handle result
        }
    }

    fun getScrollPosition(): Int {
        return sessionManager?.getSessionValue("scroll_position", "0")?.toIntOrNull() ?: 0
    }

    fun restoreScrollPosition() {
        val position = getScrollPosition()
        if (position > 0) {
            currentWebView?.scrollTo(0, position)
        }
    }

    fun takeScreenshot(): android.graphics.Bitmap? {
        return currentWebView?.capturePicture()?.let { picture ->
            android.graphics.Bitmap.createBitmap(picture.width, picture.height, android.graphics.Bitmap.Config.ARGB_8888).apply {
                val canvas = android.graphics.Canvas(this)
                picture.draw(canvas)
            }
        }
    }

    private fun addDownloadedFile(filename: String) {
        val current = _downloadedFiles.value.toMutableList()
        current.add(0, filename)
        _downloadedFiles.value = current.take(100) // Keep last 100
    }

    fun clearCookies() {
        android.webkit.CookieManager.getInstance().removeAllCookies { }
    }

    fun clearCache() {
        currentWebView?.clearCache(true)
    }

    fun clearHistory() {
        currentWebView?.clearHistory()
    }

    fun exportSession(file: java.io.File): Boolean {
        return sessionManager?.exportSession(file) ?: false
    }

    fun importSession(file: java.io.File): Boolean {
        return sessionManager?.importSession(file) ?: false
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager?.saveSessionState("app_cleared")
        currentWebView?.stopLoading()
        currentWebView?.destroy()
    }
}
