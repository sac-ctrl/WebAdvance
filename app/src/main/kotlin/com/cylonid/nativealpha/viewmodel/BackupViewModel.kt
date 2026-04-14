package com.cylonid.nativealpha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.data.BackupRepository
import com.cylonid.nativealpha.model.BackupEntity
import com.cylonid.nativealpha.service.BackupService
import com.cylonid.nativealpha.ui.screens.BackupInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val backupService: BackupService
) : ViewModel() {

    private val _backupHistory = MutableStateFlow<List<BackupInfo>>(emptyList())
    val backupHistory: StateFlow<List<BackupInfo>> = _backupHistory.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _lastBackupTime = MutableStateFlow("")
    val lastBackupTime: StateFlow<String> = _lastBackupTime.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    init {
        loadBackupHistory()
        loadLastBackupTime()
    }

    private fun loadBackupHistory() {
        viewModelScope.launch {
            backupRepository.getAllBackups().collect { entities ->
                _backupHistory.value = entities.map { entity ->
                    BackupInfo(
                        id = entity.id,
                        name = entity.name,
                        date = dateFormat.format(Date(entity.timestamp)),
                        size = formatFileSize(entity.size),
                        path = entity.path
                    )
                }
            }
        }
    }

    private fun loadLastBackupTime() {
        viewModelScope.launch {
            val lastBackup = backupRepository.getLastBackup()
            _lastBackupTime.value = if (lastBackup != null) {
                dateFormat.format(Date(lastBackup.timestamp))
            } else {
                ""
            }
        }
    }

    fun createBackup() {
        _isBackingUp.value = true
        viewModelScope.launch {
            try {
                val backupPath = backupService.createBackup()
                if (backupPath != null) {
                    val backupEntity = BackupEntity(
                        name = "Backup ${System.currentTimeMillis()}",
                        path = backupPath,
                        timestamp = System.currentTimeMillis(),
                        size = java.io.File(backupPath).length()
                    )
                    backupRepository.saveBackup(backupEntity)
                    loadBackupHistory()
                    loadLastBackupTime()
                }
            } finally {
                _isBackingUp.value = false
            }
        }
    }

    fun restoreBackup(backup: BackupInfo) {
        _isRestoring.value = true
        viewModelScope.launch {
            try {
                val success = backupService.restoreBackup(backup.path)
                if (success) {
                    // Refresh data after restore
                    loadBackupHistory()
                    loadLastBackupTime()
                }
            } finally {
                _isRestoring.value = false
            }
        }
    }

    fun deleteBackup(backup: BackupInfo) {
        viewModelScope.launch {
            backupService.deleteBackupFile(backup.path)
            backupRepository.deleteBackup(backup.id)
            loadBackupHistory()
        }
    }

    fun exportData() {
        viewModelScope.launch {
            backupService.exportData()
        }
    }

    fun importData() {
        viewModelScope.launch {
            backupService.importData()
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.1f %s".format(size, units[unitIndex])
    }
}