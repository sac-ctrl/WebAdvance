package com.cylonid.nativealpha.repository

import com.cylonid.nativealpha.data.WebAppDao
import com.cylonid.nativealpha.model.WebApp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebAppRepository @Inject constructor(
    private val webAppDao: WebAppDao
) {
    fun getAllWebApps(): Flow<List<WebApp>> = webAppDao.getAllWebApps()

    suspend fun insertWebApp(webApp: WebApp) {
        webAppDao.insertWebApp(webApp)
    }

    suspend fun updateWebApp(webApp: WebApp) {
        webAppDao.updateWebApp(webApp)
    }

    suspend fun deleteWebApp(webApp: WebApp) {
        webAppDao.deleteWebApp(webApp)
    }

    fun getWebAppById(id: Long): Flow<WebApp?> = webAppDao.getWebAppById(id)
}