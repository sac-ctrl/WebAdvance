package com.cylonid.nativealpha.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var notificationsEnabled = true

    init {
        createDefaultChannel()
    }

    private fun createDefaultChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Native Alpha Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
    }

    fun isNotificationsEnabled(): Boolean = notificationsEnabled

    fun performNotificationAction(notificationId: Long, action: String) {
        when (action) {
            "dismiss" -> notificationManager.cancel(notificationId.toInt())
            "mark_read" -> { /* update read state in DB if needed */ }
            else -> { /* no-op for unknown actions */ }
        }
    }

    fun showNotification(id: Int, title: String, message: String) {
        if (!notificationsEnabled) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "native_alpha_channel"
    }
}
