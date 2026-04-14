package com.cylonid.nativealpha.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_settings")
data class SecuritySettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val biometricEnabled: Boolean = false,
    val autoLockMinutes: Int = 15,
    val encryptCredentials: Boolean = true,
    val clearDataOnFail: Boolean = false,
    val blockScreenshots: Boolean = false,
    val incognitoMode: Boolean = false
)