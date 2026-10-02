package com.fingo.finlauncher.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fingo.finlauncher.R
import com.fingo.finlauncher.data.AppModel
import com.fingo.finlauncher.ui.theme.AccentCyan
import com.fingo.finlauncher.ui.theme.DarkBackground
import com.fingo.finlauncher.ui.theme.DarkSurface
import com.fingo.finlauncher.ui.theme.TextPrimary
import com.fingo.finlauncher.ui.theme.TextSecondary

private enum class SettingsSection {
    MAIN, PRODUCTIVITY, THEMES, ADVANCED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    onChangeDefaultLauncher: () -> Unit,
    onRestartLauncher: () -> Unit,
    onUninstallLauncher: () -> Unit,
    allApps: List<AppModel>,
    onToggleHide: (AppModel) -> Unit,
    hapticsEnabled: Boolean,
    onToggleHaptics: (Boolean) -> Unit,
    quickReplies: Boolean,
    onToggleQuickReplies: (Boolean) -> Unit,
    calendarPreview: Boolean,
    onToggleCalendarPreview: (Boolean) -> Unit,
    weatherEnabled: Boolean,
    onToggleWeather: (Boolean) -> Unit,
    mediaPlayer: Boolean,
    onToggleMediaPlayer: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentSection by remember { mutableStateOf(SettingsSection.MAIN) }
    var showHiddenAppsSheet by remember { mutableStateOf(false) }

