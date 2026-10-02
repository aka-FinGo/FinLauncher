package com.fingo.finlauncher.data

import androidx.compose.ui.graphics.ImageBitmap

data class AppModel(
    val label: String,
    val packageName: String,
    val activityName: String,
    val iconBitmap: ImageBitmap? = null,
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
