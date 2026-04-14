package com.cylonid.nativealpha.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.WebView
import androidx.core.app.NotificationCompat
import com.cylonid.nativealpha.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * Smart notification system with DOM change detection and keyword matching
 */
class SmartNotificationSystem(
    private val context: Context,
    private val appId: Long,
    private val appName: String
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationIdGenerator = AtomicInteger(1000)
    
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _keywords = MutableStateFlow<List<String>>(emptyList())
    val keywords: StateFlow<List<String>> = _keywords.asStateFlow()

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val channelId = "app_notifications_${appId}"

    private var lastPageHash = ""

    init {
        createNotificationChannel()
    }

    /**
     * Setup DOM monitoring in WebView
     */
    fun setupDOMMonitoring(webView: WebView) {
        if (!_isEnabled.value) return

        val keywords = _keywords.value.mapIndexed { index, kw -> "kw$index: '$kw'" }.joinToString(",")
        
        val script = """
            (function() {
                const keywords = {$keywords};
                
                // Store initial hash
                var lastHash = md5(document.body.innerText);
                
                // Monitor DOM changes
                const observer = new MutationObserver(function(mutations) {
                    const currentHash = md5(document.body.innerText);
                    if (currentHash !== lastHash) {
                        lastHash = currentHash;
                        
                        // Check for keywords
                        const pageText = document.body.innerText.toLowerCase();
                        for (const [key, keyword] of Object.entries(keywords)) {
                            if (pageText.includes(keyword.toLowerCase())) {
                                WebApp.reportContentChange(keyword);
                                break;
                            }
                        }
                    }
                });
                
                observer.observe(document.body, {
                    childList: true,
                    subtree: true,
                    characterData: true,
                    attributes: true
                });
            })();
            
            // Simple MD5 implementation
            function md5(str) {
                return str.split('').reduce((a, b) => {
                    a = ((a << 5) - a) + b.charCodeAt(0);
                    return a & a;
                }, 0).toString(16);
            }
        """.trimIndent()
        
        webView.evaluateJavascript(script) { }
    }

    /**
     * Add keyword to monitor
     */
    fun addKeyword(keyword: String): Boolean {
        return if (!keyword.isBlank()) {
            val keywords = _keywords.value.toMutableList()
            keywords.add(keyword.lowercase())
            _keywords.value = keywords
            true
        } else false
    }

    /**
     * Remove keyword
     */
    fun removeKeyword(keyword: String) {
        val keywords = _keywords.value.toMutableList()
        keywords.remove(keyword.lowercase())
        _keywords.value = keywords
    }

    /**
     * Clear all keywords
     */
    fun clearKeywords() {
        _keywords.value = emptyList()
    }

    /**
     * Report content change from WebView
     */
    fun reportContentChange(trigger: String) {
        val notification = AppNotification(
            id = notificationIdGenerator.incrementAndGet(),
            appId = appId,
            appName = appName,
            title = "$appName Updated",
            message = "Page content changed: $trigger",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            trigger = trigger
        )
        
        if (_isEnabled.value) {
            sendPushNotification(notification)
            
            val notifications = _notifications.value.toMutableList()
            notifications.add(0, notification)
            _notifications.value = notifications.take(100) // Keep last 100
        }
    }

    /**
     * Send push notification to system
     */
    private fun sendPushNotification(notification: AppNotification) {
        val intent = Intent(context, android.app.Activity::class.java).apply {
            putExtra("appId", notification.appId)
            putExtra("trigger", notification.trigger)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_baseline_notifications_24)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(notification.id, builder.build())
    }

    /**
     * Mark notification as read
     */
    fun markAsRead(notificationId: Int) {
        val notifications = _notifications.value.toMutableList()
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index >= 0) {
            notifications[index] = notifications[index].copy(isRead = true)
            _notifications.value = notifications
        }
    }

    /**
     * Delete notification
     */
    fun deleteNotification(notificationId: Int) {
        val notifications = _notifications.value.toMutableList()
        notifications.removeAll { it.id == notificationId }
        _notifications.value = notifications
        notificationManager.cancel(notificationId)
    }

    /**
     * Clear all notifications
     */
    fun clearAllNotifications() {
        _notifications.value.forEach { notificationManager.cancel(it.id) }
        _notifications.value = emptyList()
    }

    /**
     * Toggle notifications
     */
    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
    }

    /**
     * Get unread count
     */
    fun getUnreadCount(): Int {
        return _notifications.value.count { !it.isRead }
    }

    /**
     * Get notification statistics
     */
    fun getStatistics(): NotificationStatistics {
        val notifs = _notifications.value
        return NotificationStatistics(
            totalNotifications = notifs.size,
            unreadCount = notifs.count { !it.isRead },
            oldestNotification = notifs.minByOrNull { it.timestamp }?.timestamp ?: 0L,
            newestNotification = notifs.maxByOrNull { it.timestamp }?.timestamp ?: 0L,
            uniqueTriggers = notifs.map { it.trigger }.distinct().size
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from $appName"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    data class AppNotification(
        val id: Int,
        val appId: Long,
        val appName: String,
        val title: String,
        val message: String,
        val timestamp: Long,
        val isRead: Boolean,
        val trigger: String
    )

    data class NotificationStatistics(
        val totalNotifications: Int,
        val unreadCount: Int,
        val oldestNotification: Long,
        val newestNotification: Long,
        val uniqueTriggers: Int
    )
}
