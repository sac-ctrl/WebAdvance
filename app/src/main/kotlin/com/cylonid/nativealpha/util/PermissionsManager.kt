package com.cylonid.nativealpha.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Manages all app permissions and permission requests
 */
object PermissionsManager {

    enum class Permission(val id: String, val androidPermission: String, val description: String) {
        READ_STORAGE(
            "READ_STORAGE",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES
            else
                Manifest.permission.READ_EXTERNAL_STORAGE,
            "Access to read files from storage"
        ),
        WRITE_STORAGE(
            "WRITE_STORAGE",
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            "Access to write files to storage"
        ),
        MANAGE_STORAGE(
            "MANAGE_STORAGE",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            else
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            "Full access to all files on device"
        ),
        CAMERA(
            "CAMERA",
            Manifest.permission.CAMERA,
            "Access to device camera for screenshots and video"
        ),
        INTERNET(
            "INTERNET",
            Manifest.permission.INTERNET,
            "Access to internet for web browsing"
        ),
        RECORD_AUDIO(
            "RECORD_AUDIO",
            Manifest.permission.RECORD_AUDIO,
            "Access to microphone for audio recording"
        ),
        ACCESS_FINE_LOCATION(
            "ACCESS_FINE_LOCATION",
            Manifest.permission.ACCESS_FINE_LOCATION,
            "Access to GPS location"
        ),
        ACCESS_COARSE_LOCATION(
            "ACCESS_COARSE_LOCATION",
            Manifest.permission.ACCESS_COARSE_LOCATION,
            "Access to network-based location"
        ),
        READ_MEDIA_VIDEO(
            "READ_MEDIA_VIDEO",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_VIDEO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE,
            "Access to read video files"
        ),
        READ_MEDIA_AUDIO(
            "READ_MEDIA_AUDIO",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_AUDIO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE,
            "Access to read audio files"
        ),
        SYSTEM_ALERT_WINDOW(
            "SYSTEM_ALERT_WINDOW",
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            "Display over other apps"
        ),
        POST_NOTIFICATIONS(
            "POST_NOTIFICATIONS",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.POST_NOTIFICATIONS
            else
                "",
            "Send notifications"
        )
    }

    // Check if permission is granted
    fun hasPermission(context: Context, permission: Permission): Boolean {
        // Skip permissions that don't exist on this Android version
        if (permission.androidPermission.isEmpty()) return true
        
        return when (permission) {
            Permission.SYSTEM_ALERT_WINDOW -> {
                // Special check for SYSTEM_ALERT_WINDOW
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.provider.Settings.canDrawOverlays(context)
                } else {
                    true // Granted by default on older versions
                }
            }
            Permission.MANAGE_STORAGE -> {
                // Special check for MANAGE_EXTERNAL_STORAGE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else {
                    // On older versions, check WRITE_EXTERNAL_STORAGE
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    permission.androidPermission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    // Check multiple permissions
    fun hasPermissions(context: Context, permissions: List<Permission>): Boolean {
        return permissions.all { hasPermission(context, it) }
    }

    // Get all required permissions with their status
    fun getAllPermissionsStatus(context: Context): Map<Permission, Boolean> {
        return Permission.values().associateWith { hasPermission(context, it) }
    }

    // Get granted permissions
    fun getGrantedPermissions(context: Context): List<Permission> {
        return Permission.values().filter { hasPermission(context, it) }
    }

    // Get denied permissions
    fun getDeniedPermissions(context: Context): List<Permission> {
        return Permission.values().filter { !hasPermission(context, it) && it.androidPermission.isNotEmpty() }
    }

    // Request permissions
    fun requestPermissions(
        activity: FragmentActivity,
        permissions: List<Permission>,
        requestCode: Int
    ) {
        val permissionsToRequest = permissions
            .filter { !hasPermission(activity, it) && it.androidPermission.isNotEmpty() }
            .map { it.androidPermission }
            .toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            androidx.core.app.ActivityCompat.requestPermissions(activity, permissionsToRequest, requestCode)
        }
    }

    // Request single permission
    fun requestPermission(
        activity: FragmentActivity,
        permission: Permission,
        requestCode: Int
    ) {
        requestPermissions(activity, listOf(permission), requestCode)
    }

    // Check if app needs to show rationale for permission
    fun shouldShowRationale(activity: FragmentActivity, permission: Permission): Boolean {
        return androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            permission.androidPermission
        )
    }

    fun formatPermissionDescription(permission: Permission): String {
        return "${permission.id}: ${permission.description}"
    }
}
