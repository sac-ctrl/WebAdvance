package com.cylonid.nativealpha.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backups")
data class BackupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val path: String,
    val timestamp: Long = System.currentTimeMillis(),
    val size: Long
)