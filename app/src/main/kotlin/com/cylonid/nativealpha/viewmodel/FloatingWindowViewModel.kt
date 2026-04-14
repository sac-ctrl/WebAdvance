package com.cylonid.nativealpha.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.data.WindowPresetRepository
import com.cylonid.nativealpha.model.WindowPresetEntity
import com.cylonid.nativealpha.service.FloatingWindowService
import com.cylonid.nativealpha.ui.screens.FloatingWindowInfo
import com.cylonid.nativealpha.ui.screens.WindowPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FloatingWindowViewModel @Inject constructor(
    private val windowPresetRepository: WindowPresetRepository,
    private val floatingWindowService: FloatingWindowService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _openWindows = MutableStateFlow<List<FloatingWindowInfo>>(emptyList())
    val openWindows: StateFlow<List<FloatingWindowInfo>> = _openWindows.asStateFlow()

    private val _windowPresets = MutableStateFlow<List<WindowPreset>>(emptyList())
    val windowPresets: StateFlow<List<WindowPreset>> = _windowPresets.asStateFlow()

    init {
        loadPresets()
        observeOpenWindows()
    }

    private fun observeOpenWindows() {
        viewModelScope.launch {
            // This would typically observe the service's state
            // For now, we'll simulate with empty list
            _openWindows.value = emptyList()
        }
    }

    private fun loadPresets() {
        viewModelScope.launch {
            windowPresetRepository.getAllPresets().collect { entities ->
                _windowPresets.value = entities.map { entity ->
                    WindowPreset(
                        id = entity.id,
                        name = entity.name,
                        windows = entity.windows.map { windowEntity ->
                            FloatingWindowInfo(
                                id = windowEntity.id,
                                appName = windowEntity.appName,
                                x = windowEntity.x,
                                y = windowEntity.y,
                                width = windowEntity.width,
                                height = windowEntity.height,
                                isMinimized = windowEntity.isMinimized
                            )
                        }
                    )
                }
            }
        }
    }

    fun closeWindow(windowId: Long) {
        val intent = Intent(context, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_REMOVE_WINDOW
            putExtra("windowId", windowId)
        }
        context.startService(intent)
        updateOpenWindows()
    }

    fun minimizeWindow(windowId: Long) {
        val intent = Intent(context, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_MINIMIZE_WINDOW
            putExtra("windowId", windowId)
        }
        context.startService(intent)
        updateOpenWindows()
    }

    fun maximizeWindow(windowId: Long) {
        val intent = Intent(context, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_MAXIMIZE_WINDOW
            putExtra("windowId", windowId)
        }
        context.startService(intent)
        updateOpenWindows()
    }

    fun closeAllWindows() {
        val intent = Intent(context, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_CLOSE_ALL
        }
        context.startService(intent)
        _openWindows.value = emptyList()
    }

    fun saveCurrentLayout() {
        viewModelScope.launch {
            // Get current window positions from service
            val currentWindows = floatingWindowService.getCurrentWindows()
            if (currentWindows.isNotEmpty()) {
                val presetName = "Layout ${System.currentTimeMillis()}"
                val presetEntity = WindowPresetEntity(
                    name = presetName,
                    windows = currentWindows
                )
                windowPresetRepository.savePreset(presetEntity)
                loadPresets()
            }
        }
    }

    fun loadPreset(presetId: Long) {
        viewModelScope.launch {
            val preset = windowPresetRepository.getPreset(presetId)
            if (preset != null) {
                floatingWindowService.loadPreset(preset)
                updateOpenWindows()
            }
        }
    }

    fun deletePreset(presetId: Long) {
        viewModelScope.launch {
            windowPresetRepository.deletePreset(presetId)
            loadPresets()
        }
    }

    private fun updateOpenWindows() {
        // Update the list of open windows from the service
        val currentWindows = floatingWindowService.getCurrentWindows().map { entity ->
            FloatingWindowInfo(
                id = entity.id,
                appName = entity.appName,
                x = entity.x,
                y = entity.y,
                width = entity.width,
                height = entity.height,
                isMinimized = entity.isMinimized
            )
        }
        _openWindows.value = currentWindows
    }
}