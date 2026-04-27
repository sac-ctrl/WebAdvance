package com.cylonid.nativealpha.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cylonid.nativealpha.ui.screens.CredentialVaultScreen
import com.cylonid.nativealpha.waos.util.WaosConstants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CredentialVaultActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val webAppId = intent.getLongExtra(WaosConstants.EXTRA_WAOS_APP_ID, 0L)

        setContent {
            com.cylonid.nativealpha.ui.theme.WAOSTheme(
                themeMode = com.cylonid.nativealpha.ui.theme.ThemeState.mode
            ) {
                CredentialVaultScreen(
                    webAppId = webAppId,
                    onBackPressed = { finish() },
                    onAutoFill = { username, password ->
                        val resultIntent = android.content.Intent().apply {
                            putExtra("CREDENTIAL_USERNAME", username)
                            putExtra("CREDENTIAL_PASSWORD", password)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Auto-lock timer will be managed by ViewModel through composition state
    }

    override fun onPause() {
        super.onPause()
        // Auto-lock timer will be managed by ViewModel through composition state
    }
}