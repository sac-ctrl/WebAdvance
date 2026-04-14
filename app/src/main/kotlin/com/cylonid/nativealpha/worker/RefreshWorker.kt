package com.cylonid.nativealpha.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cylonid.nativealpha.repository.WebAppRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

@HiltWorker
class RefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: WebAppRepository
) : CoroutineWorker(context, workerParams) {

    private val client = OkHttpClient()

    override suspend fun doWork(): Result {
        val webAppId = inputData.getLong("webAppId", 0L)
        if (webAppId == 0L) return Result.failure()

        return try {
            val webApp = repository.getWebAppById(webAppId).first()
            webApp?.let { app ->
                val isSmartRefresh = app.isSmartRefreshEnabled
                
                if (isSmartRefresh) {
                    // Perform smart refresh - check for content changes
                    val hasChanges = checkForContentChanges(app.url)
                    if (hasChanges) {
                        // Content changed, update lastUpdated
                        val updatedApp = app.copy(lastUpdated = java.util.Date())
                        repository.updateWebApp(updatedApp)
                    }
                } else {
                    // Simple refresh - just update timestamp
                    val updatedApp = app.copy(lastUpdated = java.util.Date())
                    repository.updateWebApp(updatedApp)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkForContentChanges(url: String): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return false
            
            // Parse HTML and check for significant changes
            val doc = Jsoup.parse(html)
            
            // Remove dynamic content that changes frequently
            doc.select("script, style, .timestamp, .time, .date").remove()
            
            // Get text content hash
            val contentHash = doc.text().hashCode()
            
            // TODO: Compare with stored hash from previous refresh
            // For now, just return true if we can parse the page
            true
        } catch (e: Exception) {
            false
        }
    }
}