package com.cylonid.nativealpha.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.webview.WebViewClientWithDownload
import com.cylonid.nativealpha.webview.WebAppJavaScriptInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min
import javax.inject.Inject

@AndroidEntryPoint
class EnhancedFloatingWindowService : Service() {

    @Inject
    lateinit var windowManager: WindowManager

    private data class FloatingWindow(
        val id: Long,
        val webApp: WebApp,
        val view: View,
        val params: WindowManager.LayoutParams,
        var isMinimized: Boolean = false,
        var originalWidth: Int = 0,
        var originalHeight: Int = 0,
        var originalX: Int = 0,
        var originalY: Int = 0
    )

    private val floatingWindows = mutableMapOf<Long, FloatingWindow>()
    private val _openWindows = MutableStateFlow<List<WebApp>>(emptyList())
    val openWindows: StateFlow<List<WebApp>> = _openWindows.asStateFlow()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val webAppId = intent?.getLongExtra("webAppId", 0L) ?: 0L
        val windowId = intent?.getLongExtra("windowId", 0L) ?: 0L

        when (action) {
            ACTION_ADD_WINDOW -> addFloatingWindow(webAppId)
            ACTION_REMOVE_WINDOW -> removeFloatingWindow(windowId)
            ACTION_CLOSE_ALL -> closeAllWindows()
            ACTION_MINIMIZE_WINDOW -> minimizeWindow(windowId)
            ACTION_MAXIMIZE_WINDOW -> maximizeWindow(windowId)
            ACTION_BRING_TO_FRONT -> bringToFront(windowId)
        }

