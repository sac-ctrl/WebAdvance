package com.cylonid.nativealpha.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cylonid.nativealpha.ui.screens.ClipboardManagerScreen
import com.cylonid.nativealpha.waos.util.WaosConstants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClipboardManagerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val webAppId = intent.getLongExtra(WaosConstants.EXTRA_CLIPBOARD_APP_ID, 0L)

        setContent {
            com.cylonid.nativealpha.ui.theme.WAOSTheme(
                themeMode = com.cylonid.nativealpha.ui.theme.ThemeState.mode
            ) {
                ClipboardManagerScreen(
                    webAppId = webAppId,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}