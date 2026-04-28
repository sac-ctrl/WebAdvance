package com.cylonid.nativealpha.util

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility for handling screenshots for web apps.
 * Screenshots live in the app-private folder
 *   /Android/data/<pkg>/files/WAOS/{AppDisplayName}/Screenshots/
 * so they are NOT exposed to Gallery or any device file manager.
 */
object ScreenshotUtil {

    /**
     * Save screenshot for a specific app.
     * @return File path if successful, null otherwise
     */
    fun saveScreenshot(
        context: Context,
        appDisplayName: String,
        bitmap: Bitmap
    ): String? {
        return try {
            val screenshotsDir = StorageUtil.getScreenshotsDir(context, appDisplayName)
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

    private fun generateScreenshotFileName(): String {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val timestamp = timeFormat.format(Date())
        return "screenshot_$timestamp.png"
    }

    fun getAppScreenshots(context: Context, appDisplayName: String): List<File> {
        val screenshotsDir = StorageUtil.getScreenshotsDir(context, appDisplayName)
        return screenshotsDir.listFiles()
            ?.filter { it.isFile && it.extension == "png" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun deleteScreenshot(filePath: String): Boolean {
        return try {
            File(filePath).delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getScreenshotCount(context: Context, appDisplayName: String): Int {
        return getAppScreenshots(context, appDisplayName).size
    }

    fun getScreenshotsTotalSize(context: Context, appDisplayName: String): Long {
        return getAppScreenshots(context, appDisplayName).sumOf { it.length() }
    }
}
