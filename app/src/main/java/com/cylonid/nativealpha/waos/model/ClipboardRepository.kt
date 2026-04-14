package com.cylonid.nativealpha.waos.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object ClipboardRepository {
    private const val CLIPBOARD_HISTORY_FILENAME = "waos_clipboard_history.json"

    private fun getStorageFile(context: Context): File {
        return File(context.filesDir, CLIPBOARD_HISTORY_FILENAME)
    }

    fun loadClipboardItems(context: Context): MutableList<ClipboardItem> {
        val file = getStorageFile(context)
        if (!file.exists()) return mutableListOf()
        return try {
            val json = file.readText()
            val type = object : TypeToken<MutableList<ClipboardItem>>() {}.type
            Gson().fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun saveClipboardItem(context: Context, item: ClipboardItem) {
        val records = loadClipboardItems(context).toMutableList()
        records.add(0, item)
        if (records.size > 50) {
            records.removeLast()
        }
        getStorageFile(context).writeText(Gson().toJson(records))
    }

    fun saveClipboardItems(context: Context, items: List<ClipboardItem>) {
        getStorageFile(context).writeText(Gson().toJson(items))
    }

    fun deleteClipboardItem(context: Context, item: ClipboardItem) {
        val records = loadClipboardItems(context)
        records.removeAll { it.timestamp == item.timestamp && it.text == item.text && it.appId == item.appId }
        saveClipboardItems(context, records)
    }

    fun updateClipboardItem(context: Context, item: ClipboardItem) {
        val records = loadClipboardItems(context)
        val index = records.indexOfFirst { it.timestamp == item.timestamp && it.appId == item.appId }
        if (index >= 0) {
            records[index] = item
            saveClipboardItems(context, records)
        }
    }
}
