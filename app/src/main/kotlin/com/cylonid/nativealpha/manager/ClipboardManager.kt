package com.cylonid.nativealpha.manager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cylonid.nativealpha.data.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val webAppId: Long? = null, // null for global
    val content: String,
    val type: Type = Type.TEXT,
    val timestamp: Date = Date(),
    val isPinned: Boolean = false
) {
    enum class Type {
        TEXT, URL, IMAGE
    }
}

@Singleton
class ClipboardManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    private val systemClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val dao = database.clipboardItemDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun copyToAppClipboard(webAppId: Long, content: String, type: ClipboardItem.Type = ClipboardItem.Type.TEXT) {
        scope.launch {
            val item = ClipboardItem(
                webAppId = webAppId,
                content = content,
                type = type
            )
            dao.insertItem(item)
            // Optionally sync to system clipboard
            if (shouldSyncToSystem()) {
                copyToSystemClipboard(content)
            }
        }
    }

    fun copyToSystemClipboard(content: String) {
        val clip = ClipData.newPlainText("WAOS", content)
        systemClipboard.setPrimaryClip(clip)
    }

    fun getAppClipboardItems(webAppId: Long, limit: Int = 50) = dao.getItemsForApp(webAppId, limit)

    fun pinItem(itemId: Long) {
        scope.launch {
            dao.pinItem(itemId)
        }
    }

    fun deleteItem(itemId: Long) {
        scope.launch {
            dao.deleteItemById(itemId)
        }
    }

    fun clearAppClipboard(webAppId: Long) {
        scope.launch {
            dao.clearAppItems(webAppId)
        }
    }

    private fun shouldSyncToSystem(): Boolean {
        // TODO: Get from settings
        return true
    }
}