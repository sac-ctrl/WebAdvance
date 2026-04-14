package com.cylonid.nativealpha.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.cylonid.nativealpha.data.Converters

@Entity(tableName = "window_presets")
@TypeConverters(Converters::class)
data class WindowPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val windows: List<WindowEntity>
)

@Entity(tableName = "window_entities")
data class WindowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appName: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val isMinimized: Boolean = false
)