package com.cylonid.nativealpha.data

import com.cylonid.nativealpha.model.BackupEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val backupDao: BackupDao
) {

    fun getAllBackups(): Flow<List<BackupEntity>> {
        return backupDao.getAllBackups()
    }

    suspend fun getLastBackup(): BackupEntity? {
        return backupDao.getLastBackup()
    }

    suspend fun saveBackup(backup: BackupEntity) {
        backupDao.insertBackup(backup)
    }

    suspend fun deleteBackup(id: Long) {
        backupDao.deleteBackup(id)
    }

    suspend fun getBackup(id: Long): BackupEntity? {
        return backupDao.getBackup(id)
    }
}