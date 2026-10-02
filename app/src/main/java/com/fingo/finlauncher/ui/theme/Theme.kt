package com.fingo.finlauncher.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LauncherColorScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentCyanLight,
    background = Color.Transparent,
    surface = Color.Transparent,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = PureBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun FinLauncherTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Ensure system wallpaper is visible and bars are transparent
                window.statusBarColor = AndroidColor.TRANSPARENT
                window.navigationBarColor = AndroidColor.TRANSPARENT
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = LauncherColorScheme,
        typography = Typography,
        content = content
    )
}
