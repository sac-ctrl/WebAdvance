# Link Management Integration Guide

## Overview

This guide explains how to integrate the Link Management System into WebAdvance activities and WebView instances.

## Step 1: Add Dependencies

Update `app/build.gradle`:

```gradle
dependencies {
    // Room database
    implementation 'androidx.room:room-runtime:2.5.2'
    kapt 'androidx.room:room-compiler:2.5.2'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'
    
    // GSON for JSON export
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Material Design
    implementation 'com.google.android.material:material:1.9.0'
}
```

## Step 2: Initialize in WebApp Activity

```kotlin
import com.cylonid.nativealpha.links.*

class WebAppActivity : AppCompatActivity() {
    private lateinit var linkSystem: LinkManagementSystem
    private lateinit var historyTracker: LinkHistoryTracker
    private lateinit var suggestionEngine: LinkSuggestionEngine
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appId = getIntent().getLongExtra("APP_ID", 0L)
        
        // Initialize link management components
        linkSystem = LinkManagementSystem(this, appId)
        historyTracker = LinkHistoryTracker(this, appId)
        suggestionEngine = LinkSuggestionEngine(historyTracker, linkSystem)
        
        // Setup WebView link handling
        setupLinkHandling()
        
        // Observe suggestions
        observeLinkSuggestions()
    }
    
    private fun setupLinkHandling() {
        // Enable long-press context menu
        linkSystem.setupLinkContextMenu(webView)
        
        // Add copy button to toolbar
        addCopyButton()
    }
    
    private fun addCopyButton() {
        val copyButton = ImageView(this).apply {
            setImageResource(R.drawable.ic_baseline_content_copy_24)
            setOnClickListener { showLinkFormatPicker() }
        }
        toolbar.addView(copyButton)
    }
}
```

## Step 3: Implement Link Copying

```kotlin
private fun showLinkFormatPicker() {
    linkSystem.getCurrentPageInfo(webView) { url, title ->
        val dialog = LinkPickerBottomSheet(linkSystem) { format ->
            val formatted = linkSystem.copyUrl(url, title, format)
            
            lifecycleScope.launch {
                historyTracker.recordAction(
                    url = url,
                    pageTitle = title,
                    action = "copy",
                    format = format.name
                )
                
                showToast("Copied to clipboard")
            }
        }
        dialog.show(supportFragmentManager, "link_picker")
    }
}

private fun showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
```

## Step 4: Add Save Link Feature

```kotlin
private fun showSaveDialog(url: String, title: String) {
    val categories = listOf("Unsorted", "Reading", "Reference", "Shopping", "Custom")
    
    AlertDialog.Builder(this)
        .setTitle("Save Link")
        .setItems(categories.toTypedArray()) { _, which ->
            val category = categories[which]
            
            if (category == "Custom") {
                showCustomCategoryDialog(url, title)
            } else {
                linkSystem.saveLink(url, title, category)
                showToast("Link saved to $category")
            }
        }
        .show()
}

private fun showCustomCategoryDialog(url: String, title: String) {
    val input = EditText(this)
    
    AlertDialog.Builder(this)
        .setTitle("Category Name")
        .setView(input)
        .setPositiveButton("Save") { _, _ ->
            linkSystem.saveLink(url, title, input.text.toString())
            showToast("Link saved")
        }
        .show()
}
```

## Step 5: Add Share Functionality

```kotlin
private fun shareCurrentPage() {
    linkSystem.getCurrentPageInfo(webView) { url, title ->
        val suggestions = listOf(
            "Share Link Only",
            "Share Link & Title",
            "Share Markdown",
            "Share HTML"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Share as")
            .setItems(suggestions.toTypedArray()) { _, which ->
                val format = when (which) {
                    0 -> LinkManagementSystem.LinkFormat.PLAIN_URL
                    1 -> LinkManagementSystem.LinkFormat.URL_WITH_TITLE
                    2 -> LinkManagementSystem.LinkFormat.MARKDOWN
                    3 -> LinkManagementSystem.LinkFormat.HTML_ANCHOR
                    else -> LinkManagementSystem.LinkFormat.PLAIN_URL
                }
                
                linkSystem.copyUrl(url, title, format)
                linkSystem.shareLink(url, title)
                
                lifecycleScope.launch {
                    historyTracker.recordAction(
                        url = url,
                        pageTitle = title,
                        action = "share",
                        format = format.name
                    )
                }
            }
            .show()
    }
}
```

## Step 6: Display Usage Suggestions

```kotlin
private fun observeLinkSuggestions() {
    lifecycleScope.launch {
        flow {
            val suggestions = suggestionEngine.generateSuggestions()
            emit(suggestions)
        }.collect { suggestions ->
            updateSuggestionUI(suggestions)
        }
    }
}

private fun updateSuggestionUI(suggestions: List<LinkSuggestion>) {
    if (suggestions.isEmpty()) return
    
    // Show suggestion chip or carousel
    suggestionChip.apply {
        text = suggestions.first().title
        setOnClickListener {
            linkSystem.copyUrl(suggestions.first().url)
            showToast("Link copied")
        }
    }
}
```

