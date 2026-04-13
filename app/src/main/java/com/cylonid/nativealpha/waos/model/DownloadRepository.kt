package com.cylonid.nativealpha.waos.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object DownloadRepository {
    private const val DOWNLOAD_HISTORY_FILENAME = "waos_download_history.json"

    private fun getStorageFile(context: Context): File {
        return File(context.filesDir, DOWNLOAD_HISTORY_FILENAME)
    }

    fun loadDownloads(context: Context): MutableList<DownloadRecord> {
        val file = getStorageFile(context)
        if (!file.exists()) return mutableListOf()
        return try {
            val json = file.readText()
            val type = object : TypeToken<MutableList<DownloadRecord>>() {}.type
            Gson().fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun saveDownload(context: Context, record: DownloadRecord) {
        val records = loadDownloads(context)
        records.add(0, record)
        getStorageFile(context).writeText(Gson().toJson(records))
    }
}
