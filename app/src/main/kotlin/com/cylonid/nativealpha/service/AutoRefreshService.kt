package com.cylonid.nativealpha.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.webkit.WebView
import androidx.work.*
import com.cylonid.nativealpha.model.WebApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Service for managing auto-refresh of web apps
 */
@AndroidEntryPoint
class AutoRefreshService : Service() {
    
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            when (intent?.action) {
                ACTION_START_REFRESH -> {
                    val webAppId = intent.getLongExtra("appId", 0L)
                    val intervalSeconds = intent.getLongExtra("interval", 300L)
                    scheduleAutoRefresh(webAppId, intervalSeconds)
                }
                ACTION_STOP_REFRESH -> {
                    val webAppId = intent.getLongExtra("appId", 0L)
                    stopAutoRefresh(webAppId)
                }
                ACTION_REFRESH_NOW -> {
                    val webAppId = intent.getLongExtra("appId", 0L)
                    triggerImmediateRefresh(webAppId)
                }
            }
        }
        return START_STICKY
    }

    private fun scheduleAutoRefresh(appId: Long, intervalSeconds: Long) {
        if (intervalSeconds <= 0) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val refreshWork = PeriodicWorkRequestBuilder<RefreshWorker>(
            intervalSeconds, TimeUnit.SECONDS
        )
            .setConstraints(constraints)
            .setInputData(workDataOf(
                "appId" to appId,
                "interval" to intervalSeconds
            ))
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "refresh_app_$appId",
            ExistingPeriodicWorkPolicy.KEEP,
            refreshWork
        )
    }

    private fun stopAutoRefresh(appId: Long) {
        WorkManager.getInstance(applicationContext)
            .cancelUniqueWork("refresh_app_$appId")
    }

    private fun triggerImmediateRefresh(appId: Long) {
        val refreshWork = OneTimeWorkRequestBuilder<RefreshWorker>()
            .setInputData(workDataOf("appId" to appId))
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "refresh_now_$appId",
            ExistingWorkPolicy.REPLACE,
            refreshWork
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        const val ACTION_START_REFRESH = "com.cylonid.nativealpha.START_REFRESH"
        const val ACTION_STOP_REFRESH = "com.cylonid.nativealpha.STOP_REFRESH"
        const val ACTION_REFRESH_NOW = "com.cylonid.nativealpha.REFRESH_NOW"
    }
}

/**
 * Worker for handling refresh tasks
 */
class RefreshWorker(
    context: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val appId = inputData.getLong("appId", 0L)
            
            // Check if page content changed using stored hash
            val oldHash = getStoredPageHash(appId)
            val currentHash = getCurrentPageHash(appId)
            
            if (oldHash != currentHash) {
                // Page content changed, refresh
                triggerRefresh(appId)
                storePageHash(appId, currentHash)
                
                // Send notification of change
                sendNotification(appId, "Page content updated")
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun triggerRefresh(appId: Long) {
        // Refresh WebView - implemented via broadcast
        val intent = Intent("com.cylonid.nativealpha.REFRESH_APP_$appId")
        applicationContext.sendBroadcast(intent)
    }

    private fun getStoredPageHash(appId: Long): String {
        val prefs = applicationContext.getSharedPreferences("refresh", android.content.Context.MODE_PRIVATE)
        return prefs.getString("hash_$appId", "") ?: ""
    }

    private fun getCurrentPageHash(appId: Long): String {
        // In real implementation, would compute hash of page content
        return System.currentTimeMillis().toString()
    }

    private fun storePageHash(appId: Long, hash: String) {
        val prefs = applicationContext.getSharedPreferences("refresh", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("hash_$appId", hash).apply()
    }

    private fun sendNotification(appId: Long, message: String) {
        // Send broadcast for notification
        val intent = Intent("com.cylonid.nativealpha.REFRESH_NOTIFICATION").apply {
            putExtra("appId", appId)
            putExtra("message", message)
        }
        applicationContext.sendBroadcast(intent)
    }
}
