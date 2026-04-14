package com.cylonid.nativealpha.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cylonid.nativealpha.ui.screens.ClipboardManagerScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClipboardManagerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val webAppId = intent.getLongExtra("WEB_APP_ID", 0L)

        setContent {
            ClipboardManagerScreen(
                webAppId = webAppId,
                onBackPressed = { finish() }
            )
        }
    }
}