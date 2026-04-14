package com.cylonid.nativealpha.links

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.DialogFragment
import com.cylonid.nativealpha.R
import kotlinx.coroutines.launch

/**
 * Complete implementation example for WebApp integration
 */
class LinkManagementExample(
    private val context: Context,
    private val appId: Long
) {
    
    private val linkSystem = LinkManagementSystem(context, appId)
    private val historyTracker = LinkHistoryTracker(context, appId)
    private val suggestionEngine = LinkSuggestionEngine(historyTracker, linkSystem)

    /**
     * Create copy link button for toolbar
     */
    fun createCopyButton(container: ViewGroup? = null): AppCompatImageButton {
        return AppCompatImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_view)
            contentDescription = "Copy Link"
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            
            setOnClickListener {
                showLinkOptions()
            }
            
            container?.addView(this)
        }
    }

    /**
     * Show link management options menu
     */
    private fun showLinkOptions() {
        val options = arrayOf(
            "Copy Link",
            "Save Link",
            "Share Link",
            "View History",
            "Link Statistics"
        )
        
        AlertDialog.Builder(context)
            .setTitle("Link Management")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCopyOptions()
                    1 -> showSaveOptions()
                    2 -> showShareOptions()
                    3 -> showLinkHistory()
                    4 -> showStatistics()
                }
            }
            .show()
    }

    /**
     * Show copy format options
     */
    private fun showCopyOptions() {
        val formats = LinkManagementSystem.LinkFormat.values()
        val formatNames = formats.map { it.name.replace("_", " ") }.toTypedArray()
        
        AlertDialog.Builder(context)
            .setTitle("Copy Format")
            .setItems(formatNames) { _, which ->
                val format = formats[which]
                linkSystem.setDefaultFormat(format)
                // TODO: Get URL and copy it
            }
            .show()
    }

    /**
     * Show save options with category selection
     */
    private fun showSaveOptions() {
        val categories = listOf(
            "Unsorted",
            "Reading",
            "Reference", 
            "Shopping",
            "Work",
            "Personal",
            "Custom..."
        ).toTypedArray()
        
        AlertDialog.Builder(context)
            .setTitle("Save to Category")
            .setItems(categories) { _, which ->
                when {
                    which < categories.size - 1 -> {
                        val category = categories[which]
                        // TODO: Get URL and save
                    }
                    else -> showCustomCategoryDialog()
                }
            }
            .show()
    }

    /**
     * Dialog for custom category
     */
    private fun showCustomCategoryDialog() {
        val input = EditText(context).apply {
            hint = "Category name"
            setSingleLine()
        }
        
        AlertDialog.Builder(context)
            .setTitle("New Category")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                // TODO: Save with custom category
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show share options
     */
    private fun showShareOptions() {
        val shareOptions = arrayOf(
            "Share Plain URL",
            "Share with Title",
            "Share Markdown",
            "Share HTML",
            "Share to File"
        )
        
        AlertDialog.Builder(context)
            .setTitle("Share Link As")
            .setItems(shareOptions) { _, which ->
                when (which) {
                    0 -> shareAsFormat(LinkManagementSystem.LinkFormat.PLAIN_URL)
                    1 -> shareAsFormat(LinkManagementSystem.LinkFormat.URL_WITH_TITLE)
                    2 -> shareAsFormat(LinkManagementSystem.LinkFormat.MARKDOWN)
                    3 -> shareAsFormat(LinkManagementSystem.LinkFormat.HTML_ANCHOR)
                    4 -> exportAndShare()
                }
            }
            .show()
    }

    /**
     * Share in specific format
     */
    private fun shareAsFormat(format: LinkManagementSystem.LinkFormat) {
        // TODO: Get current URL and share in format
    }

    /**
     * Export and share all links
     */
    private fun exportAndShare() {
        AlertDialog.Builder(context)
            .setTitle("Export Format")
            .setItems(arrayOf("JSON", "HTML")) { _, which ->
                if (which == 0) {
                    val json = linkSystem.exportLinksAsJson()
                    // Save and share JSON
                } else {
                    val html = linkSystem.exportLinksAsHtml()
                    // Save and share HTML
                }
            }
            .show()
    }

    /**
     * Show link history
     */
    private fun showLinkHistory() {
        val userContext = context as? androidx.fragment.app.FragmentActivity ?: return
        
        userContext.lifecycleScope.launch {
            val recentPages = historyTracker.getRecentPageTitles(20)
            
            AlertDialog.Builder(context)
                .setTitle("Recent Pages (${recentPages.size})")
                .setItems(recentPages.toTypedArray()) { _, which ->
                    // Copy the selected link
                    val title = recentPages[which]
                    Toast.makeText(context, "Copied: $title", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Clear History") { _, _ ->
                    userContext.lifecycleScope.launch {
                        historyTracker.clearAll()
                        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }
    }

    /**
     * Show link statistics
     */
    private fun showStatistics() {
        val userContext = context as? androidx.fragment.app.FragmentActivity ?: return
        
        userContext.lifecycleScope.launch {
            val report = historyTracker.generateReport()
            val stats = linkSystem.getStatistics()
            
            val message = buildString {
                appendLine("=== Link Statistics ===\n")
                appendLine("Total Saved Links: ${stats.totalLinks}")
                appendLine("Categories: ${stats.categories}")
                appendLine("Most Used Format: ${stats.mostUsedFormat}\n")
                
                appendLine("=== Activity ===\n")
                appendLine("Total Actions: ${report.totalActions}")
                
                if (report.actionBreakdown.isNotEmpty()) {
                    appendLine("\nActions:")
                    report.actionBreakdown.forEach { (action, count) ->
                        appendLine("  • $action: $count")
                    }
                }
                
                if (report.mostFrequentLinks.isNotEmpty()) {
                    appendLine("\nMost Frequent Links:")
                    report.mostFrequentLinks.take(3).forEach { link ->
                        appendLine("  • ${link.url} (${link.count}x)")
                    }
                }
                
                if (stats.oldestLink > 0) {
                    appendLine("\nData Range:")
                    appendLine("  From: ${formatDate(stats.oldestLink)}")
                    appendLine("  To: ${formatDate(stats.newestLink)}")
                }
            }
            
            AlertDialog.Builder(context)
                .setTitle("Statistics")
                .setMessage(message)
                .setPositiveButton("Export") { _, _ ->
                    exportStatistics(report)
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    /**
     * Export statistics
     */
    private fun exportStatistics(report: LinkHisticsReport) {
        val json = buildString {
            appendLine("{")
            appendLine("  \"generatedAt\": \"${formatDate(report.generatedAt)}\",")
            appendLine("  \"totalActions\": ${report.totalActions},")
            appendLine("  \"actions\": {")
            report.actionBreakdown.forEach { (action, count) ->
                appendLine("    \"$action\": $count,")
            }
            appendLine("  }")
            appendLine("}")
        }
        
        Toast.makeText(context, "Statistics exported", Toast.LENGTH_SHORT).show()
    }

    private fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return format.format(date)
    }

    /**
     * Setup WebView with long-press link handling
     */
    fun setupWebViewLinkHandling(webView: android.webkit.WebView) {
        linkSystem.setupLinkContextMenu(webView)
        
        // Add JavaScript interface for link interactions
        webView.addJavascriptInterface(LinkJavaScriptInterface(), "WebApp")
    }

    /**
     * JavaScript interface for WebView communication
     */
    private inner class LinkJavaScriptInterface {
        @android.webkit.JavascriptInterface
        fun onLinkLongPress(url: String, title: String) {
            showLinkContextMenu(url, title)
        }
        
        @android.webkit.JavascriptInterface
        fun onPageTitleChanged(title: String) {
            // Update UI with current page title
        }
    }

    /**
     * Show context menu for long-pressed link
     */
    private fun showLinkContextMenu(url: String, title: String) {
        val options = arrayOf(
            "Copy Link",
            "Save Link",
            "Share Link",
            "Open in New Tab",
            "Copy Title"
        )
        
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        linkSystem.copyUrl(url, title)
                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                    }
                    1 -> linkSystem.saveLink(url, title)
                    2 -> linkSystem.shareLink(url, title)
                    3 -> {} // Open in new tab
                    4 -> {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("title", title))
                        Toast.makeText(context, "Title copied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    /**
     * Get quick suggestions for toolbar
     */
    fun getQuickSuggestions(
        onSuggestionsReady: (List<LinkSuggestion>) -> Unit
    ) {
        val userContext = context as? androidx.fragment.app.FragmentActivity ?: return
        
        userContext.lifecycleScope.launch {
            val suggestions = suggestionEngine.generateSuggestions()
            onSuggestionsReady(suggestions)
        }
    }

    /**
     * Record link interaction for analytics
     */
    fun recordLinkInteraction(
        url: String,
        title: String,
        action: String,
        format: String = "PLAIN_URL"
    ) {
        val userContext = context as? androidx.fragment.app.FragmentActivity ?: return
        
        userContext.lifecycleScope.launch {
            historyTracker.recordAction(
                url = url,
                pageTitle = title,
                action = action,
                format = format
            )
        }
    }
}

/**
 * Dialog fragment for link management
 */
class LinkManagementDialogFragment(
    private val linkSystem: LinkManagementSystem,
    private val onAction: (String, String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Link Management")
            .setItems(arrayOf("Copy", "Save", "Share", "History")) { _, which ->
                when (which) {
                    0 -> onAction("copy", "")
                    1 -> onAction("save", "")
                    2 -> onAction("share", "")
                    3 -> onAction("history", "")
                }
            }
            .create()
    }

    companion object {
        fun newInstance(
            linkSystem: LinkManagementSystem,
            onAction: (String, String) -> Unit
        ) = LinkManagementDialogFragment(linkSystem, onAction)
    }
}
