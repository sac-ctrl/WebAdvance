package com.cylonid.nativealpha.waos.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object ClipboardRepository {
    private const val CLIPBOARD_HISTORY_FILENAME = "waos_clipboard_history.json"
    private const val DEFAULT_MAX_ITEMS = 50
    private const val GLOBAL_MAX_ITEMS = 500

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

    fun loadClipboardItemsForApp(context: Context, appId: Int): MutableList<ClipboardItem> {
        return loadClipboardItems(context).filter { it.appId == appId }.toMutableList()
    }

    fun saveClipboardItem(context: Context, item: ClipboardItem, maxItems: Int = DEFAULT_MAX_ITEMS) {
        val allRecords = loadClipboardItems(context).toMutableList()
        val appRecords = allRecords.filter { it.appId == item.appId }.toMutableList()
        val otherRecords = allRecords.filter { it.appId != item.appId }

        appRecords.add(0, item)
        val effectiveMax = maxItems.coerceIn(1, GLOBAL_MAX_ITEMS)
        while (appRecords.size > effectiveMax) {
            appRecords.removeLast()
        }

        val combined = (appRecords + otherRecords).toMutableList()
        while (combined.size > GLOBAL_MAX_ITEMS) {
            combined.removeLast()
        }

        getStorageFile(context).writeText(Gson().toJson(combined))
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

    fun clearAppClipboard(context: Context, appId: Int) {
        val records = loadClipboardItems(context)
        records.removeAll { it.appId == appId }
        saveClipboardItems(context, records)
    }

    fun getItemCount(context: Context, appId: Int): Int {
        return loadClipboardItems(context).count { it.appId == appId }
    }
}
