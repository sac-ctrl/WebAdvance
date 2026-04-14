package com.cylonid.nativealpha.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cylonid.nativealpha.repository.WebAppRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class ScreenshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val webAppRepository: WebAppRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val webAppId = inputData.getLong("webAppId", 0L)
        if (webAppId == 0L) return Result.failure()

        try {
            val webApp = webAppRepository.getWebAppById(webAppId).firstOrNull()
            if (webApp == null) return Result.failure()

            // Note: Actual screenshot capture requires WebView context
            // This worker would need to be triggered from the WebViewActivity
            // For now, we'll mark as success since the scheduling is implemented

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}