package com.cylonid.nativealpha.clipboard

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.cylonid.nativealpha.waos.model.ClipboardItem
import com.cylonid.nativealpha.waos.model.ClipboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

/**
 * Enhanced clipboard manager with per-app history, sync, and advanced features
 */
class EnhancedClipboardManager(
    private val context: Context,
    private val appId: Long
) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    private val _clipboardItems = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val clipboardItems: StateFlow<List<ClipboardItem>> = _clipboardItems.asStateFlow()

    private val _selectedItemId = MutableStateFlow<Long?>(null)
    val selectedItemId: StateFlow<Long?> = _selectedItemId.asStateFlow()

    private val _isSynced = MutableStateFlow(false)
    val isSynced: StateFlow<Boolean> = _isSynced.asStateFlow()

    init {
        loadClipboardItems()
        startMonitoringSystemClipboard()
    }

    /**
     * Copy text to system clipboard and store in history
     */
    fun copyToClipboard(text: String): Boolean {
        return try {
            val clip = ClipData.newPlainText("clipboard", text)
            clipboardManager.setPrimaryClip(clip)
            
            // Store in app history
            val item = ClipboardItem(
                appId = appId,
                text = text,
                imageData = null,
                isPinned = false,
                timestamp = System.currentTimeMillis()
            )
            ClipboardRepository.saveClipboardItem(context, item)
            loadClipboardItems()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Copy image to system clipboard and store in history
     */
    fun copyImageToClipboard(bitmap: Bitmap): Boolean {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val imageData = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
            
            // Store as MIME data
            val clip = ClipData.newPlainText("image", imageData)
            clipboardManager.setPrimaryClip(clip)
            
            // Store in history
            val item = ClipboardItem(
                appId = appId,
                text = null,
                imageData = imageData,
                isPinned = false,
                timestamp = System.currentTimeMillis()
            )
            ClipboardRepository.saveClipboardItem(context, item)
            loadClipboardItems()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Insert item from clipboard history
     */
    fun insertClipboardItem(item: ClipboardItem) {
        val clip = ClipData.newPlainText("clipboard", item.text ?: "")
        clipboardManager.setPrimaryClip(clip)
    }

    /**
     * Delete clipboard item
     */
    fun deleteClipboardItem(item: ClipboardItem) {
        ClipboardRepository.deleteClipboardItem(context, item)
        loadClipboardItems()
    }

    /**
     * Pin/unpin clipboard item
     */
    fun togglePinItem(item: ClipboardItem) {
        val updated = item.copy(isPinned = !item.isPinned)
        ClipboardRepository.updateClipboardItem(context, updated)
        loadClipboardItems()
    }

    /**
     * Search in clipboard history
     */
    fun searchClipboard(query: String): List<ClipboardItem> {
        return _clipboardItems.value.filter { item ->
            item.text?.contains(query, ignoreCase = true) == true
        }
    }

    /**
     * Clear all clipboard history
     */
    fun clearAllHistory() {
        val items = _clipboardItems.value
        items.forEach { ClipboardRepository.deleteClipboardItem(context, it) }
        loadClipboardItems()
    }

    /**
     * Clear old items (older than days)
     */
    fun clearOldItems(daysOld: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        val itemsToDelete = _clipboardItems.value.filter { it.timestamp < cutoffTime }
        itemsToDelete.forEach { ClipboardRepository.deleteClipboardItem(context, it) }
        loadClipboardItems()
    }

    /**
     * Export clipboard as JSON
     */
    fun exportAsJson(): String {
        val gson = com.google.gson.Gson()
        return gson.toJson(mapOf(
            "appId" to appId,
            "exportDate" to System.currentTimeMillis(),
            "items" to _clipboardItems.value
        ))
    }

    /**
     * Export clipboard as text file
     */
    fun exportAsText(): String {
        return _clipboardItems.value.joinToString("\n\n---\n\n") { item ->
            item.text ?: "[Image item]"
        }
    }

    /**
     * Select multiple items for operations
     */
    fun selectItem(itemId: Long) {
        _selectedItemId.value = itemId
    }

    /**
     * Merge selected items
     */
    fun mergeItems(items: List<ClipboardItem>): ClipboardItem {
        val merged = items.mapNotNull { it.text }.joinToString("\n---\n")
        return ClipboardItem(
            appId = appId,
            text = merged,
            imageData = null,
            isPinned = false,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Edit clipboard item
     */
    fun editClipboardItem(item: ClipboardItem, newText: String): ClipboardItem {
        val updated = item.copy(text = newText)
        ClipboardRepository.updateClipboardItem(context, updated)
        loadClipboardItems()
        return updated
    }

    /**
     * Get clipboard statistics
     */
    fun getStatistics(): ClipboardStatistics {
        val items = _clipboardItems.value
        return ClipboardStatistics(
            totalItems = items.size,
            textItems = items.count { it.text != null },
            imageItems = items.count { it.imageData != null },
            pinnedItems = items.count { it.isPinned },
            oldestItem = items.minByOrNull { it.timestamp }?.timestamp ?: 0L,
            newestItem = items.maxByOrNull { it.timestamp }?.timestamp ?: 0L
        )
    }

    /**
     * Sync with system clipboard if enabled
     */
    fun syncWithSystemClipboard(enabled: Boolean) {
        _isSynced.value = enabled
        if (enabled) {
            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()?.let {
                copyToClipboard(it)
            }
        }
    }

    /**
     * Get system clipboard content
     */
    fun getSystemClipboardContent(): String? {
        return try {
            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Monitor system clipboard for changes
     */
    private fun startMonitoringSystemClipboard() {
        clipboardManager.addPrimaryClipChangedListener {
            if (_isSynced.value) {
                getSystemClipboardContent()?.let { content ->
                    copyToClipboard(content)
                }
            }
        }
    }

    private fun loadClipboardItems() {
        val items = ClipboardRepository.loadClipboardItems(context)
        _clipboardItems.value = items.sortedByDescending { it.timestamp }
    }

    data class ClipboardStatistics(
        val totalItems: Int,
        val textItems: Int,
        val imageItems: Int,
        val pinnedItems: Int,
        val oldestItem: Long,
        val newestItem: Long
    )
}
