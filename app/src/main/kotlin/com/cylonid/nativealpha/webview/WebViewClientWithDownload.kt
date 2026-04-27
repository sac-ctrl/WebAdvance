package com.cylonid.nativealpha.webview

import android.content.Context
import android.graphics.Bitmap
import android.webkit.*
import android.webkit.DownloadListener
import android.webkit.WebViewClient

class WebViewClientWithDownload(
    private val context: Context,
    private val onPageStarted: (String) -> Unit = {},
    private val onPageFinished: (String) -> Unit = {},
    private val onDownloadStart: (String, String) -> Unit = { _, _ -> }
) : WebViewClient(), DownloadListener {

    var adblockEnabled: Boolean = false

    private val adHosts = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "ads.youtube.com", "adservice.google.com", "adservice.google.co",
        "pagead2.googlesyndication.com", "tpc.googlesyndication.com",
        "amazon-adsystem.com", "moatads.com", "advertising.com",
        "scorecardresearch.com", "quantserve.com", "outbrain.com",
        "taboola.com", "criteo.com", "cdn.viglink.com", "viglink.com",
        "ads.twitter.com", "ads.linkedin.com", "adsystem.amazon.com",
        "pixel.facebook.com", "connect.facebook.net", "stats.g.doubleclick.net"
    )
    
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
        // Don't enqueue here — defer to the host (DownloadManager) so we get
        // unified Chrome-like handling with cookies, referer, per-app folders,
        // and a single source of truth in the DB.
        onDownloadStart(url, contentDisposition.ifBlank { mimetype })
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        if (adblockEnabled) {
            val host = request?.url?.host?.lowercase() ?: ""
            val blocked = adHosts.any { ad -> host == ad || host.endsWith(".$ad") }
            if (blocked) {
                return WebResourceResponse("text/plain", "utf-8", null)
            }
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

}