        return START_STICKY
    }

    private fun addFloatingWindow(webAppId: Long) {
        if (floatingWindows.containsKey(webAppId)) return
        if (floatingWindows.size >= MAX_WINDOWS) return

        try {
            val webApp = WebApp(id = webAppId, name = "Window $webAppId", url = "https://example.com")
            
            val layoutParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                gravity = Gravity.TOP or Gravity.START
                width = DEFAULT_WINDOW_WIDTH
                height = DEFAULT_WINDOW_HEIGHT
                x = 50 + (floatingWindows.size * 30)
                y = 100 + (floatingWindows.size * 30)
            }

            val containerView = createWindowView(webApp, layoutParams)
            
            windowManager.addView(containerView, layoutParams)

            floatingWindows[webAppId] = FloatingWindow(
                id = webAppId,
                webApp = webApp,
                view = containerView,
                params = layoutParams,
                originalWidth = layoutParams.width,
                originalHeight = layoutParams.height,
                originalX = layoutParams.x,
                originalY = layoutParams.y
            )

            updateWindowsList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createWindowView(webApp: WebApp, layoutParams: WindowManager.LayoutParams): View {
        return FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.WHITE)

            // Title bar
            val titleBar = LinearLayout(this@EnhancedFloatingWindowService).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    50,
                    Gravity.TOP
                )
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(ContextCompat.getColor(this@EnhancedFloatingWindowService, R.color.md_theme_primary))
                setOnTouchListener(createWindowTouchListener(layoutParams))

                // Title text
                val titleText = android.widget.TextView(this@EnhancedFloatingWindowService).apply {
                    text = webApp.name
                    setTextColor(android.graphics.Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(16, 0, 0, 0)
                }
                addView(titleText)

                // Close button
                val closeBtn = ImageButton(this@EnhancedFloatingWindowService).apply {
                    setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    setBackgroundColor(android.graphics.Color. TRANSPARENT)
                    layoutParams = LinearLayout.LayoutParams(50, 50)
                    setOnClickListener {
                        removeFloatingWindow(webApp.id)
                    }
                }
                addView(closeBtn)

                // Minimize button
                val minBtn = ImageButton(this@EnhancedFloatingWindowService).apply {
                    setImageResource(android.R.drawable.ic_menu_info_details)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    layoutParams = LinearLayout.LayoutParams(50, 50)
                    setOnClickListener {
                        minimizeWindow(webApp.id)
                    }
                }
                addView(minBtn)

                // Maximize button
                val maxBtn = ImageButton(this@EnhancedFloatingWindowService).apply {
                    setImageResource(android.R.drawable.ic_menu_view)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    layoutParams = LinearLayout.LayoutParams(50, 50)
                    setOnClickListener {
                        maximizeWindow(webApp.id)
                    }
                }
                addView(maxBtn)
            }
            addView(titleBar)

            // WebView
            val webView = WebView(this@EnhancedFloatingWindowService).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    topMargin = 50
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                webViewClient = WebViewClientWithDownload(this@EnhancedFloatingWindowService)
                addJavascriptInterface(WebAppJavaScriptInterface(this@EnhancedFloatingWindowService), "WebApp")

                loadUrl(webApp.url)
            }
            addView(webView)

            // Resize handle (bottom-right corner)
            val resizeHandle = View(this@EnhancedFloatingWindowService).apply {
                layoutParams = FrameLayout.LayoutParams(30, 30, Gravity.BOTTOM or Gravity.END)
                setBackgroundColor(ContextCompat.getColor(this@EnhancedFloatingWindowService, R.color.md_theme_primary))
                setOnTouchListener(createResizeTouchListener(layoutParams))
            }
            addView(resizeHandle)
        }
    }

    private fun createWindowTouchListener(layoutParams: WindowManager.LayoutParams): View.OnTouchListener {
        var lastX = 0f
        var lastY = 0f

        return View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - lastX
                    val deltaY = event.rawY - lastY

                    layoutParams.x += deltaX.toInt()
                    layoutParams.y += deltaY.toInt()

                    windowManager.updateViewLayout(v.parent as View, layoutParams)

                    lastX = event.rawX
                    lastY = event.rawY
                }
            }
            false
        }
    }

    private fun createResizeTouchListener(layoutParams: WindowManager.LayoutParams): View.OnTouchListener {
        var lastX = 0f
        var lastY = 0f

        return View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - lastX
                    val deltaY = event.rawY - lastY

                    layoutParams.width = max(MIN_WINDOW_WIDTH, layoutParams.width + deltaX.toInt())
                    layoutParams.height = max(MIN_WINDOW_HEIGHT, layoutParams.height + deltaY.toInt())

                    windowManager.updateViewLayout(v.parent as View, layoutParams)

                    lastX = event.rawX
                    lastY = event.rawY
                }
            }
            true
        }
    }

    private fun removeFloatingWindow(windowId: Long) {
        floatingWindows[windowId]?.let { window ->
            try {
                windowManager.removeView(window.view)
                floatingWindows.remove(windowId)
                updateWindowsList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun closeAllWindows() {
        floatingWindows.keys.toList().forEach { removeFloatingWindow(it) }
    }

    private fun minimizeWindow(windowId: Long) {
        floatingWindows[windowId]?.let { window ->
            val params = window.params
            if (window.isMinimized) {
                // Restore
                params.width = window.originalWidth
                params.height = window.originalHeight
                params.x = window.originalX
                params.y = window.originalY
                window.isMinimized = false
            } else {
                // Minimize to pill shape
                window.originalWidth = params.width
                window.originalHeight = params.height
                window.originalX = params.x
                window.originalY = params.y
                params.width = 200
                params.height = 50
                window.isMinimized = true
            }
            try {
                windowManager.updateViewLayout(window.view, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun maximizeWindow(windowId: Long) {
        floatingWindows[windowId]?.let { window ->
            val params = window.params
            val displayMetrics = android.util.DisplayMetrics()
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            display?.getRealMetrics(displayMetrics)

            params.x = 0
            params.y = 0
            params.width = displayMetrics.widthPixels
            params.height = displayMetrics.heightPixels

            try {
                windowManager.updateViewLayout(window.view, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun bringToFront(windowId: Long) {
        floatingWindows[windowId]?.let { window ->
            try {
                windowManager.updateViewLayout(window.view, window.params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateWindowsList() {
        _openWindows.value = floatingWindows.values.map { it.webApp }
    }

    companion object {
        const val ACTION_ADD_WINDOW = "com.cylonid.nativealpha.ADD_WINDOW"
        const val ACTION_REMOVE_WINDOW = "com.cylonid.nativealpha.REMOVE_WINDOW"
        const val ACTION_CLOSE_ALL = "com.cylonid.nativealpha.CLOSE_ALL"
        const val ACTION_MINIMIZE_WINDOW = "com.cylonid.nativealpha.MINIMIZE_WINDOW"
        const val ACTION_MAXIMIZE_WINDOW = "com.cylonid.nativealpha.MAXIMIZE_WINDOW"
        const val ACTION_BRING_TO_FRONT = "com.cylonid.nativealpha.BRING_TO_FRONT"

        private const val DEFAULT_WINDOW_WIDTH = 800
        private const val DEFAULT_WINDOW_HEIGHT = 600
        private const val MIN_WINDOW_WIDTH = 150
        private const val MIN_WINDOW_HEIGHT = 150
        private const val MAX_WINDOWS = 5
    }
}
