package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CapturedNotificationItem(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

class AetherNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
    }

    override fun onListenerDisconnected() {
        _isConnected.value = false
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notif = sbn?.notification ?: return
        val extras = notif.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        val item = CapturedNotificationItem(
            key = sbn.key ?: "${sbn.packageName}_${sbn.postTime}",
            packageName = sbn.packageName ?: "unknown",
            title = title.ifBlank { "Notification" },
            text = text,
            timestamp = sbn.postTime
        )
        _capturedNotifications.value = (listOf(item) + _capturedNotifications.value).take(25)
    }

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _capturedNotifications = MutableStateFlow(
            listOf(
                CapturedNotificationItem(
                    key = "seed_1",
                    packageName = "com.aistudio.aetherosagent.vqxkpm",
                    title = "AetherOS Security & Automation Guard",
                    text = "End-to-End Encrypted Vault & Accessibility Bridge initialized.",
                    timestamp = System.currentTimeMillis()
                )
            )
        )
        val capturedNotifications: StateFlow<List<CapturedNotificationItem>> = _capturedNotifications.asStateFlow()
    }
}
