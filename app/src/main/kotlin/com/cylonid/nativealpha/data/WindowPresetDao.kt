package com.cylonid.nativealpha.data

import androidx.room.*
import com.cylonid.nativealpha.model.WindowPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WindowPresetDao {

    @Query("SELECT * FROM window_presets ORDER BY name ASC")
    fun getAllPresets(): Flow<List<WindowPresetEntity>>

    @Query("SELECT * FROM window_presets WHERE id = :id")
    suspend fun getPreset(id: Long): WindowPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: WindowPresetEntity): Long

    @Update
    suspend fun updatePreset(preset: WindowPresetEntity)

    @Delete
    suspend fun deletePreset(preset: WindowPresetEntity)

    @Query("DELETE FROM window_presets WHERE id = :id")
    suspend fun deletePreset(id: Long)
}