package com.cylonid.nativealpha.waos.model

data class EncryptedCredentialItem(
    val appId: Int,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)
