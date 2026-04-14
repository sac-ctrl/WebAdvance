package com.cylonid.nativealpha.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.webkit.WebView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.model.WindowEntity
import com.cylonid.nativealpha.model.WindowPresetEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class FloatingWindowService : Service() {

    @Inject
    lateinit var windowManager: WindowManager

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
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
        }

        val view = LayoutInflater.from(this).inflate(R.layout.floating_window, null)
        val webView = view.findViewById<WebView>(R.id.floatingWebView)
        val titleText = view.findViewById<TextView>(R.id.titleText)
        titleText.text = webAppName

        // Configure WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // TODO: Load actual web app URL
        webView.loadUrl(webAppUrl)

        val closeButton = view.findViewById<android.widget.Button>(R.id.closeButton)
        val minimizeButton = view.findViewById<android.widget.Button>(R.id.minimizeButton)
        val maximizeButton = view.findViewById<android.widget.Button>(R.id.maximizeButton)

        closeButton?.setOnClickListener {
            removeFloatingWindow(webAppId)
        }

        minimizeButton?.setOnClickListener {
            minimizeWindow(webAppId)
        }

        maximizeButton?.setOnClickListener {
            maximizeWindow(webAppId)
        }

        val resizeHandle = view.findViewById<View>(R.id.resizeHandle)

        // Add resize functionality
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
        floatingWindows[webAppId] = FloatingWindowView(
            view = view,
            layoutParams = layoutParams,
            webView = webView,
            isMinimized = false,
            isMaximized = false,
            originalWidth = 800,
            originalHeight = 600,
            originalX = 100,
            originalY = 100
        )

        updateOpenWindows()
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

    fun getCurrentWindows(): List<WindowEntity> {
        return floatingWindows.map { (id, windowView) ->
            WindowEntity(
                id = id,
                appName = "Web App $id", // TODO: Get actual app name
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

    private fun updateOpenWindows() {
        _openWindows.value = getCurrentWindows()
    }

    companion object {
        const val ACTION_ADD_WINDOW = "ADD_WINDOW"
        const val ACTION_REMOVE_WINDOW = "REMOVE_WINDOW"
        const val ACTION_CLOSE_ALL = "CLOSE_ALL"
        const val ACTION_MINIMIZE_WINDOW = "MINIMIZE_WINDOW"
        const val ACTION_MAXIMIZE_WINDOW = "MAXIMIZE_WINDOW"
    }
}

data class FloatingWindowView(
    val view: View,
    val layoutParams: WindowManager.LayoutParams,
    val webView: WebView,
    var isMinimized: Boolean = false,
    var isMaximized: Boolean = false,
    var originalWidth: Int = 800,
    var originalHeight: Int = 600,
    var originalX: Int = 100,
    var originalY: Int = 100
)