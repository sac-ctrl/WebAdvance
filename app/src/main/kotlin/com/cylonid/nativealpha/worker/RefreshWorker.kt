package com.cylonid.nativealpha.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cylonid.nativealpha.repository.WebAppRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class RefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: WebAppRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val webAppId = inputData.getLong("webAppId", 0L)
        if (webAppId == 0L) return Result.failure()

        return try {
            val webApp = repository.getWebAppById(webAppId).first()
            webApp?.let {
                // TODO: Implement actual refresh logic
                // For now, just update lastUpdated
                val updatedApp = it.copy(lastUpdated = java.util.Date())
                repository.updateWebApp(updatedApp)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}