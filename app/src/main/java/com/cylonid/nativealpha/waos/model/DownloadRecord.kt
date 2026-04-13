package com.cylonid.nativealpha.waos.model

import java.util.*

data class DownloadRecord(
    val appId: Int,
    val fileName: String,
    val mimeType: String,
    val uriPath: String,
    val sizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    var status: String = "completed"
)
