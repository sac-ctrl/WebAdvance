package com.cylonid.nativealpha.data

import com.cylonid.nativealpha.model.SecuritySettingsEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepository @Inject constructor(
    private val securitySettingsDao: SecuritySettingsDao
) {

    suspend fun getSecuritySettings(): SecuritySettingsEntity {
        return securitySettingsDao.getSettings() ?: SecuritySettingsEntity()
    }

    suspend fun saveSecuritySettings(settings: SecuritySettingsEntity) {
        securitySettingsDao.insertSettings(settings)
    }
}