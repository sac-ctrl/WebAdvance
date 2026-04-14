package com.cylonid.nativealpha.data

import androidx.room.*
import com.cylonid.nativealpha.model.BackupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {

    @Query("SELECT * FROM backups ORDER BY timestamp DESC")
    fun getAllBackups(): Flow<List<BackupEntity>>

    @Query("SELECT * FROM backups ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastBackup(): BackupEntity?

    @Query("SELECT * FROM backups WHERE id = :id")
    suspend fun getBackup(id: Long): BackupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupEntity): Long

    @Delete
    suspend fun deleteBackup(backup: BackupEntity)

    @Query("DELETE FROM backups WHERE id = :id")
    suspend fun deleteBackup(id: Long)
}