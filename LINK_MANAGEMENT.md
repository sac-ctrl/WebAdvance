# Link Management System

## Overview

The Link Management System provides comprehensive link handling, organization, and sharing capabilities for web app content. It allows users to efficiently copy, save, and share URLs in multiple formats with intelligent suggestions and analytics.

## Key Features

### 1. Multi-Format Link Copying
- **Plain URL**: Direct URL copying
- **URL with Title**: Includes the page title along with the URL
- **Markdown**: Formats link as `[title](url)`
- **HTML Anchor**: Creates `<a href="url">title</a>`
- **Custom**: User-defined formats

### 2. Link Management
- **Save Links**: Store URLs with metadata and categories
- **Categorization**: Organize saved links by category
- **Search**: Full-text search across saved links
- **Quick Access**: Recently used or frequently accessed links

### 3. Sharing Capabilities
- **Direct Share**: Share links to other apps via Android Intent
- **Export Formats**: Export all links as JSON or HTML
- **Batch Operations**: Handle multiple links efficiently

### 4. History & Analytics
- **Link History**: Track all link interactions with timestamps
- **Usage Statistics**: Monitor copy/share/access patterns
- **Link Frequency**: Identify most frequently used links
- **Activity Reports**: Generate comprehensive usage reports

### 5. Smart Suggestions
- **Frequency-based**: Suggest frequently used links
- **Context-aware**: Suggest links from similar domains
- **Format Learning**: Learn and recommend preferred formats
- **Time-based**: Suggest links based on access patterns

## Architecture

### Core Components

#### LinkManagementSystem
Main system for link operations:
- Copy links in various formats
- Manage saved links
- Share to external apps
- Export and import links

```kotlin
val linkSystem = LinkManagementSystem(context, appId)

// Copy URL in Markdown format
val formatted = linkSystem.copyUrl(
    url = "https://example.com",
    pageTitle = "Example Page",
    format = LinkFormat.MARKDOWN
)

// Save for later
linkSystem.saveLink(
    url = "https://example.com",
    pageTitle = "Example Page",
    category = "References"
)

// Share to other apps
linkSystem.shareLink(url, "Example Page")
```

#### LinkHistoryTracker
Tracks and analyzes link interactions:
- Record copy, share, and access events
- Generate usage statistics
- Query most used links
- Cleanup old history entries

```kotlin
val tracker = LinkHistoryTracker(context, appId)

// Record an action
tracker.recordAction(
    url = "https://example.com",
    pageTitle = "Example",
    action = "copy",
    format = "MARKDOWN"
)

// Get statistics
val report = tracker.generateReport()
```

#### LinkSuggestionEngine
Provides intelligent recommendations:
- Frequency-based suggestions
- Context-aware recommendations
- Format recommendations
- Learning from user behavior

```kotlin
val engine = LinkSuggestionEngine(tracker, linkSystem)

// Get suggestions
val suggestions = engine.generateSuggestions()

// Get format recommendation
val recommendation = engine.getFormatRecommendations(url)
```

### Data Models

#### SavedLink
```kotlin
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
```

#### LinkHistoryEntity
```kotlin
@Entity(tableName = "link_history")
data class LinkHistoryEntity(
    val id: Long,
    val appId: Long,
    val url: String,
    val pageTitle: String,
    val action: String,
    val format: String,
    val timestamp: Long,
    val referrer: String?
)
```

#### LinkSuggestion
```kotlin
data class LinkSuggestion(
    val url: String,
    val title: String,
    val reason: SuggestionReason,
    val score: Float,
    val timestamp: Long
)
```

## Usage Examples

### Setting Up Link Management

```kotlin
class WebAppActivity : AppCompatActivity() {
    private lateinit var linkSystem: LinkManagementSystem
    private lateinit var historyTracker: LinkHistoryTracker
    private lateinit var suggestionEngine: LinkSuggestionEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appId = 123L
        linkSystem = LinkManagementSystem(this, appId)
        historyTracker = LinkHistoryTracker(this, appId)
        suggestionEngine = LinkSuggestionEngine(historyTracker, linkSystem)
        
        // Setup link context menu in WebView
        linkSystem.setupLinkContextMenu(webView)
    }
}
```

### Copy and Share Workflow

```kotlin
// Get current page info
linkSystem.getCurrentPageInfo(webView) { url, title ->
    // Show format picker
    val dialog = LinkPickerBottomSheet(linkSystem) { selectedUrl, format ->
        // Copy in selected format
        val formatted = linkSystem.copyUrl(selectedUrl, title, format)
        
        // Record action
        lifecycleScope.launch {
            historyTracker.recordAction(
                url = selectedUrl,
                pageTitle = title,
                action = "copy",
                format = format.name
            )
        }
    }
    dialog.show(supportFragmentManager, "link_picker")
}
```

### Suggest Next Format

```kotlin
lifecycleScope.launch {
    val recommendation = suggestionEngine.getFormatRecommendations(url)
    
    // Use recommended format as default in UI
    formatPickerView.setDefaultFormat(recommendation.recommendedFormat)
}
```

### Generate Usage Report

```kotlin
lifecycleScope.launch {
    val report = historyTracker.generateReport()
    
    println("Total actions: ${report.totalActions}")
    println("Most used format: ${report.actionBreakdown}")
    println("Recent pages: ${report.recentPages}")
}
```

## Integration Points

### With WebView
- Long-press context menu on links
- JavaScript interface for link interactions
- Current page URL and title extraction

### With UI Components
- Format selection spinners
- Link picker sheets
- History list views
- Suggestion carousels

### With Other Systems
- Credential storage for link-specific credentials
- Widget system for quick link access
- Clipboard manager integration
- Intent-based sharing

## Performance Considerations

1. **Database Optimization**
   - Indexed queries on appId and timestamp
   - Efficient cleanup of old history
   - Pagination for large result sets

2. **Memory Management**
   - Flow-based reactive updates
   - Lazy loading of suggestions
   - Cleanup of temporary data

3. **Network Efficiency**
   - Local processing without network calls
   - Batch operations for multiple links
   - Efficient export formats

## Security & Privacy

1. **Data Protection**
   - Links stored in encrypted database
   - No transmission unless explicitly shared
   - User-controlled history cleanup

2. **Permission Model**
   - Clipboard access for copying
   - Share intent permissions
   - File system access for exports

3. **Privacy Controls**
   - Clear history option
   - Selective link deletion
   - Export data portability

## Future Enhancements

1. **Advanced Suggestions**
   - Machine learning for format prediction
   - Collaborative recommendations
   - Time-based suggestions

2. **Link Management**
   - QR code generation for links
   - Link shortening integration
   - URL metadata caching

3. **Sharing Enhancements**
   - Direct sharing to messaging apps
   - Cloud sync across devices
   - Link expiration and sharing permissions

4. **Analytics**
   - Detailed usage graphs
   - Link performance metrics
   - Category-based statistics

## Testing

### Unit Tests
- Format conversion logic
- String similarity calculations
- History queries and filters

### Integration Tests
- Database operations
- Flow-based updates
- UI component integration

### UI Tests
- Link picker dialog interaction
- Format selection
- Share functionality

## Configuration

Link management can be configured through:
- Default formats (user preference)
- History retention period
- Export format preferences
- Suggestion algorithms

