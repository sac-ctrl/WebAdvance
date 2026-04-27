package com.cylonid.nativealpha.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Single source of truth for every permission the app surfaces in the
 * Permissions screen. Each enum entry carries:
 *
 *  - [androidPermission]  the underlying Manifest constant (or "" when the
 *                         permission doesn't exist on this Android version,
 *                         in which case [hasPermission] returns true).
 *  - [displayName]        a human label shown in the UI (e.g. "Camera").
 *  - [description]        one-line summary shown under the title.
 *  - [explanation]        longer "what this enables" text shown in the card.
 *  - [category]           grouping bucket on the screen.
 *  - [iconKey]            string key picked up by the screen to choose the
 *                         right Material icon (we keep enum free of Compose
 *                         deps so it stays usable from non-UI code).
 *  - [runtimeRequest]     true if [requestPermissions] should ask via the
 *                         standard runtime dialog. False for install-time
 *                         normal perms (INTERNET, MODIFY_AUDIO_SETTINGS) and
 *                         for special-access ones that go through Settings.
 *  - [specialAccess]      true for permissions that must be granted via a
 *                         Settings screen, not the runtime dialog (overlay,
 *                         all-files access, etc).
 */
object PermissionsManager {

    enum class Category(val displayName: String) {
        FILES("Files & Storage"),
        MEDIA("Camera, Mic & Audio"),
        LOCATION("Location"),
        NOTIFICATIONS("Notifications"),
        DISPLAY("Display Over Apps"),
        NETWORK("Network"),
    }

    enum class Permission(
        val id: String,
        val androidPermission: String,
        val displayName: String,
        val description: String,
        val explanation: String,
        val category: Category,
        val iconKey: String,
        val runtimeRequest: Boolean = true,
        val specialAccess: Boolean = false,
    ) {
        READ_STORAGE(
            id = "READ_STORAGE",
            androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES
            else
                Manifest.permission.READ_EXTERNAL_STORAGE,
            displayName = "Read Photos",
            description = "Read images from your gallery",
            explanation = "Lets web apps pick photos for uploads, and lets the in-app downloader save into shared media folders.",
            category = Category.FILES,
            iconKey = "image",
        ),
        READ_MEDIA_VIDEO(
            id = "READ_MEDIA_VIDEO",
            androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_VIDEO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE,
            displayName = "Read Videos",
            description = "Read videos from your gallery",
            explanation = "Required when a web app asks you to upload a video clip from the device.",
            category = Category.FILES,
            iconKey = "video",
        ),
        READ_MEDIA_AUDIO(
            id = "READ_MEDIA_AUDIO",
            androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_AUDIO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE,
            displayName = "Read Music & Audio",
            description = "Read audio files from your device",
            explanation = "Used when a web app needs to attach a song or voice memo from device storage.",
            category = Category.FILES,
            iconKey = "audiofile",
        ),
        WRITE_STORAGE(
            id = "WRITE_STORAGE",
            androidPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
            displayName = "Save to Storage",
            description = "Save downloads to public folders",
            explanation = "On Android 9 and below this is required for the in-app downloader to write to the public Downloads folder. Auto-granted on Android 10+.",
            category = Category.FILES,
            iconKey = "download",
        ),
        MANAGE_STORAGE(
            id = "MANAGE_STORAGE",
            androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            else
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            displayName = "All Files Access",
            description = "Manage every file on the device",
            explanation = "Optional. Only needed for the universal file viewer to open files outside the standard media folders. Granted via a system Settings page.",
            category = Category.FILES,
            iconKey = "folder",
            specialAccess = true,
            runtimeRequest = false,
        ),
        CAMERA(
            id = "CAMERA",
            androidPermission = Manifest.permission.CAMERA,
            displayName = "Camera",
            description = "Capture photos and video inside web apps",
            explanation = "Required for sites that take photos (Snapchat, Discord, video calls, QR scanners, etc.).",
            category = Category.MEDIA,
            iconKey = "camera",
        ),
        RECORD_AUDIO(
            id = "RECORD_AUDIO",
            androidPermission = Manifest.permission.RECORD_AUDIO,
            displayName = "Microphone",
            description = "Record audio inside web apps",
            explanation = "Required for voice messages, video calls, voice search and dictation in WebView pages.",
            category = Category.MEDIA,
            iconKey = "mic",
        ),
        MODIFY_AUDIO_SETTINGS(
            id = "MODIFY_AUDIO_SETTINGS",
            androidPermission = Manifest.permission.MODIFY_AUDIO_SETTINGS,
            displayName = "Audio Routing",
            description = "Switch between speaker, earpiece and headset",
            explanation = "Auto-granted on install. Lets WebRTC calls move audio between earpiece, speaker and Bluetooth headset.",
            category = Category.MEDIA,
            iconKey = "volume",
            runtimeRequest = false,
        ),
        ACCESS_FINE_LOCATION(
            id = "ACCESS_FINE_LOCATION",
            androidPermission = Manifest.permission.ACCESS_FINE_LOCATION,
            displayName = "Precise Location",
            description = "GPS-accurate location",
            explanation = "Lets sites like Maps, Uber and food delivery apps see your exact position.",
            category = Category.LOCATION,
            iconKey = "location",
        ),
        ACCESS_COARSE_LOCATION(
            id = "ACCESS_COARSE_LOCATION",
            androidPermission = Manifest.permission.ACCESS_COARSE_LOCATION,
            displayName = "Approximate Location",
            description = "Network-based, city-level location",
            explanation = "Lower-accuracy fallback when a site only needs to know your general area (weather, news, search).",
            category = Category.LOCATION,
            iconKey = "place",
        ),
        POST_NOTIFICATIONS(
            id = "POST_NOTIFICATIONS",
            androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.POST_NOTIFICATIONS
            else
                "",
            displayName = "Notifications",
            description = "Show notifications from web apps",
            explanation = "Required on Android 13+ for download status, web push notifications and foreground service alerts.",
            category = Category.NOTIFICATIONS,
            iconKey = "notifications",
        ),
        SYSTEM_ALERT_WINDOW(
            id = "SYSTEM_ALERT_WINDOW",
            androidPermission = Manifest.permission.SYSTEM_ALERT_WINDOW,
            displayName = "Display Over Other Apps",
            description = "Show floating WebView windows",
            explanation = "Required for the floating-window feature so a web app can stay on top while you use other apps. Granted via a system Settings page.",
            category = Category.DISPLAY,
            iconKey = "overlay",
            specialAccess = true,
            runtimeRequest = false,
        ),
        INTERNET(
            id = "INTERNET",
            androidPermission = Manifest.permission.INTERNET,
            displayName = "Internet",
            description = "Connect to the network",
            explanation = "Auto-granted on install. Required for everything the app does.",
            category = Category.NETWORK,
            iconKey = "wifi",
            runtimeRequest = false,
        ),
    }

