package com.cylonid.nativealpha.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cylonid.nativealpha.ui.screens.DownloadHistoryScreen
import com.cylonid.nativealpha.waos.util.WaosConstants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DownloadHistoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val webAppId = intent.getLongExtra(WaosConstants.EXTRA_DOWNLOAD_APP_ID, 0L)

        setContent {
            DownloadHistoryScreen(webAppId = webAppId)
        }
    }
}
