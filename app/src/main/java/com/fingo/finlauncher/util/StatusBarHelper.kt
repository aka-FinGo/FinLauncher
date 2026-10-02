package com.fingo.finlauncher.util

import android.annotation.SuppressLint
import android.content.Context

object StatusBarHelper {
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (_: Exception) {}
    }
}
