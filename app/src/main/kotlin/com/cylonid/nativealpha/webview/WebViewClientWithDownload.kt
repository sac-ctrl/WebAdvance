package com.cylonid.nativealpha.webview

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.webkit.*
import android.webkit.WebViewClient
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class WebViewClientWithDownload(
    private val context: Context,
    private val onPageStarted: (String) -> Unit = {},
    private val onPageFinished: (String) -> Unit = {},
    private val onDownloadStart: (String, String) -> Unit = { _, _ -> }
) : WebViewClient() {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { onPageStarted(it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { onPageFinished(it) }
    }

    override fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimetype: String,
        contentLength: Long
    ) {
        try {
            val filename = parseFilename(contentDisposition, url)
            val downloadDir = getDownloadDirectory(context)
            
            // Create download request
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimetype)
                addRequestHeader("User-Agent", userAgent)
                setTitle(filename)
                setDescription("Downloading from $url")
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    filename
                )
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            }

            // Start download
            downloadManager.enqueue(request)
            onDownloadStart(filename, url)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        // Log intercepted requests for debugging
        request?.url?.let {
            // Could add custom handling here for specific file types
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        error?.let {
            // Log error for debugging
            android.util.Log.e("WebViewError", "Error ${it.errorCode}: ${it.description}")
        }
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: android.net.http.SslError?
    ) {
        // In production, never call proceed() on SSL errors
        // For development, we might allow it with user confirmation
        handler?.cancel()
    }

    private fun parseFilename(contentDisposition: String, url: String): String {
        // Try to extract filename from Content-Disposition header
        val regex = """filename\*?=['"]?([^'"\n]*?)['"]?(?:;|$)""".toRegex()
        val match = regex.find(contentDisposition)
        
        return if (match != null) {
            match.groupValues[1].split("/").last()
        } else {
            // Fallback to URL filename
            url.split("/").last().split("?").first().let {
                if (it.isNotEmpty()) it else {
                    "download_${System.currentTimeMillis()}"
                }
            }
        }
    }

    private fun getDownloadDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "WAOS")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
