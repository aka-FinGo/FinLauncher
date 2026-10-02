package com.fingo.finlauncher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fingo.finlauncher.data.AppModel
import com.fingo.finlauncher.ui.components.AlphabetWaveSlider
import com.fingo.finlauncher.ui.components.AppItemRow
import com.fingo.finlauncher.ui.components.AppOptionsBottomSheet
import com.fingo.finlauncher.ui.components.MinimalHeader
import com.fingo.finlauncher.ui.components.SearchBar
import com.fingo.finlauncher.ui.theme.AccentCyan
import com.fingo.finlauncher.ui.theme.TextSecondary
import com.fingo.finlauncher.util.StatusBarHelper
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    apps: List<AppModel>,
    onLaunchApp: (AppModel) -> Unit,
    onToggleFavorite: (AppModel) -> Unit,
    onOpenAppInfo: (AppModel) -> Unit,
    onUninstallApp: (AppModel) -> Unit,
    onToggleHide: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDrawerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForOptions by remember { mutableStateOf<AppModel?>(null) }
    
    val drawerListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // System Back button closes Drawer
    BackHandler(enabled = isDrawerOpen) {
        if (searchQuery.isNotEmpty()) {
            searchQuery = ""
        } else {
            isDrawerOpen = false
        }
    }

    // Filter apps
    val visibleApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) {
            apps.filter { !it.isHidden }
        } else {
            apps.filter {
                !it.isHidden && it.label.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val favoriteApps = remember(apps) {
        apps.filter { !it.isHidden && it.isFavorite }
    }

    // Group apps alphabetically
    val groupedApps = remember(visibleApps, searchQuery) {
        if (searchQuery.isBlank()) {
            visibleApps
                .groupBy { it.firstLetter }
                .toSortedMap()
        } else {
            sortedMapOf(' ' to visibleApps)
        }
    }

    // Index mapping for instant wave slider scrolling (SearchBar is outside list now!)
    val letterIndexMap = remember(groupedApps, searchQuery) {
        val map = mutableMapOf<Char, Int>()
        if (searchQuery.isBlank()) {
            var currentIndex = 0
            groupedApps.forEach { (letter, appList) ->
                map[letter] = currentIndex
                currentIndex += appList.size + 1 // +1 for header
            }
        }
        map
    }

    val availableLetters = remember(groupedApps) {
        val letters = groupedApps.keys.filter { it != ' ' }
        listOf('★') + (if (letters.isEmpty()) ('A'..'Z').toList() else letters)
    }

    // Root Container - Transparent to display system wallpaper cleanly without flickering
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // ==========================================
        // 1. HOME SCREEN (Clean View with ONLY Favorites)
        // ==========================================
        if (!isDrawerOpen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp, bottom = 16.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            // Swipe UP anywhere on home screen opens Drawer
                            if (dragAmount < -15f) {
                                isDrawerOpen = true
                            }
                            // Swipe DOWN anywhere on home screen expands Notifications!
                            else if (dragAmount > 18f) {
                                StatusBarHelper.expandNotifications(context)
                            }
                        }
                    }
            ) {
                // Clock, Date, Battery
                MinimalHeader()

                Spacer(modifier = Modifier.height(12.dp))

                // Favorites List ONLY (NO alphabet letters cluttering the favorites!)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(favoriteApps, key = { "home_fav_${it.packageName}" }) { app ->
                        AppItemRow(
                            app = app,
                            onAppClick = { onLaunchApp(app) },
                            onAppLongClick = { selectedAppForOptions = app }
                        )
                    }
                }

                // Minimalist Swipe Up Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Swipe Up",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Barcha ilovalar",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ==========================================
        // 2. ALL APPS DRAWER (Solid Dark Slide-Up View)
        // ==========================================
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(260)
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(220)
            ) + fadeOut(animationSpec = tween(180))
        ) {
            // Solid dark backdrop prevents ghosting of favorites underneath
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF50D0E11))
                    .padding(top = 32.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // STICKY FIXED SEARCH BAR (Never scrolls away!)
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.padding(end = 50.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // All Apps List
                    LazyColumn(
                        state = drawerListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(end = 50.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    // Swipe DOWN from top returns to Home View
                                    if (dragAmount > 20f && drawerListState.firstVisibleItemIndex == 0 && searchQuery.isBlank()) {
                                        isDrawerOpen = false
                                    }
                                }
                            }
                    ) {
                        groupedApps.forEach { (letter, appList) ->
                            if (searchQuery.isBlank()) {
                                item(key = "drawer_header_$letter") {
                                    Text(
                                        text = letter.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            items(appList, key = { "drawer_${it.packageName}_${it.activityName}" }) { app ->
                                AppItemRow(
                                    app = app,
                                    onAppClick = { onLaunchApp(app) },
                                    onAppLongClick = { selectedAppForOptions = app }
                                )
                            }
                        }

                        item(key = "drawer_spacer") {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }

                // NIAGARA CURVED ALPHABET WAVE SLIDER (Active only in Drawer)
                AlphabetWaveSlider(
                    availableLetters = availableLetters,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onLetterSelected = { letter ->
                        if (letter == '★') {
                            isDrawerOpen = false
                        } else {
                            val targetIndex = letterIndexMap[letter]
                            if (targetIndex != null) {
                                coroutineScope.launch {
                                    drawerListState.scrollToItem(targetIndex)
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    // Long press options bottom sheet
    selectedAppForOptions?.let { app ->
        AppOptionsBottomSheet(
            app = app,
            onDismiss = { selectedAppForOptions = null },
            onToggleFavorite = { onToggleFavorite(app) },
            onOpenAppInfo = { onOpenAppInfo(app) },
            onUninstall = { onUninstallApp(app) },
            onToggleHide = { onToggleHide(app) }
        )
    }
}