    BackHandler {
        if (showHiddenAppsSheet) {
            showHiddenAppsSheet = false
        } else if (currentSection != SettingsSection.MAIN) {
            currentSection = SettingsSection.MAIN
        } else {
            onClose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        AnimatedContent(
            targetState = currentSection,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "settings_nav"
        ) { section ->
            when (section) {
                SettingsSection.MAIN -> {
                    MainSettingsView(
                        onClose = onClose,
                        onOpenProductivity = { currentSection = SettingsSection.PRODUCTIVITY },
                        onOpenThemes = { currentSection = SettingsSection.THEMES },
                        onOpenAdvanced = { currentSection = SettingsSection.ADVANCED },
                        context = context
                    )
                }
                SettingsSection.PRODUCTIVITY -> {
                    ProductivityView(
                        onBack = { currentSection = SettingsSection.MAIN },
                        quickReplies = quickReplies,
                        onToggleQuickReplies = onToggleQuickReplies,
                        calendarPreview = calendarPreview,
                        onToggleCalendarPreview = onToggleCalendarPreview,
                        weatherEnabled = weatherEnabled,
                        onToggleWeather = onToggleWeather,
                        mediaPlayer = mediaPlayer,
                        onToggleMediaPlayer = onToggleMediaPlayer,
                        context = context
                    )
                }
                SettingsSection.THEMES -> {
                    ThemesView(
                        onBack = { currentSection = SettingsSection.MAIN }
                    )
                }
                SettingsSection.ADVANCED -> {
                    AdvancedView(
                        onBack = { currentSection = SettingsSection.MAIN },
                        onChangeDefaultLauncher = onChangeDefaultLauncher,
                        onRestartLauncher = onRestartLauncher,
                        onUninstallLauncher = onUninstallLauncher,
                        onOpenHiddenApps = { showHiddenAppsSheet = true },
                        hapticsEnabled = hapticsEnabled,
                        onToggleHaptics = onToggleHaptics,
                        context = context
                    )
                }
            }
        }
    }

    // Hidden Apps Management Sheet
    if (showHiddenAppsSheet) {
        val hiddenList = remember(allApps) { allApps.filter { it.isHidden } }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showHiddenAppsSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.hidden_apps),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (hiddenList.isEmpty()) {
                    Text(
                        text = "Hozirda yashirilgan ilovalar mavjud emas",
                        fontSize = 15.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(hiddenList, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onToggleHide(app) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = app.label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = "Unhide",
                                    tint = AccentCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 1. MAIN SETTINGS VIEW (1:1 Niagara Screenshot)
// =========================================================================
@Composable
private fun MainSettingsView(
    onClose: () -> Unit,
    onOpenProductivity: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenAdvanced: () -> Unit,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(36.dp))
            
            // Back/Close Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logo & Pill Header (Matching Niagara Screenshot 1:1)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dual Wave Brand Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0x2200E5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = "FinLauncher",
                        tint = AccentCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Niagara Oval Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 32.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.settings_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_free_version),
                            fontSize = 15.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }

        // Productivity
        item {
            SettingsMenuItem(
                icon = Icons.Outlined.Star,
                title = stringResource(R.string.productivity_title),
                subtitle = stringResource(R.string.productivity_subtitle),
                onClick = onOpenProductivity
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Themes
        item {
            SettingsMenuItem(
                icon = Icons.Outlined.ColorLens,
                title = stringResource(R.string.themes_title),
                subtitle = stringResource(R.string.themes_subtitle),
                onClick = onOpenThemes
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Help & Feedback
        item {
            SettingsMenuItem(
                icon = Icons.Outlined.Help,
                title = stringResource(R.string.help_feedback_title),
                subtitle = "Savollar, takliflar va yordam",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aka-FinGo/FinLauncher"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Community
        item {
            SettingsMenuItem(
                icon = Icons.Outlined.People,
                title = stringResource(R.string.community_title),
                subtitle = "Foydalanuvchilar jamiyati va guruhlar",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aka-FinGo/FinLauncher/releases"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Advanced
        item {
            SettingsMenuItem(
                icon = Icons.Outlined.MoreHoriz,
                title = stringResource(R.string.advanced_title),
                subtitle = "Qo‘shimcha sozlamalar va parametrlar",
                onClick = onOpenAdvanced
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// =========================================================================
// 2. PRODUCTIVITY VIEW (1:1 with Screenshot)
// =========================================================================
@Composable
private fun ProductivityView(
    onBack: () -> Unit,
    quickReplies: Boolean,
    onToggleQuickReplies: (Boolean) -> Unit,
    calendarPreview: Boolean,
    onToggleCalendarPreview: (Boolean) -> Unit,
    weatherEnabled: Boolean,
    onToggleWeather: (Boolean) -> Unit,
    mediaPlayer: Boolean,
    onToggleMediaPlayer: (Boolean) -> Unit,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(36.dp))
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 36.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.productivity_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.instant_access),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE55B44),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Quick Replies
        item {
            SettingsSwitchItem(
                icon = Icons.Outlined.ChatBubbleOutline,
                title = stringResource(R.string.quick_replies),
                subtitle = stringResource(R.string.quick_replies_desc),
                checked = quickReplies,
                onCheckedChange = onToggleQuickReplies
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Calendar Agenda
        item {
            SettingsMenuItem(
                icon = Icons.Outlined.CalendarMonth,
                title = stringResource(R.string.calendar_agenda),
                subtitle = stringResource(R.string.calendar_agenda_desc),
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_APP_CALENDAR)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Calendar Preview
        item {
            SettingsSwitchItem(
                icon = Icons.Outlined.CalendarToday,
                title = stringResource(R.string.calendar_preview),
                subtitle = stringResource(R.string.calendar_preview_desc),
                checked = calendarPreview,
                onCheckedChange = onToggleCalendarPreview
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Weather Forecast
        item {
            SettingsSwitchItem(
                icon = Icons.Outlined.CloudQueue,
                title = stringResource(R.string.weather_forecast),
                subtitle = stringResource(R.string.weather_forecast_desc),
                checked = weatherEnabled,
                onCheckedChange = onToggleWeather
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Media Player
        item {
            SettingsSwitchItem(
                icon = Icons.Outlined.PlayCircleOutline,
                title = stringResource(R.string.media_player),
                subtitle = stringResource(R.string.media_player_desc),
                checked = mediaPlayer,
                onCheckedChange = onToggleMediaPlayer
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// =========================================================================
// 3. THEMES VIEW (1:1 with Screenshot)
// =========================================================================
@Composable
private fun ThemesView(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(36.dp))
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 36.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.themes_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            SettingsMenuItem(
                icon = Icons.Outlined.AddCircleOutline,
                title = stringResource(R.string.create_theme),
                subtitle = stringResource(R.string.create_theme_desc),
                onClick = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            SettingsMenuItem(
                icon = Icons.Outlined.Edit,
                title = stringResource(R.string.edit_theme),
                subtitle = stringResource(R.string.edit_theme_desc),
                onClick = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            SettingsMenuItem(
                icon = Icons.Outlined.FolderOpen,
                title = stringResource(R.string.my_themes),
                subtitle = stringResource(R.string.my_themes_desc),
                onClick = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            SettingsMenuItem(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.ready_themes),
                subtitle = stringResource(R.string.ready_themes_desc),
                onClick = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            SettingsMenuItem(
                icon = Icons.Outlined.Star,
                title = stringResource(R.string.community_themes),
                subtitle = stringResource(R.string.community_themes_desc),
                onClick = {}
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// =========================================================================
// 4. ADVANCED VIEW (1:1 with Screenshot)
// =========================================================================
@Composable
private fun AdvancedView(
    onBack: () -> Unit,
    onChangeDefaultLauncher: () -> Unit,
    onRestartLauncher: () -> Unit,
    onUninstallLauncher: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    hapticsEnabled: Boolean,
    onToggleHaptics: (Boolean) -> Unit,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(36.dp))
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 36.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.advanced_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // App info
        item {
            SettingsMenuItem(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.app_info),
                subtitle = "FinLauncher v1.0.06 (Free Edition)",
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Change default launcher
        item {
            SettingsMenuItem(
                icon = Icons.Outlined.Home,
                title = stringResource(R.string.change_default_launcher),
                subtitle = "FinLauncher-ni asosiy launcher qilish",
                onClick = onChangeDefaultLauncher
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Restart launcher
        item {
            SettingsMenuItem(
                icon = Icons.Outlined.Refresh,
                title = stringResource(R.string.restart_launcher),
                subtitle = stringResource(R.string.restart_launcher_desc),
                onClick = onRestartLauncher
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Hidden Apps
        item {
            SettingsMenuItem(
                icon = Icons.Outlined.Apps,
                title = stringResource(R.string.hidden_apps),
                subtitle = "Yashirilgan barcha ilovalarni boshqarish",
                onClick = onOpenHiddenApps
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Haptic feedback
        item {
            SettingsSwitchItem(
                icon = Icons.Outlined.Vibration,
                title = stringResource(R.string.haptic_feedback),
                subtitle = stringResource(R.string.haptic_feedback_desc),
                checked = hapticsEnabled,
                onCheckedChange = onToggleHaptics
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Uninstall launcher
        item {
            SettingsMenuItem(
                icon = Icons.Outlined.DeleteOutline,
                title = stringResource(R.string.uninstall_launcher),
                subtitle = "FinLauncher-ni qurilmadan o‘chirish",
                onClick = onUninstallLauncher
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Open source licenses
        item {
            SettingsMenuItem(
                icon = Icons.Outlined.OpenInNew,
                title = stringResource(R.string.open_source_licenses),
                subtitle = "Apache 2.0 & MIT litsenziyalari",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aka-FinGo/FinLauncher/blob/main/LICENSE"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// =========================================================================
// REUSABLE COMPONENTS
// =========================================================================
@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = TextPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = TextPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFE55B44),
                uncheckedThumbColor = Color(0xFF9E9E9E),
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}
