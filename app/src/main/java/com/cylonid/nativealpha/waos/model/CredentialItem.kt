package com.cylonid.nativealpha.waos.model

data class CredentialItem(
    val appId: Int,
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val timestamp: Long = System.currentTimeMillis()
)
