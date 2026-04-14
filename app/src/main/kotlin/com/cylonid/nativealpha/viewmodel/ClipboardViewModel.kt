package com.cylonid.nativealpha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.data.ClipboardItemDao
import com.cylonid.nativealpha.manager.ClipboardItem
import com.cylonid.nativealpha.manager.ClipboardManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClipboardViewModel @Inject constructor(
    private val clipboardDao: ClipboardItemDao,
    private val clipboardManager: ClipboardManager
) : ViewModel() {

    private var currentWebAppId: Long? = null
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _clipboardItems = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val clipboardItems: StateFlow<List<ClipboardItem>> = combine(
        _clipboardItems,
        _searchQuery
    ) { items, query ->
        if (query.isBlank()) items
        else items.filter { it.content.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadClipboardItems(webAppId: Long?) {
        currentWebAppId = webAppId
        viewModelScope.launch {
            if (webAppId == null) {
                // Global clipboard - combine all apps
                clipboardDao.getItemsForApp(0L).collect { items ->
                    _clipboardItems.value = items
                }
            } else {
                clipboardDao.getItemsForApp(webAppId, 50).collect { items ->
                    _clipboardItems.value = items
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun copyToSystemClipboard(content: String) {
        clipboardManager.copyToSystemClipboard(content)
    }

    fun copyToSystemClipboard(item: ClipboardItem, context: android.content.Context) {
        val systemClipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("WAOS", item.content)
        systemClipboard.setPrimaryClip(clip)
    }

    fun togglePin(item: ClipboardItem) {
        viewModelScope.launch {
            if (item.isPinned) {
                // Unpin - just update the item
                val updated = item.copy(isPinned = false)
                clipboardDao.insertItem(updated)
            } else {
                // Pin
                clipboardDao.pinItem(item.id)
            }
        }
    }

    fun pinItem(itemId: Long) {
        viewModelScope.launch {
            clipboardDao.pinItem(itemId)
        }
    }

    fun deleteItem(item: ClipboardItem) {
        viewModelScope.launch {
            clipboardDao.deleteItem(item)
        }
    }

    fun clearClipboard() {
        currentWebAppId?.let { webAppId ->
            viewModelScope.launch {
                clipboardDao.clearAppItems(webAppId)
            }
        }
    }

    fun clearAllItems(webAppId: Long) {
        viewModelScope.launch {
            clipboardDao.clearAppItems(webAppId)
        }
    }

    fun addClipboardItem(webAppId: Long, content: String, type: ClipboardItem.Type = ClipboardItem.Type.TEXT) {
        clipboardManager.copyToAppClipboard(webAppId, content, type)
    }
}