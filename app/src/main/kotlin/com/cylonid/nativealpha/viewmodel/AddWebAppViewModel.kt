package com.cylonid.nativealpha.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.repository.WebAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddWebAppViewModel @Inject constructor(
    private val repository: WebAppRepository
) : ViewModel() {

    var url by mutableStateOf("")
    var name by mutableStateOf("")
    var category by mutableStateOf("General")
    var customGroup by mutableStateOf("")
    var useDesktopUserAgent by mutableStateOf(false)
    var useCustomUserAgent by mutableStateOf(false)
    var userAgent by mutableStateOf("")
    var isJavaScriptEnabled by mutableStateOf(true)
    var isAdblockEnabled by mutableStateOf(false)
    var isDarkModeEnabled by mutableStateOf(false)
    var refreshIntervalText by mutableStateOf("Manual")
    var isSmartRefreshEnabled by mutableStateOf(false)
    var isLocked by mutableStateOf(false)
    var pin by mutableStateOf("")
    var customDownloadFolder by mutableStateOf("")
    var clipboardMaxItems by mutableStateOf(50)
    var credentialAutoLockTimeout by mutableStateOf(300000L)
    var floatingWindowDefaultWidth by mutableStateOf(800)
    var floatingWindowDefaultHeight by mutableStateOf(600)
    var floatingWindowDefaultOpacity by mutableStateOf(1.0f)
    var screenshotSaveLocation by mutableStateOf("app")
    var linkCopierDefaultFormat by mutableStateOf("url")
    var userAgentOverride by mutableStateOf<String?>(null)
    var cacheMode by mutableStateOf("default")
    var isKeepAwake by mutableStateOf(false)
    var isCameraPermission by mutableStateOf(false)
    var isMicrophonePermission by mutableStateOf(false)
    var isEnableZooming by mutableStateOf(false)

    fun saveWebApp() {
        val refreshInterval = when (refreshIntervalText) {
            "Manual" -> 0L
            "5s" -> 5000L
            "15s" -> 15000L
            "30s" -> 30000L
            "1min" -> 60000L
            "5min" -> 300000L
            else -> 0L
        }

        val webApp = WebApp(
            name = name.ifBlank { url.replace("https://", "").replace("http://", "").replace("www.", "") },
            url = url,
            category = category,
            customGroup = customGroup.takeIf { it.isNotBlank() },
            userAgent = if (useCustomUserAgent) userAgent else null,
            isJavaScriptEnabled = isJavaScriptEnabled,
            isAdblockEnabled = isAdblockEnabled,
            isDarkModeEnabled = isDarkModeEnabled,
            refreshInterval = refreshInterval,
            isSmartRefreshEnabled = isSmartRefreshEnabled,
            isLocked = isLocked,
            pin = pin.takeIf { it.isNotBlank() },
            customDownloadFolder = customDownloadFolder.takeIf { it.isNotBlank() },
            clipboardMaxItems = clipboardMaxItems,
            credentialAutoLockTimeout = credentialAutoLockTimeout,
            floatingWindowDefaultWidth = floatingWindowDefaultWidth,
            floatingWindowDefaultHeight = floatingWindowDefaultHeight,
            floatingWindowDefaultOpacity = floatingWindowDefaultOpacity,
            screenshotSaveLocation = screenshotSaveLocation,
            linkCopierDefaultFormat = linkCopierDefaultFormat,
            userAgentOverride = userAgentOverride,
            cacheMode = cacheMode,
            isKeepAwake = isKeepAwake,
            isCameraPermission = isCameraPermission,
            isMicrophonePermission = isMicrophonePermission,
            isEnableZooming = isEnableZooming
        )

        viewModelScope.launch {
            repository.insertWebApp(webApp)
        }
    }
}