    /** Permission state. Granted, denied (still askable), or permanently
     *  denied / blocked (must go through Settings). */
    enum class Status { GRANTED, DENIED, BLOCKED }

    fun hasPermission(context: Context, permission: Permission): Boolean {
        if (permission.androidPermission.isEmpty()) return true
        return when (permission) {
            Permission.SYSTEM_ALERT_WINDOW -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    android.provider.Settings.canDrawOverlays(context)
                else true
            }
            Permission.MANAGE_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    android.os.Environment.isExternalStorageManager()
                else ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            Permission.WRITE_STORAGE -> {
                // Auto-granted on Android 10+ (scoped storage handles it).
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) true
                else ContextCompat.checkSelfPermission(
                    context, permission.androidPermission
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> ContextCompat.checkSelfPermission(
                context, permission.androidPermission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasPermissions(context: Context, permissions: List<Permission>): Boolean =
        permissions.all { hasPermission(context, it) }

    fun getAllPermissionsStatus(context: Context): Map<Permission, Boolean> =
        Permission.values().associateWith { hasPermission(context, it) }

    fun getGrantedPermissions(context: Context): List<Permission> =
        Permission.values().filter { hasPermission(context, it) }

    fun getDeniedPermissions(context: Context): List<Permission> =
        Permission.values().filter { !hasPermission(context, it) && it.androidPermission.isNotEmpty() }

    /** All runtime-grantable permissions that aren't yet granted. Used by the
     *  "Grant all missing" button on the Permissions screen. */
    fun getMissingRuntimePermissions(context: Context): List<Permission> =
        Permission.values().filter {
            it.runtimeRequest && !it.specialAccess &&
                it.androidPermission.isNotEmpty() && !hasPermission(context, it)
        }

    fun requestPermissions(
        activity: FragmentActivity,
        permissions: List<Permission>,
        requestCode: Int
    ) {
        val toRequest = permissions
            .filter { it.runtimeRequest && !it.specialAccess && !hasPermission(activity, it) && it.androidPermission.isNotEmpty() }
            .map { it.androidPermission }
            .toTypedArray()
        if (toRequest.isNotEmpty()) {
            androidx.core.app.ActivityCompat.requestPermissions(activity, toRequest, requestCode)
        }
    }

    fun requestPermission(activity: FragmentActivity, permission: Permission, requestCode: Int) =
        requestPermissions(activity, listOf(permission), requestCode)

    fun shouldShowRationale(activity: FragmentActivity, permission: Permission): Boolean =
        androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
            activity, permission.androidPermission
        )

    fun formatPermissionDescription(permission: Permission): String =
        "${permission.displayName}: ${permission.description}"
}
