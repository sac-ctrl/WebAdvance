package com.cylonid.nativealpha.data

import androidx.room.*
import com.cylonid.nativealpha.manager.ClipboardItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardItemDao {
    @Query("SELECT * FROM clipboard_items WHERE webAppId = :webAppId ORDER BY timestamp DESC LIMIT :limit")
    fun getItemsForApp(webAppId: Long, limit: Int): Flow<List<ClipboardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ClipboardItem)

    @Query("UPDATE clipboard_items SET isPinned = 1 WHERE id = :itemId")
    suspend fun pinItem(itemId: Long)

    @Delete
    suspend fun deleteItem(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Long)

    @Query("DELETE FROM clipboard_items WHERE webAppId = :webAppId")
    suspend fun clearAppItems(webAppId: Long)

    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC")
    suspend fun getAllItems(): List<ClipboardItem>

    @Query("DELETE FROM clipboard_items")
    suspend fun clearAll()
}