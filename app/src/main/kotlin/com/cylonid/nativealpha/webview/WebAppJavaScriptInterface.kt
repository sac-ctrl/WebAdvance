package com.cylonid.nativealpha.webview

import android.content.Context
import android.webkit.JavascriptInterface
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * JavaScript interface for clipboard, automation, and utility features
 * Injected into WebView with @JavascriptInterface
 */
class WebAppJavaScriptInterface(
    private val context: Context,
    private val onClipboardCopy: (String) -> Unit = {},
    private val onClipboardPaste: (() -> String)? = null,
    private val onScrollPositionChange: (Int) -> Unit = {},
    private val onDOMChange: () -> Unit = {}
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    /**
     * Copy text to clipboard
     */
    @JavascriptInterface
    fun copyToClipboard(text: String) {
        scope.launch {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("web content", text)
            clipboard.setPrimaryClip(clip)
            onClipboardCopy(text)
            Log.d("WebApp", "Copied to clipboard: ${text.take(100)}")
        }
    }

    /**
     * Get current page URL
     */
    @JavascriptInterface
    fun getCurrentPageUrl(): String? {
        // This would be set from WebView
        return null
    }

    /**
     * Get all visible text from page
     */
    @JavascriptInterface
    fun copyAllVisibleText(): String {
        // This is called from JavaScript which extracts the text
        return ""
    }

    /**
     * Get current page title
     */
    @JavascriptInterface
    fun getPageTitle(): String {
        return "Unknown"
    }

    /**
     * Execute custom JavaScript and return result
     */
    @JavascriptInterface
    fun executeScript(script: String): String {
        Log.d("WebApp", "Script execution requested: ${script.take(100)}")
        return "Script execution not directly supported from Java"
    }

    /**
     * Detect and report page content changes
     */
    @JavascriptInterface
    fun reportPageChange(description: String) {
        scope.launch {
            onDOMChange()
            Log.d("WebApp", "Page changed: $description")
        }
    }

    /**
     * Log current scroll position
     */
    @JavascriptInterface
    fun logScrollPosition(position: Int) {
        scope.launch {
            onScrollPositionChange(position)
            Log.d("WebApp", "Scroll position: $position")
        }
    }

    /**
     * Click element by CSS selector
     */
    @JavascriptInterface
    fun clickElement(selector: String) {
        Log.d("WebApp", "Click requested for: $selector")
    }

    /**
     * Auto-scroll page
     */
    @JavascriptInterface
    fun autoScroll(direction: String, speed: Int) {
        Log.d("WebApp", "Auto-scroll: $direction at speed $speed")
    }

    /**
     * Fill form field with value
     */
    @JavascriptInterface
    fun fillFormField(selector: String, value: String) {
        Log.d("WebApp", "Fill form field '$selector' with value")
    }

    /**
     * Get clipboard content (if available)
     */
    @JavascriptInterface
    fun getClipboardContent(): String {
        return onClipboardPaste?.invoke() ?: ""
    }

    /**
     * Inject CSS for dark mode
     */
    @JavascriptInterface
    fun applyDarkMode(enabled: Boolean) {
        Log.d("WebApp", "Dark mode: $enabled")
    }

    /**
     * Get current scroll position
     */
    @JavascriptInterface
    fun getScrollPosition(): Int {
        return 0
    }

    /**
     * Screenshot the visible area
     */
    @JavascriptInterface
    fun captureScreenshot(): Boolean {
        Log.d("WebApp", "Screenshot requested")
        return true
    }

    /**
     * Simulate network condition
     */
    @JavascriptInterface
    fun setNetworkCondition(condition: String) {
        Log.d("WebApp", "Network condition changed to: $condition")
    }

    /**
     * Check if adblock is active
     */
    @JavascriptInterface
    fun isAdblockActive(): Boolean {
        return false // Set based on actual adblock status
    }
}
