package com.fingo.finlauncher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
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
import com.fingo.finlauncher.ui.screens.OnboardingFavoritesScreen
import com.fingo.finlauncher.ui.screens.SettingsScreen
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
                val isFirstRunDone by preferencesManager.isFirstRunDoneFlow.collectAsState(initial = null)
                val favorites by preferencesManager.favoritesFlow.collectAsState(initial = emptySet())
                val hiddenApps by preferencesManager.hiddenAppsFlow.collectAsState(initial = emptySet())
                val notifications by NotificationListener.notificationsFlow.collectAsState(initial = emptyMap())
                
                val hapticsEnabled by preferencesManager.hapticsEnabledFlow.collectAsState(initial = true)
                val quickReplies by preferencesManager.quickRepliesFlow.collectAsState(initial = true)
                val calendarPreview by preferencesManager.calendarPreviewFlow.collectAsState(initial = true)
                val weatherEnabled by preferencesManager.weatherEnabledFlow.collectAsState(initial = true)
                val mediaPlayer by preferencesManager.mediaPlayerFlow.collectAsState(initial = true)

                val scope = rememberCoroutineScope()
                var isSettingsOpen by remember { mutableStateOf(false) }

                // Check and prompt default launcher on launch
                LaunchedEffect(Unit) {
                    if (!isDefaultLauncher()) {
                        promptSetDefaultLauncher()
                    }
                }

                // If first run, show onboarding screen to pick favorite apps for home screen
                if (isFirstRunDone == false && appsList.isNotEmpty()) {
                    OnboardingFavoritesScreen(
                        apps = appsList,
                        onComplete = { selectedSet ->
                            scope.launch {
                                preferencesManager.setFavorites(selectedSet)
                                preferencesManager.setFirstRunDone(true)
                            }
                        }
                    )
                } else if (isSettingsOpen) {
                    val enrichedApps = remember(appsList, favorites, hiddenApps) {
                        appsList.map { app ->
                            app.copy(
                                isFavorite = favorites.contains(app.packageName),
                                isHidden = hiddenApps.contains(app.packageName)
                            )
                        }
                    }

                    SettingsScreen(
                        onClose = { isSettingsOpen = false },
                        onChangeDefaultLauncher = { promptSetDefaultLauncher() },
                        onRestartLauncher = { recreate() },
                        onUninstallLauncher = { appRepository.uninstallApp(packageName) },
                        allApps = enrichedApps,
                        onToggleHide = { app ->
                            scope.launch {
                                preferencesManager.toggleHidden(app.packageName)
                            }
                        },
                        hapticsEnabled = hapticsEnabled,
                        onToggleHaptics = { enabled ->
                            scope.launch { preferencesManager.setHapticsEnabled(enabled) }
                        },
                        quickReplies = quickReplies,
                        onToggleQuickReplies = { enabled ->
                            scope.launch { preferencesManager.setQuickReplies(enabled) }
                        },
                        calendarPreview = calendarPreview,
                        onToggleCalendarPreview = { enabled ->
                            scope.launch { preferencesManager.setCalendarPreview(enabled) }
                        },
                        weatherEnabled = weatherEnabled,
                        onToggleWeather = { enabled ->
                            scope.launch { preferencesManager.setWeatherEnabled(enabled) }
                        },
                        mediaPlayer = mediaPlayer,
                        onToggleMediaPlayer = { enabled ->
                            scope.launch { preferencesManager.setMediaPlayer(enabled) }
                        }
                    )
                } else {
                    // Enrich app models with preferences and live notifications
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
                        },
                        onOpenSettings = {
                            isSettingsOpen = true
                        },
                        hapticsEnabled = hapticsEnabled
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshApps()
    }

    private fun refreshApps() {
        lifecycleScope.launch {
            val installed = appRepository.getInstalledApps(emptySet(), emptySet())
            appsList = installed
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun promptSetDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    startActivity(intent)
                    return
                }
            }
        }
        
        // Fallback for older Android versions
        try {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
