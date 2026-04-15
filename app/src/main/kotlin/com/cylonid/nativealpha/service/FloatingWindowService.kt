package com.cylonid.nativealpha.service

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.*
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.model.WindowEntity
import com.cylonid.nativealpha.model.WindowPresetEntity
import com.cylonid.nativealpha.repository.WebAppRepository
import com.cylonid.nativealpha.webview.WebViewClientWithDownload
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class FloatingWindowService : Service() {

    @Inject
    lateinit var windowManager: WindowManager

    @Inject
    lateinit var webAppRepository: WebAppRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val floatingWindows = mutableMapOf<Long, FloatingWindowView>()
    private val _openWindows = MutableStateFlow<List<WindowEntity>>(emptyList())
    val openWindows: StateFlow<List<WindowEntity>> = _openWindows.asStateFlow()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val webAppId = intent?.getLongExtra("webAppId", 0L) ?: 0L
        val webAppUrl = intent?.getStringExtra("webAppUrl") ?: "https://example.com"
        val webAppName = intent?.getStringExtra("webAppName") ?: "Web App"
        val windowId = intent?.getLongExtra("windowId", 0L) ?: 0L

        when (action) {
            ACTION_ADD_WINDOW -> addFloatingWindow(webAppId, webAppUrl, webAppName)
            ACTION_REMOVE_WINDOW -> removeFloatingWindow(windowId)
            ACTION_CLOSE_ALL -> closeAllWindows()
            ACTION_MINIMIZE_WINDOW -> minimizeWindow(windowId)
            ACTION_MAXIMIZE_WINDOW -> maximizeWindow(windowId)
        }

        return START_STICKY
    }

    private fun addFloatingWindow(webAppId: Long, webAppUrl: String, webAppName: String) {
        if (floatingWindows.containsKey(webAppId)) return

        val layoutParams = WindowManager.LayoutParams().apply {
            width = 800
            height = 600
            x = 100
            y = 100
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
        }

        val view = LayoutInflater.from(this).inflate(R.layout.floating_window, null).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(ContextCompat.getColor(this@FloatingWindowService, R.color.floating_window_background))
                setStroke(2, ContextCompat.getColor(this@FloatingWindowService, R.color.floating_window_resize_handle))
            }
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View?, outline: android.graphics.Outline?) {
                    outline?.setRoundRect(0, 0, view?.width ?: 0, view?.height ?: 0, 24f)
                }
            }
        }

        val webView = view.findViewById<WebView>(R.id.floatingWebView)
        val titleText = view.findViewById<TextView>(R.id.titleText)
        val overflowButton = view.findViewById<ImageButton>(R.id.toolbarOverflowButton)
        titleText.text = webAppName

        val webViewClient = WebViewClientWithDownload(
            context = this,
            onPageStarted = { /* no-op */ },
            onPageFinished = { /* no-op */ }
        ).apply {
            adblockEnabled = false
        }

        val windowView = FloatingWindowView(
            view = view,
            layoutParams = layoutParams,
            webView = webView,
            webViewClient = webViewClient,
            appName = webAppName,
            isMinimized = false,
            isMaximized = false,
            originalWidth = 800,
            originalHeight = 600,
            originalX = 100,
            originalY = 100
        )

        // Drag support via title bar
        val titleBar = view.findViewById<LinearLayout>(R.id.titleBar)
        var dragStartX = 0f
        var dragStartY = 0f
        var windowStartX = 0
        var windowStartY = 0
        titleBar?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    bringWindowToFront(webAppId)
                    dragStartX = event.rawX
                    dragStartY = event.rawY
                    windowStartX = layoutParams.x
                    windowStartY = layoutParams.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = windowStartX + (event.rawX - dragStartX).toInt()
                    layoutParams.y = windowStartY + (event.rawY - dragStartY).toInt()
                    windowManager.updateViewLayout(view, layoutParams)
                    true
                }
                else -> false
            }
        }

        // Configure WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
        }
        webView.webViewClient = webViewClient
        webView.loadUrl(webAppUrl)

        val closeButton = view.findViewById<ImageButton>(R.id.closeButton)
        val minimizeButton = view.findViewById<ImageButton>(R.id.minimizeButton)
        val maximizeButton = view.findViewById<ImageButton>(R.id.maximizeButton)

        closeButton?.setOnClickListener {
            removeFloatingWindow(webAppId)
        }

        minimizeButton?.setOnClickListener {
            minimizeWindow(webAppId)
        }

        maximizeButton?.setOnClickListener {
            maximizeWindow(webAppId)
        }

        overflowButton?.setOnClickListener {
            showToolbarPopup(it, windowView, webAppUrl)
        }

        val resizeHandle = view.findViewById<View>(R.id.resizeHandle)
        var initialWidth = 0
        var initialHeight = 0
        var initialResizeTouchX = 0f
        var initialResizeTouchY = 0f

        resizeHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialWidth = layoutParams.width
                    initialHeight = layoutParams.height
                    initialResizeTouchX = event.rawX
                    initialResizeTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialResizeTouchX).toInt()
                    val deltaY = (event.rawY - initialResizeTouchY).toInt()
                    layoutParams.width = (initialWidth + deltaX).coerceAtLeast(300)
                    layoutParams.height = (initialHeight + deltaY).coerceAtLeast(200)
                    windowManager.updateViewLayout(view, layoutParams)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, layoutParams)
        floatingWindows[webAppId] = windowView
        updateOpenWindows()

        serviceScope.launch {
            loadWebAppConfig(webAppId, webView, titleText, windowView)
        }
    }

    private fun showToolbarPopup(anchor: View, windowView: FloatingWindowView, webAppUrl: String) {
        val popupMenu = PopupMenu(this, anchor)
        val toolbarItems = listOf(
            ACTION_TOOLBAR_BACK to "Back",
            ACTION_TOOLBAR_FORWARD to "Forward",
            ACTION_TOOLBAR_HOME to "Home",
            ACTION_TOOLBAR_REFRESH to "Refresh",
            ACTION_TOOLBAR_DESKTOP to if (windowView.desktopModeEnabled) "Desktop mode: On" else "Desktop mode: Off",
            ACTION_TOOLBAR_ADBLOCK to if (windowView.adblockEnabled) "Adblock: ${if (windowView.adblockEnabled) "On" else "Off"}" else "Adblock: Off",
            ACTION_TOOLBAR_AUTOSCROLL to if (windowView.autoScrollEnabled) "Auto scroll: On" else "Auto scroll: Off",
            ACTION_TOOLBAR_AUTOCLICK to if (windowView.autoClickEnabled) "Auto click: On" else "Auto click: Off",
            ACTION_TOOLBAR_OPEN_BROWSER to "Open in browser",
            ACTION_TOOLBAR_SHARE to "Share URL",
            ACTION_TOOLBAR_COPY_URL to "Copy URL"
        )
        toolbarItems.forEach { (id, title) ->
            popupMenu.menu.add(0, id, 0, title)
        }
        popupMenu.setOnMenuItemClickListener { item ->
            handleToolbarAction(item.itemId, windowView, webAppUrl)
            true
        }
        popupMenu.show()
    }

    private fun handleToolbarAction(actionId: Int, windowView: FloatingWindowView, webAppUrl: String) {
        val webView = windowView.webView
        when (actionId) {
            ACTION_TOOLBAR_BACK -> if (webView.canGoBack()) webView.goBack()
            ACTION_TOOLBAR_FORWARD -> if (webView.canGoForward()) webView.goForward()
            ACTION_TOOLBAR_HOME -> webView.loadUrl(webAppUrl)
            ACTION_TOOLBAR_REFRESH -> webView.reload()
            ACTION_TOOLBAR_DESKTOP -> {
                windowView.desktopModeEnabled = !windowView.desktopModeEnabled
                applyDesktopMode(windowView)
            }
            ACTION_TOOLBAR_ADBLOCK -> {
                windowView.adblockEnabled = !windowView.adblockEnabled
                applyAdblock(windowView)
            }
            ACTION_TOOLBAR_AUTOSCROLL -> {
                windowView.autoScrollEnabled = !windowView.autoScrollEnabled
                applyAutoScroll(windowView)
            }
            ACTION_TOOLBAR_AUTOCLICK -> {
                windowView.autoClickEnabled = !windowView.autoClickEnabled
                applyAutoClick(windowView)
            }
            ACTION_TOOLBAR_OPEN_BROWSER -> {
                val currentUrl = webView.url ?: webAppUrl
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }
            ACTION_TOOLBAR_SHARE -> {
                val currentUrl = webView.url ?: webAppUrl
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, currentUrl)
                            putExtra(Intent.EXTRA_SUBJECT, windowView.view.contentDescription ?: "Web page")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                        "Share URL"
                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                )
            }
            ACTION_TOOLBAR_COPY_URL -> {
                val currentUrl = webView.url ?: webAppUrl
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("URL", currentUrl))
            }
        }
    }

    private fun applyDesktopMode(windowView: FloatingWindowView) {
        val webView = windowView.webView
        if (windowView.originalUserAgent == null) {
            windowView.originalUserAgent = webView.settings.userAgentString
        }
        webView.settings.userAgentString = if (windowView.desktopModeEnabled) {
            DESKTOP_USER_AGENT
        } else {
            windowView.originalUserAgent ?: webView.settings.userAgentString
        }
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        if (windowView.desktopModeEnabled) {
            webView.evaluateJavascript(
                "javascript:(function(){var meta=document.querySelector('meta[name=viewport]'); if(!meta){meta=document.createElement('meta'); meta.name='viewport'; document.head.appendChild(meta);} meta.content='width=device-width, initial-scale=0.5, maximum-scale=3.0, user-scalable=yes'; var style=document.createElement('style'); style.textContent='body{min-width:100vw!important;overflow-x:auto;} @media (max-width:768px){body{font-size:14px;}}'; document.head.appendChild(style);})()",
                null
            )
        }
        webView.reload()
    }

    private fun applyAdblock(windowView: FloatingWindowView) {
        windowView.webViewClient.adblockEnabled = windowView.adblockEnabled
        windowView.webView.reload()
    }

    private fun applyAutoScroll(windowView: FloatingWindowView) {
        val webView = windowView.webView
        if (windowView.autoScrollEnabled) {
            webView.evaluateJavascript(
                "javascript:(function(){window.waosAutoScroll={interval:null,start:function(){this.stop();this.interval=setInterval(function(){window.scrollBy(0,2);},50);},stop:function(){if(this.interval){clearInterval(this.interval);this.interval=null;}}};window.waosAutoScroll.start();})()",
                null
            )
        } else {
            webView.evaluateJavascript(
                "javascript:(function(){if(window.waosAutoScroll){window.waosAutoScroll.stop();}})()",
                null
            )
        }
    }

    private fun applyAutoClick(windowView: FloatingWindowView) {
        val webView = windowView.webView
        if (windowView.autoClickEnabled) {
            webView.evaluateJavascript(
                "javascript:(function(){window.waosAutoClick={interval:null,start:function(){this.stop();this.interval=setInterval(function(){var btn=document.querySelector('button'); if(btn) btn.click();},1500);},stop:function(){if(this.interval){clearInterval(this.interval);this.interval=null;}}};window.waosAutoClick.start();})()",
                null
            )
        } else {
            webView.evaluateJavascript(
                "javascript:(function(){if(window.waosAutoClick){window.waosAutoClick.stop();}})()",
                null
            )
        }
    }

    private suspend fun loadWebAppConfig(webAppId: Long, webView: WebView, titleText: TextView, windowView: FloatingWindowView) {
        val app = webAppRepository.getWebAppById(webAppId).firstOrNull()
        app?.let { webApp ->
            withContext(Dispatchers.Main) {
                titleText.text = webApp.name
                windowView.appName = webApp.name
                webView.settings.javaScriptEnabled = webApp.isJavaScriptEnabled
                webView.settings.builtInZoomControls = webApp.isEnableZooming
                webView.settings.setSupportZoom(true)
                webView.settings.displayZoomControls = false
                webView.settings.useWideViewPort = true
                webView.settings.loadWithOverviewMode = true

                val requestedUserAgent = webApp.userAgent?.takeIf { it.isNotBlank() }
                val isSavedDesktopMode = requestedUserAgent?.contains("Mozilla/5.0 (X11; Linux x86_64)") == true || webApp.isRequestDesktop
                var shouldReload = false
                if (requestedUserAgent != null) {
                    webView.settings.userAgentString = requestedUserAgent
                    shouldReload = true
                } else if (isSavedDesktopMode) {
                    webView.settings.userAgentString = DESKTOP_USER_AGENT
                    shouldReload = true
                }

                windowView.originalUserAgent = webView.settings.userAgentString
                windowView.desktopModeEnabled = isSavedDesktopMode
                windowView.adblockEnabled = webApp.isUseAdblock || webApp.isAdblockEnabled
                windowView.webViewClient.adblockEnabled = windowView.adblockEnabled

                if (webApp.floatingWindowWidth > 0) {
                    windowView.layoutParams.width = webApp.floatingWindowWidth
                    windowView.originalWidth = webApp.floatingWindowWidth
                }
                if (webApp.floatingWindowHeight > 0) {
                    windowView.layoutParams.height = webApp.floatingWindowHeight
                    windowView.originalHeight = webApp.floatingWindowHeight
                }
                windowManager.updateViewLayout(windowView.view, windowView.layoutParams)

                if (windowView.desktopModeEnabled) {
                    applyDesktopMode(windowView)
                }
                if (windowView.adblockEnabled) {
                    applyAdblock(windowView)
                }
                if (shouldReload && !windowView.desktopModeEnabled && !windowView.adblockEnabled) {
                    webView.reload()
                }
            }
        }
    }

    private fun removeFloatingWindow(windowId: Long) {
        floatingWindows[windowId]?.let { windowView ->
            windowManager.removeView(windowView.view)
            floatingWindows.remove(windowId)
            updateOpenWindows()
        }
    }

    private fun minimizeWindow(windowId: Long) {
        floatingWindows[windowId]?.let { windowView ->
            windowView.isMinimized = !windowView.isMinimized
            if (windowView.isMinimized) {
                // Save original size
                windowView.originalWidth = windowView.layoutParams.width
                windowView.originalHeight = windowView.layoutParams.height

                // Minimize to title bar only
                windowView.layoutParams.width = 300
                windowView.layoutParams.height = 50
                windowView.webView.visibility = View.GONE
            } else {
                // Restore original size
                windowView.layoutParams.width = windowView.originalWidth
                windowView.layoutParams.height = windowView.originalHeight
                windowView.webView.visibility = View.VISIBLE
            }
            windowManager.updateViewLayout(windowView.view, windowView.layoutParams)
        }
    }

    private fun maximizeWindow(windowId: Long) {
        floatingWindows[windowId]?.let { windowView ->
            windowView.isMaximized = !windowView.isMaximized
            if (windowView.isMaximized) {
                // Save current position and size
                windowView.originalX = windowView.layoutParams.x
                windowView.originalY = windowView.layoutParams.y
                windowView.originalWidth = windowView.layoutParams.width
                windowView.originalHeight = windowView.layoutParams.height

                // Maximize to screen size
                val displayMetrics = resources.displayMetrics
                windowView.layoutParams.x = 0
                windowView.layoutParams.y = 0
                windowView.layoutParams.width = displayMetrics.widthPixels
                windowView.layoutParams.height = displayMetrics.heightPixels
            } else {
                // Restore previous size and position
                windowView.layoutParams.x = windowView.originalX
                windowView.layoutParams.y = windowView.originalY
                windowView.layoutParams.width = windowView.originalWidth
                windowView.layoutParams.height = windowView.originalHeight
            }
            windowManager.updateViewLayout(windowView.view, windowView.layoutParams)
        }
    }

    private fun closeAllWindows() {
        floatingWindows.values.forEach { windowView ->
            windowManager.removeView(windowView.view)
        }
        floatingWindows.clear()
        updateOpenWindows()
        stopSelf()
    }

    private fun bringWindowToFront(windowId: Long) {
        floatingWindows[windowId]?.let { windowView ->
            try {
                windowView.view.bringToFront()
                windowManager.updateViewLayout(windowView.view, windowView.layoutParams)
            } catch (ignored: Exception) {
            }
        }
    }

    fun getCurrentWindows(): List<WindowEntity> {
        return floatingWindows.map { (id, windowView) ->
            WindowEntity(
                id = id,
                appName = windowView.appName,
                x = windowView.layoutParams.x,
                y = windowView.layoutParams.y,
                width = windowView.layoutParams.width,
                height = windowView.layoutParams.height,
                isMinimized = windowView.isMinimized
            )
        }
    }

    fun loadPreset(preset: WindowPresetEntity) {
        closeAllWindows()
        // TODO: Implement loading preset windows
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updateOpenWindows() {
        _openWindows.value = getCurrentWindows()
    }

    companion object {
        const val ACTION_ADD_WINDOW = "ADD_WINDOW"
        const val ACTION_REMOVE_WINDOW = "REMOVE_WINDOW"
        const val ACTION_CLOSE_ALL = "CLOSE_ALL"
        const val ACTION_MINIMIZE_WINDOW = "MINIMIZE_WINDOW"
        const val ACTION_MAXIMIZE_WINDOW = "MAXIMIZE_WINDOW"

        const val ACTION_TOOLBAR_BACK = 101
        const val ACTION_TOOLBAR_FORWARD = 102
        const val ACTION_TOOLBAR_HOME = 103
        const val ACTION_TOOLBAR_REFRESH = 104
        const val ACTION_TOOLBAR_DESKTOP = 105
        const val ACTION_TOOLBAR_ADBLOCK = 106
        const val ACTION_TOOLBAR_AUTOSCROLL = 107
        const val ACTION_TOOLBAR_AUTOCLICK = 108
        const val ACTION_TOOLBAR_OPEN_BROWSER = 109
        const val ACTION_TOOLBAR_SHARE = 110
        const val ACTION_TOOLBAR_COPY_URL = 111

        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}

data class FloatingWindowView(
    val view: View,
    val layoutParams: WindowManager.LayoutParams,
    val webView: WebView,
    val webViewClient: WebViewClientWithDownload,
    var appName: String,
    var isMinimized: Boolean = false,
    var isMaximized: Boolean = false,
    var originalWidth: Int = 800,
    var originalHeight: Int = 600,
    var originalX: Int = 100,
    var originalY: Int = 100,
    var desktopModeEnabled: Boolean = false,
    var adblockEnabled: Boolean = false,
    var autoScrollEnabled: Boolean = false,
    var autoClickEnabled: Boolean = false,
    var originalUserAgent: String? = null
)