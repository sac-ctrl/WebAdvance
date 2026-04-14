package com.cylonid.nativealpha.links

import android.content.Context
import android.content.Intent
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Link management system for copying URLs in various formats
 */
class LinkManagementSystem(
    private val context: Context,
    private val appId: Long
) {
    private val _savedLinks = MutableStateFlow<List<SavedLink>>(emptyList())
    val savedLinks: StateFlow<List<SavedLink>> = _savedLinks.asStateFlow()

    private val _defaultFormat = MutableStateFlow<LinkFormat>(LinkFormat.PLAIN_URL)
    val defaultFormat: StateFlow<LinkFormat> = _defaultFormat.asStateFlow()

    /**
     * Copy URL in specified format
     */
    fun copyUrl(
        url: String,
        pageTitle: String? = null,
        format: LinkFormat = _defaultFormat.value
    ): String {
        val formattedLink = when (format) {
            LinkFormat.PLAIN_URL -> url
            
            LinkFormat.URL_WITH_TITLE -> {
                val title = pageTitle ?: "Web Page"
                "$title: $url"
            }
            
            LinkFormat.MARKDOWN -> {
                val title = pageTitle ?: "Link"
                "[$title]($url)"
            }
            
            LinkFormat.HTML_ANCHOR -> {
                val title = pageTitle ?: "Link"
                """<a href="$url">$title</a>"""
            }
            
            LinkFormat.CUSTOM -> url
        }
        
        copyToClipboard(formattedLink)
        return formattedLink
    }

    /**
     * Setup long-press on links in WebView
     */
    fun setupLinkContextMenu(webView: WebView) {
        val script = """
            (function() {
                document.addEventListener('contextmenu', function(e) {
                    if (e.target.tagName === 'A') {
                        e.preventDefault();
                        const url = e.target.href;
                        const title = e.target.innerText || e.target.title;
                        
                        WebApp.onLinkLongPress(url, title);
                        return false;
                    }
                });
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(script) { }
    }

    /**
     * Get current page URL and title
     */
    fun getCurrentPageInfo(webView: WebView, callback: (String, String) -> Unit) {
        webView.evaluateJavascript(
            "(function() { return JSON.stringify({url: window.location.href, title: document.title}); })()"
        ) { result ->
            try {
                val json = com.google.gson.JsonParser.parseString(result).asJsonObject
                val url = json.get("url").asString
                val title = json.get("title").asString
                callback(url, title)
            } catch (e: Exception) {
                e.printStackTrace()
                callback("", "")
            }
        }
    }

    /**
     * Save link for later use
     */
    fun saveLink(url: String, pageTitle: String, category: String = "Unsorted") {
        val link = SavedLink(
            id = System.currentTimeMillis(),
            appId = appId,
            url = url,
            pageTitle = pageTitle,
            category = category,
            format = _defaultFormat.value,
            savedAt = System.currentTimeMillis(),
            accessCount = 0
        )
        
        val links = _savedLinks.value.toMutableList()
        links.add(0, link)
        _savedLinks.value = links
    }

    /**
     * Share link to other apps
     */
    fun shareLink(url: String, title: String? = null) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            title?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            type = "text/plain"
        }
        
        val shareIntent = Intent.createChooser(sendIntent, "Share Link")
        context.startActivity(shareIntent)
    }

    /**
     * Get all links in category
     */
    fun getLinksByCategory(category: String): List<SavedLink> {
        return _savedLinks.value.filter { it.category == category }
    }

    /**
     * Search saved links
     */
    fun searchLinks(query: String): List<SavedLink> {
        return _savedLinks.value.filter { link ->
            link.url.contains(query, ignoreCase = true) ||
            link.pageTitle.contains(query, ignoreCase = true)
        }
    }

    /**
     * Delete saved link
     */
    fun deleteLink(linkId: Long) {
        val links = _savedLinks.value.toMutableList()
        links.removeAll { it.id == linkId }
        _savedLinks.value = links
    }

    /**
     * Set default copy format
     */
    fun setDefaultFormat(format: LinkFormat) {
        _defaultFormat.value = format
    }

    /**
     * Get link statistics
     */
    fun getStatistics(): LinkStatistics {
        val links = _savedLinks.value
        return LinkStatistics(
            totalLinks = links.size,
            categories = links.map { it.category }.distinct().size,
            mostUsedFormat = links.groupingBy { it.format }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: LinkFormat.PLAIN_URL,
            oldestLink = links.minByOrNull { it.savedAt }?.savedAt ?: 0L,
            newestLink = links.maxByOrNull { it.savedAt }?.savedAt ?: 0L
        )
    }

    /**
     * Export links as JSON
     */
    fun exportLinksAsJson(): String {
        val gson = com.google.gson.Gson()
        return gson.toJson(mapOf(
            "appId" to appId,
            "exportDate" to System.currentTimeMillis(),
            "links" to _savedLinks.value
        ))
    }

    /**
     * Export links as HTML
     */
    fun exportLinksAsHtml(): String {
        val sb = StringBuilder()
        sb.append("<html><head><title>Saved Links</title></head><body>")
        sb.append("<h1>Saved Links from Web App</h1>")
        
        val categories = _savedLinks.value.groupBy { it.category }
        categories.forEach { (category, links) ->
            sb.append("<h2>$category</h2>")
            sb.append("<ul>")
            links.forEach { link ->
                sb.append("<li><a href=\"${link.url}\">${link.pageTitle}</a></li>")
            }
            sb.append("</ul>")
        }
        
        sb.append("</body></html>")
        return sb.toString()
    }

    /**
     * Clear all links
     */
    fun clearAllLinks() {
        _savedLinks.value = emptyList()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("link", text)
        clipboard.setPrimaryClip(clip)
    }

    enum class LinkFormat {
        PLAIN_URL,
        URL_WITH_TITLE,
        MARKDOWN,
        HTML_ANCHOR,
        CUSTOM
    }

    data class SavedLink(
        val id: Long,
        val appId: Long,
        val url: String,
        val pageTitle: String,
        val category: String,
        val format: LinkFormat,
        val savedAt: Long,
        val accessCount: Int
    )

    data class LinkStatistics(
        val totalLinks: Int,
        val categories: Int,
        val mostUsedFormat: LinkFormat,
        val oldestLink: Long,
        val newestLink: Long
    )
}
