package com.cylonid.nativealpha.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.ui.WebAppLauncherActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Creates a launcher home-screen shortcut for a [WebApp]. The shortcut icon is
 * the website's favicon centered on a clean light background, with a small
 * NexWeb OS badge in the top-right corner so the user can tell it's a NexWeb
 * app at a glance. Tapping the shortcut routes through [WebAppLauncherActivity]
 * which hands off to [WebAppRouter] for the proper sandboxed process.
 */
object HomeShortcutCreator {

    /** @return true if a pin request was issued (or attempted). */
    suspend fun pinToHomeScreen(context: Context, webApp: WebApp): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false

        val faviconBitmap = withContext(Dispatchers.IO) { loadFavicon(context, webApp) }
        val iconBitmap = composeIcon(context, faviconBitmap)

        val launchIntent = Intent(context, WebAppLauncherActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(WebAppRouter.EXTRA_WEB_APP_ID, webApp.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val shortcut = ShortcutInfoCompat.Builder(context, "waos_app_${webApp.id}")
            .setShortLabel(webApp.name.ifBlank { "NexWeb App" }.take(20))
            .setLongLabel(webApp.name.ifBlank { "NexWeb App" })
            .setIcon(IconCompat.createWithBitmap(iconBitmap))
            .setIntent(launchIntent)
            .build()

        return try {
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun loadFavicon(context: Context, webApp: WebApp): Bitmap? {
        val host = try { Uri.parse(webApp.url).host } catch (_: Exception) { null }
        // Prefer a high-res version of the favicon for the launcher icon.
        val candidates = buildList {
            webApp.iconUrl?.takeIf { it.isNotBlank() }?.let { add(it) }
            host?.let {
                add("https://www.google.com/s2/favicons?sz=256&domain=$it")
                add("https://icons.duckduckgo.com/ip3/$it.ico")
            }
        }
        for (url in candidates) {
            val bitmap = try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()
                val result = ImageLoader(context).execute(request)
                (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            } catch (_: Exception) { null }
            if (bitmap != null && bitmap.width > 1) return bitmap
        }
        return null
    }

    private fun composeIcon(context: Context, favicon: Bitmap?): Bitmap {
        // 192x192 is a good launcher-friendly size that scales well on every
        // density bucket without going crazy on memory.
        val size = 192
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // Light rounded-square background so favicons (which are usually
        // designed against white) read clearly on dark wallpapers.
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val radius = size * 0.22f
        canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), radius, radius, bgPaint)

        // Draw the favicon centered, fit to ~62% of the canvas.
        if (favicon != null) {
            val fSize = (size * 0.62f).toInt()
            val left = (size - fSize) / 2f
            val top = (size - fSize) / 2f
            val rect = RectF(left, top, left + fSize, top + fSize)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(favicon, null, rect, paint)
        }

        // Draw a small NexWeb OS badge in the top-right corner.
        try {
            val badge = BitmapFactory.decodeResource(context.resources, R.drawable.nexweb_logo)
            if (badge != null) {
                val badgeSize = (size * 0.34f) // visual diameter of badge
                val pad = size * 0.04f
                val cx = size - badgeSize / 2f - pad
                val cy = badgeSize / 2f + pad

                // White circle with subtle shadow behind the badge for contrast.
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    setShadowLayer(3f, 0f, 1f, Color.argb(110, 0, 0, 0))
                }
                canvas.drawCircle(cx, cy, badgeSize / 2f + pad * 0.6f, ringPaint)

                // Subtle outline ring.
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(40, 0, 0, 0)
                    style = Paint.Style.STROKE
                    strokeWidth = 1.2f
                }
                canvas.drawCircle(cx, cy, badgeSize / 2f + pad * 0.6f, outlinePaint)

                val srcRect = Rect(0, 0, badge.width, badge.height)
                val dstRect = RectF(
                    cx - badgeSize / 2f,
                    cy - badgeSize / 2f,
                    cx + badgeSize / 2f,
                    cy + badgeSize / 2f
                )
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(badge, srcRect, dstRect, badgePaint)
            }
        } catch (_: Exception) {}

        return out
    }
}
