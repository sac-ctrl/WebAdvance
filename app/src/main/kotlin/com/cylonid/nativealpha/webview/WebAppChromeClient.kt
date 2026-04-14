package com.cylonid.nativealpha.webview

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.util.Log

/**
 * WebChromeClient for handling JavaScript dialogs, progress, and other browser features
 */
class WebAppChromeClient(
    private val onProgressChanged: (Int) -> Unit = {},
    private val onTitleReceived: (String) -> Unit = {},
    private val onErrorOccurred: (String, String) -> Unit = { _, _ -> }
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
        Log.d("WebApp", "Loading progress: $newProgress%")
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        title?.let { onTitleReceived(it) }
    }

    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        Log.d("WebApp", "JS Alert: $message from $url")
        // In a real app, show an AlertDialog
        result?.confirm()
        return true
    }

    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        Log.d("WebApp", "JS Confirm: $message from $url")
        // In a real app, show confirmation dialog
        result?.confirm()
        return true
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        Log.d("WebApp", "JS Prompt: $message from $url")
        // In a real app, show text input dialog
        result?.confirm(defaultValue)
        return true
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        Log.d("WebApp", "File chooser requested")
        // This would need to be handled by the Activity with file picker UI
        return true
    }

    /**
     * Handle console messages from page JavaScript
     */
    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
        consoleMessage?.let {
            Log.d("WebConsole", "[${it.lineNumber}] ${it.message}")
        }
        return true
    }
}
