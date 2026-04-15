package com.cylonid.nativealpha.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility for handling screenshots for web apps
 * Screenshots are saved to: /storage/emulated/0/Download/WAOS/{AppDisplayName}/Screenshots/
 */
object ScreenshotUtil {

    /**
     * Save screenshot for a specific app
     * @param context Application context
     * @param appDisplayName App display name (used for folder)
     * @param bitmap Screenshot bitmap to save
     * @return File path if successful, null otherwise
     */
    fun saveScreenshot(
        context: Context,
        appDisplayName: String,
        bitmap: Bitmap
    ): String? {
        return try {
            val screenshotsDir = StorageUtil.getScreenshotsDir(appDisplayName)
            if (!screenshotsDir.exists()) {
                screenshotsDir.mkdirs()
            }

            val fileName = generateScreenshotFileName()
            val file = File(screenshotsDir, fileName)

            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, output)
                output.flush()
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate unique screenshot filename with timestamp
     */
    private fun generateScreenshotFileName(): String {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val timestamp = timeFormat.format(Date())
        return "screenshot_$timestamp.png"
    }

    /**
     * Get all screenshots for an app
     */
    fun getAppScreenshots(appDisplayName: String): List<File> {
        val screenshotsDir = StorageUtil.getScreenshotsDir(appDisplayName)
        return screenshotsDir.listFiles()
            ?.filter { it.isFile && it.extension == "png" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Delete a screenshot file
     */
    fun deleteScreenshot(filePath: String): Boolean {
        return try {
            File(filePath).delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get total screenshots count for an app
     */
    fun getScreenshotCount(appDisplayName: String): Int {
        return getAppScreenshots(appDisplayName).size
    }

    /**
     * Get total screenshots size in bytes
     */
    fun getScreenshotsTotalSize(appDisplayName: String): Long {
        return getAppScreenshots(appDisplayName).sumOf { it.length() }
    }
}
