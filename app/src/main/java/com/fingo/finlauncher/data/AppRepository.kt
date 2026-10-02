package com.fingo.finlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    suspend fun getInstalledApps(
        favoritePackages: Set<String>,
        hiddenPackages: Set<String>
    ): List<AppModel> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val ownPackageName = context.packageName

        resolveInfos
            .filter { it.activityInfo.packageName != ownPackageName }
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val activity = resolveInfo.activityInfo.name
                val label = resolveInfo.loadLabel(pm)?.toString() ?: pkg
                val icon = resolveInfo.loadIcon(pm)

                AppModel(
                    label = label,
                    packageName = pkg,
                    activityName = activity,
                    icon = icon,
                    isFavorite = favoritePackages.contains(pkg),
                    isHidden = hiddenPackages.contains(pkg)
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launchApp(app: AppModel) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(app.packageName, app.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        }
    }

    fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
