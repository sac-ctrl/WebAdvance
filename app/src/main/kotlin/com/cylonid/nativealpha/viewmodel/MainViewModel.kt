package com.cylonid.nativealpha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.repository.WebAppRepository
import com.cylonid.nativealpha.ui.screens.GroupOption
import com.cylonid.nativealpha.ui.screens.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: WebAppRepository
) : ViewModel() {

    private val _webApps = MutableStateFlow<List<WebApp>>(emptyList())
    val webApps: StateFlow<List<WebApp>> = _webApps

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortBy = MutableStateFlow(SortOption.NAME)
    val sortBy: StateFlow<SortOption> = _sortBy

    private val _groupBy = MutableStateFlow(GroupOption.NONE)
    val groupBy: StateFlow<GroupOption> = _groupBy

    private val _filteredWebApps = combine(_webApps, _searchQuery, _sortBy) { apps, query, sort ->
        val filtered = if (query.isBlank()) apps else apps.filter {
            it.name.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
        }
        sortWebApps(filtered, sort)
    }
    val filteredWebApps: StateFlow<List<WebApp>> = _filteredWebApps

    val groupedWebApps: StateFlow<Map<String, List<WebApp>>> = combine(_filteredWebApps, _groupBy) { apps, group ->
        if (group == GroupOption.NONE) emptyMap() else groupWebApps(apps, group)
    }

    init {
        loadWebApps()
    }

    private fun loadWebApps() {
        viewModelScope.launch {
            repository.getAllWebApps().collect { apps ->
                _webApps.value = apps
            }
        }
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortBy(sortOption: SortOption) {
        _sortBy.value = sortOption
    }

    fun updateGroupBy(groupOption: GroupOption) {
        _groupBy.value = groupOption
    }

    private fun sortWebApps(apps: List<WebApp>, sortOption: SortOption): List<WebApp> {
        return when (sortOption) {
            SortOption.NAME -> apps.sortedBy { it.name.lowercase() }
            SortOption.LAST_USED -> apps.sortedByDescending { it.lastUsed ?: Date(0) }
            SortOption.MOST_ACTIVE -> apps.sortedByDescending { it.usageCount }
            SortOption.CUSTOM -> apps.sortedBy { it.customOrder }
        }
    }

    private fun groupWebApps(apps: List<WebApp>, groupOption: GroupOption): Map<String, List<WebApp>> {
        return when (groupOption) {
            GroupOption.CATEGORY -> apps.groupBy { it.category ?: "Uncategorized" }
            GroupOption.STATUS -> apps.groupBy { getStatusString(it) }
            GroupOption.CUSTOM -> apps.groupBy { it.customGroup ?: "Default" }
            GroupOption.NONE -> emptyMap()
        }
    }

    private fun getStatusString(webApp: WebApp): String {
        return when {
            webApp.isActive -> "Active"
            webApp.isBackground -> "Background"
            else -> "Inactive"
        }
    }

    fun addWebApp(webApp: WebApp) {
        viewModelScope.launch {
            repository.insertWebApp(webApp)
        }
    }

    fun updateWebApp(webApp: WebApp) {
        viewModelScope.launch {
            repository.updateWebApp(webApp)
        }
    }

    fun deleteWebApp(webApp: WebApp) {
        viewModelScope.launch {
            repository.deleteWebApp(webApp)
        }
    }

    fun refreshWebApps() {
        loadWebApps()
    }
}