## Step 7: Add Link History View

```kotlin
private fun showLinkHistory() {
    lifecycleScope.launch {
        val history = historyTracker.getRecentPageTitles(20)
        
        val adapter = ArrayAdapter(
            this@WebAppActivity,
            android.R.layout.simple_list_item_1,
            history
        )
        
        AlertDialog.Builder(this@WebAppActivity)
            .setTitle("Recent Pages")
            .setAdapter(adapter) { _, which ->
                // Use selected history item
                val title = history[which]
                linkSystem.getCurrentPageInfo(webView) { url, _ ->
                    linkSystem.copyUrl(url, title)
                }
            }
            .show()
    }
}
```

## Step 8: Add Analytics Export

```kotlin
private fun showAnalytics() {
    lifecycleScope.launch {
        val report = historyTracker.generateReport()
        
        val message = """
            Total Link Actions: ${report.totalActions}
            
            Actions by Type:
            ${report.actionBreakdown.entries.joinToString("\n") { 
                "${it.key}: ${it.value}" 
            }}
            
            Most Used Links:
            ${report.mostFrequentLinks.take(3).joinToString("\n") { 
                "• ${it.url} (${it.count}x)" 
            }}
        """.trimIndent()
        
        AlertDialog.Builder(this@WebAppActivity)
            .setTitle("Link Statistics")
            .setMessage(message)
            .setPositiveButton("Export JSON") { _, _ ->
                exportAnalyticsJson()
            }
            .setNegativeButton("Close", null)
            .show()
    }
}

private fun exportAnalyticsJson() {
    val json = linkSystem.exportLinksAsJson()
    
    // Save to file or share
    val file = File(getExternalFilesDir(null), "links_export.json")
    file.writeText(json)
    
    showToast("Exported to ${file.absolutePath}")
}
```

## Integration with Existing Features

### Copy Button in Context Menu

```kotlin
override fun onCreateContextMenu(
    menu: ContextMenu,
    v: View,
    menuInfo: ContextMenu.ContextMenuInfo?
) {
    super.onCreateContextMenu(menu, v, menuInfo)
    
    menu.add(0, COPY_LINK_ID, 0, "Copy Link").apply {
        setOnMenuItemClickListener { handleCopyLink() }
    }
    
    menu.add(0, SAVE_LINK_ID, 0, "Save Link").apply {
        setOnMenuItemClickListener { handleSaveLink() }
    }
}

private fun handleCopyLink(): Boolean {
    // Get link from WebView hit test
    showLinkFormatPicker()
    return true
}
```

### Integration with Credential Vault

```kotlin
private fun saveLinkWithCredentials(
    url: String,
    title: String,
    username: String? = null,
    password: String? = null
) {
    linkSystem.saveLink(url, title, category = "Credentials")
    
    // Also save in credential vault if provided
    if (username != null && password != null) {
        credentialVault.saveCredential(
            website = url,
            username = username,
            password = password
        )
    }
}
```

### Integration with Clipboard Manager

```kotlin
private fun syncWithClipboardManager() {
    lifecycleScope.launch {
        historyTracker.getHistoryFlow().collect { history ->
            // Sync recent links with clipboard manager
            clipboardManager.updateRecentLinks(
                history.take(10).map { it.url }
            )
        }
    }
}
```

## Testing

### Unit Test Example

```kotlin
class LinkManagementSystemTest {
    @Test
    fun testCopyUrl_plainFormat() {
        val linkSystem = LinkManagementSystem(context, 1L)
        val formatted = linkSystem.copyUrl(
            "https://example.com",
            "Example",
            LinkManagementSystem.LinkFormat.PLAIN_URL
        )
        assertEquals("https://example.com", formatted)
    }
    
    @Test
    fun testCopyUrl_markdownFormat() {
        val linkSystem = LinkManagementSystem(context, 1L)
        val formatted = linkSystem.copyUrl(
            "https://example.com",
            "Example",
            LinkManagementSystem.LinkFormat.MARKDOWN
        )
        assertEquals("[Example](https://example.com)", formatted)
    }
}
```

## Performance Tips

1. **Lazy Load Suggestions**: Don't generate all suggestions on app start
2. **Batch History Operations**: Group history updates when possible
3. **Clean Old History**: Periodically clean history older than 30 days
4. **Use Pagination**: Load history in pages for large datasets
5. **Cache Frequent URLs**: Cache frequently accessed links in memory

## Troubleshooting

### Links Not Being Saved
- Check that `appId` is correctly passed
- Verify Room database is initialized
- Check permissions for file access

### Suggestions Not Showing
- Ensure history is being recorded
- Check that `historyTracker` is initialized
- Verify flow subscriptions are active

### Copy Format Not Working
- Check if `CLIPBOARD_SERVICE` permission is granted
- Verify format enum is supported
- Test with different formats

## Next Steps

1. Customize link categories for your user base
2. Add keyboard shortcuts for quick copying
3. Implement cloud sync for link history
4. Add link preview generation
5. Create link management dashboard

