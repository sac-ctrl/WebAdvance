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
        )
    }

    // Check if permission is granted
    fun hasPermission(context: Context, permission: Permission): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission.androidPermission
        ) == PackageManager.PERMISSION_GRANTED
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
        return Permission.values().filter { !hasPermission(context, it) }
    }

    // Request permissions
    fun requestPermissions(
        activity: FragmentActivity,
        permissions: List<Permission>,
        requestCode: Int
    ) {
        val permissionsToRequest = permissions
            .filter { !hasPermission(activity, it) }
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
