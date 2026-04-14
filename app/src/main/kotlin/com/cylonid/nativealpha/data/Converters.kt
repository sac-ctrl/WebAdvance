package com.cylonid.nativealpha.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import com.cylonid.nativealpha.manager.ClipboardItem
import com.cylonid.nativealpha.model.AdblockConfig
import com.cylonid.nativealpha.model.WindowEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStatus(status: WebApp.Status): String {
        return status.name
    }

    @TypeConverter
    fun fromClipboardType(type: ClipboardItem.Type): String {
        return type.name
    }

    @TypeConverter
    fun toClipboardType(type: String): ClipboardItem.Type {
        return ClipboardItem.Type.valueOf(type)
    }

    @TypeConverter
    fun fromWindowEntityList(windows: List<WindowEntity>): String {
        return gson.toJson(windows)
    }

    @TypeConverter
    fun toWindowEntityList(json: String): List<WindowEntity> {
        val type = object : TypeToken<List<WindowEntity>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromBitmap(bitmap: Bitmap?): ByteArray? {
        if (bitmap == null) return null
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    @TypeConverter
    fun toBitmap(bytes: ByteArray?): Bitmap? {
        if (bytes == null) return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toStringMap(json: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    @TypeConverter
    fun fromAdblockConfigList(list: List<AdblockConfig>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toAdblockConfigList(json: String): List<AdblockConfig> {
        val type = object : TypeToken<List<AdblockConfig>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}