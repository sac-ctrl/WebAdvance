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

    var editId by mutableStateOf(0L)
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

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            repository.getWebAppById(id).collect { app ->
                app?.let {
                    val desktopUA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36"
                    editId = it.id
                    url = it.url
                    name = it.name
                    category = it.category ?: "General"
                    customGroup = it.customGroup ?: ""
                    isJavaScriptEnabled = it.isJavaScriptEnabled
                    isAdblockEnabled = it.isAdblockEnabled
                    isDarkModeEnabled = it.isDarkModeEnabled
                    isSmartRefreshEnabled = it.isSmartRefreshEnabled
                    isLocked = it.isLocked
                    pin = it.pin ?: ""
                    customDownloadFolder = it.customDownloadFolder ?: ""
                    clipboardMaxItems = it.clipboardMaxItems
                    credentialAutoLockTimeout = it.credentialAutoLockTimeout
                    floatingWindowDefaultWidth = it.floatingWindowDefaultWidth
                    floatingWindowDefaultHeight = it.floatingWindowDefaultHeight
                    floatingWindowDefaultOpacity = it.floatingWindowDefaultOpacity
                    screenshotSaveLocation = it.screenshotSaveLocation
                    linkCopierDefaultFormat = it.linkCopierDefaultFormat
                    userAgentOverride = it.userAgentOverride
                    cacheMode = it.cacheMode
                    isKeepAwake = it.isKeepAwake
                    isCameraPermission = it.isCameraPermission
                    isMicrophonePermission = it.isMicrophonePermission
                    isEnableZooming = it.isEnableZooming
                    useDesktopUserAgent = it.userAgent?.contains(desktopUA) == true
                    useCustomUserAgent = !it.userAgent.isNullOrBlank() && it.userAgent?.contains(desktopUA) == false
                    userAgent = if (useCustomUserAgent) it.userAgent ?: "" else ""
                    refreshIntervalText = when (it.refreshInterval) {
                        0L -> "Manual"
                        5000L -> "5s"
                        15000L -> "15s"
                        30000L -> "30s"
                        60000L -> "1min"
                        300000L -> "5min"
                        else -> "Manual"
                    }
                }
            }
        }
    }

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

        val desktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        val resolvedUserAgent = when {
            useCustomUserAgent && userAgent.isNotBlank() -> userAgent
            useDesktopUserAgent -> desktopUserAgent
            else -> null
        }

        val webApp = WebApp(
            id = if (editId > 0L) editId else 0L,
            name = name.ifBlank {
                url.replace("https://", "").replace("http://", "").replace("www.", "").substringBefore("/")
            },
            url = url,
            category = category,
            customGroup = customGroup.takeIf { it.isNotBlank() },
            userAgent = resolvedUserAgent,
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
            if (editId > 0L) {
                repository.updateWebApp(webApp)
            } else {
                repository.insertWebApp(webApp)
            }
        }
    }
}
