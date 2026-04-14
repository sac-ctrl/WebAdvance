package com.cylonid.nativealpha.data

import androidx.room.*
import com.cylonid.nativealpha.model.SecuritySettingsEntity

@Dao
interface SecuritySettingsDao {

    @Query("SELECT * FROM security_settings LIMIT 1")
    suspend fun getSettings(): SecuritySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SecuritySettingsEntity)

    @Update
    suspend fun updateSettings(settings: SecuritySettingsEntity)
}