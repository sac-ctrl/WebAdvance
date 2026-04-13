package com.cylonid.nativealpha.waos.model

data class ClipboardItem(
    val appId: Int,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val pinned: Boolean = false
)
