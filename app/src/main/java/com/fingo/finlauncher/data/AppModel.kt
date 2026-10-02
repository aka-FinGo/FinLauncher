package com.fingo.finlauncher.data

import android.graphics.drawable.Drawable

data class AppModel(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val notificationCount: Int = 0,
    val latestNotificationText: String? = null
) {
    val firstLetter: Char
        get() {
            val first = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
            return if (first in 'A'..'Z') first else '#'
        }
}
