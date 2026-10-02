package com.fingo.finlauncher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationListener : NotificationListenerService() {

    companion object {
        private val _notificationsFlow = MutableStateFlow<Map<String, String>>(emptyMap())
        val notificationsFlow = _notificationsFlow.asStateFlow()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val pkg = it.packageName
            val title = it.notification.extras.getCharSequence("android.title")?.toString()
            val text = it.notification.extras.getCharSequence("android.text")?.toString()
            val summary = listOfNotNull(title, text).joinToString(": ")
            if (summary.isNotBlank()) {
                val current = _notificationsFlow.value.toMutableMap()
                current[pkg] = summary
                _notificationsFlow.value = current
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let {
            val pkg = it.packageName
            val current = _notificationsFlow.value.toMutableMap()
            current.remove(pkg)
            _notificationsFlow.value = current
        }
    }
}
