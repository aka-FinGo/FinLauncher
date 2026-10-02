package com.fingo.finlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.fingo.finlauncher.data.AppModel
import com.fingo.finlauncher.data.AppRepository
import com.fingo.finlauncher.data.PreferencesManager
import com.fingo.finlauncher.service.NotificationListener
import com.fingo.finlauncher.ui.screens.HomeScreen
import com.fingo.finlauncher.ui.theme.FinLauncherTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appRepository: AppRepository
    private lateinit var preferencesManager: PreferencesManager

    private var appsList by mutableStateOf<List<AppModel>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        appRepository = AppRepository(this)
        preferencesManager = PreferencesManager(this)

        setContent {
            FinLauncherTheme {
                val favorites by preferencesManager.favoritesFlow.collectAsState(initial = emptySet())
                val hiddenApps by preferencesManager.hiddenAppsFlow.collectAsState(initial = emptySet())
                val notifications by NotificationListener.notificationsFlow.collectAsState(initial = emptyMap())
                val scope = rememberCoroutineScope()

                // Merge notification preview text into apps
                val enrichedApps = remember(appsList, favorites, hiddenApps, notifications) {
                    appsList.map { app ->
                        val notifText = notifications[app.packageName]
                        app.copy(
                            isFavorite = favorites.contains(app.packageName),
                            isHidden = hiddenApps.contains(app.packageName),
                            latestNotificationText = notifText,
                            notificationCount = if (notifText != null) 1 else 0
                        )
                    }
                }

                HomeScreen(
                    apps = enrichedApps,
                    onLaunchApp = { app ->
                        appRepository.launchApp(app)
                    },
                    onToggleFavorite = { app ->
                        scope.launch {
                            preferencesManager.toggleFavorite(app.packageName)
                        }
                    },
                    onOpenAppInfo = { app ->
                        appRepository.openAppInfo(app.packageName)
                    },
                    onUninstallApp = { app ->
                        appRepository.uninstallApp(app.packageName)
                    },
                    onToggleHide = { app ->
                        scope.launch {
                            preferencesManager.toggleHidden(app.packageName)
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshApps()
    }

    private fun refreshApps() {
        lifecycleScope.launch {
            // Initial read without waiting for datastore if not ready
            val installed = appRepository.getInstalledApps(emptySet(), emptySet())
            appsList = installed
        }
    }
}
