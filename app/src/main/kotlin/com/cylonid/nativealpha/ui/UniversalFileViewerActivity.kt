package com.cylonid.nativealpha.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cylonid.nativealpha.ui.screens.UniversalFileViewerScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UniversalFileViewerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filePath = intent.getStringExtra("FILE_PATH") ?: ""

        setContent {
            UniversalFileViewerScreen(
                filePath = filePath,
                onBackPressed = { finish() }
            )
        }
    }
}