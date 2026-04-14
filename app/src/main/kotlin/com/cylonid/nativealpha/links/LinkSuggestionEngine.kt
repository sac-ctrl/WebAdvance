package com.cylonid.nativealpha.links

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Smart link suggestion engine
 */
class LinkSuggestionEngine(
    private val historyTracker: LinkHistoryTracker,
    private val linkManagementSystem: LinkManagementSystem
) {
    private val _suggestions = MutableStateFlow<List<LinkSuggestion>>(emptyList())
    val suggestions: Flow<List<LinkSuggestion>> = _suggestions

    /**
     * Generate suggestions based on history and patterns
     */
    suspend fun generateSuggestions(): List<LinkSuggestion> {
        val suggestions = mutableListOf<LinkSuggestion>()

        // Get frequent links
        val frequent = historyTracker.getMostFrequentLinks(5)
        suggestions.addAll(
            frequent.map { link ->
                LinkSuggestion(
                    url = link.url,
                    title = "Frequently used",
                    reason = SuggestionReason.FREQUENCY,
                    score = link.count.toFloat()
                )
            }
        )

        // Get action patterns
        val actions = historyTracker.getActionStatistics()
        val mostCommonAction = actions.maxByOrNull { it.count }?.action

        _suggestions.value = suggestions
        return suggestions
    }

    /**
     * Get contextual suggestions for current page
     */
    suspend fun getContextualSuggestions(
        currentUrl: String,
        currentPageTitle: String
    ): List<LinkSuggestion> {
        val suggestions = mutableListOf<LinkSuggestion>()

        // Find similar URLs from history
        val history = historyTracker.getRecentPageTitles(10)
        val similarTitles = history.filter { title ->
            calculateSimilarity(title, currentPageTitle) > 0.6f
        }

        suggestions.addAll(
            similarTitles.map { title ->
                LinkSuggestion(
                    url = currentUrl,
                    title = title,
                    reason = SuggestionReason.SIMILAR_CONTEXT,
                    score = calculateSimilarity(title, currentPageTitle)
                )
            }
        )

        return suggestions
    }

    /**
     * Get format recommendations
     */
    suspend fun getFormatRecommendations(url: String): LinkFormatRecommendation {
        val actions = historyTracker.getActionStatistics()
        val mostUsedAction = actions.maxByOrNull { it.count }?.action ?: "copy"

        val recommendation = when (mostUsedAction) {
            "markdown_copy" -> LinkManagementSystem.LinkFormat.MARKDOWN
            "html_share" -> LinkManagementSystem.LinkFormat.HTML_ANCHOR
            "titled_copy" -> LinkManagementSystem.LinkFormat.URL_WITH_TITLE
            else -> LinkManagementSystem.LinkFormat.PLAIN_URL
        }

        return LinkFormatRecommendation(
            recommendedFormat = recommendation,
            confidence = 0.8f,
            reason = "Based on your recent usage patterns"
        )
    }

    /**
     * Get saved links that match current domain
     */
    fun getRelatedSavedLinks(url: String): Flow<List<LinkManagementSystem.SavedLink>> {
        return linkManagementSystem.savedLinks.map { links ->
            val currentDomain = extractDomain(url)
            links.filter { link ->
                extractDomain(link.url) == currentDomain && link.url != url
            }
        }
    }

    /**
     * Calculate string similarity using Levenshtein distance
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        val maxLength = maxOf(s1.length, s2.length)
        if (maxLength == 0) return 1.0f

        val distance = levenshteinDistance(s1, s2)
        return 1.0f - (distance.toFloat() / maxLength)
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }

    /**
     * Extract domain from URL
     */
    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Learn from user feedback
     */
    fun recordFeedback(suggestion: LinkSuggestion, isUseful: Boolean) {
        // Update suggestion scoring model based on feedback
        // This would typically be persisted to improve future suggestions
    }
}

data class LinkSuggestion(
    val url: String,
    val title: String,
    val reason: SuggestionReason,
    val score: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class LinkFormatRecommendation(
    val recommendedFormat: LinkManagementSystem.LinkFormat,
    val confidence: Float,
    val reason: String
)

enum class SuggestionReason {
    FREQUENCY,           // Frequently visited
    SIMILAR_CONTEXT,     // Similar to current page
    TIME_BASED,         // Often visited at this time
    CATEGORY_MATCH,     // Matches page category
    SEARCH_HISTORY,     // Based on search queries
    RECENT_ACTIVITY,    // Recently visited
    TRENDING           // Trending in your usage
}

/**
 * Link recommendation observer for UI updates
 */
class LinkRecommendationObserver(
    private val suggestionEngine: LinkSuggestionEngine
) {
    fun observeSuggestions(onUpdate: (List<LinkSuggestion>) -> Unit) {
        // Setup flow collection for UI updates
    }

    fun observeFormatRecommendations(onUpdate: (LinkFormatRecommendation) -> Unit) {
        // Setup flow collection for format updates
    }

    fun recordUserPreference(url: String, format: LinkManagementSystem.LinkFormat) {
        // Track user preferences for better recommendations
    }
}
