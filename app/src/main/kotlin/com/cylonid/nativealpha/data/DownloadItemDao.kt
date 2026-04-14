package com.cylonid.nativealpha.data

import androidx.room.*
import com.cylonid.nativealpha.manager.DownloadItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadItemDao {
    @Query("SELECT * FROM downloads WHERE webAppId = :webAppId ORDER BY timestamp DESC")
    fun getDownloadsForApp(webAppId: Long): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE id = :downloadId")
    suspend fun getDownloadById(downloadId: Long): DownloadItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadItem)

    @Update
    suspend fun updateDownload(download: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :downloadId")
    suspend fun deleteDownload(downloadId: Long)

    @Query("DELETE FROM downloads WHERE webAppId = :webAppId")
    suspend fun deleteDownloadsForApp(webAppId: Long)
}