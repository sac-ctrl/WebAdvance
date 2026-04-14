package com.cylonid.nativealpha.data

import com.cylonid.nativealpha.model.WindowPresetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WindowPresetRepository @Inject constructor(
    private val windowPresetDao: WindowPresetDao
) {

    fun getAllPresets(): Flow<List<WindowPresetEntity>> {
        return windowPresetDao.getAllPresets()
    }

    suspend fun getPreset(id: Long): WindowPresetEntity? {
        return windowPresetDao.getPreset(id)
    }

    suspend fun savePreset(preset: WindowPresetEntity) {
        windowPresetDao.insertPreset(preset)
    }

    suspend fun deletePreset(id: Long) {
        windowPresetDao.deletePreset(id)
    }

    suspend fun updatePreset(preset: WindowPresetEntity) {
        windowPresetDao.updatePreset(preset)
    }
}