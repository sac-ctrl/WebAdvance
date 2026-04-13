package com.cylonid.nativealpha.waos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.waos.ui.WaosDashboardActivity
import com.cylonid.nativealpha.waos.util.WaosConstants

/**
 * Floating bubble service for WAOS.
 * Provides an overlay launcher and a future window manager host.
 */
class FloatingWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private var bubbleParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatingBubble()
        startForegroundServiceWithNotification()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createFloatingBubble() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.activity_floating_window, null)
        val bubbleIcon = bubbleView.findViewById<ImageView>(R.id.floating_bubble_icon)
        bubbleIcon.setOnTouchListener { view, event ->
            handleBubbleDrag(event)
            true
        }
        bubbleIcon.setOnClickListener {
            val dashboardIntent = Intent(this, WaosDashboardActivity::class.java)
            dashboardIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(dashboardIntent)
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 10
            y = 100
        }

        windowManager.addView(bubbleView, bubbleParams)
    }

    private fun handleBubbleDrag(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                bubbleParams?.let {
                    it.x = event.rawX.toInt() - bubbleView.width / 2
                    it.y = event.rawY.toInt() - bubbleView.height / 2
                    windowManager.updateViewLayout(bubbleView, it)
                }
            }
        }
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "waos_floating_service",
                "WAOS Floating Window",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, WaosDashboardActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, "waos_floating_service")
            .setContentTitle("WAOS floating window active")
            .setContentText("Tap to open the WAOS dashboard")
            .setSmallIcon(R.mipmap.native_alpha)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1001, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bubbleView.isInitialized) {
            windowManager.removeView(bubbleView)
        }
    }
}
