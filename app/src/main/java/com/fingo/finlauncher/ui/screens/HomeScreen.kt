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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fingo.finlauncher.R
import com.fingo.finlauncher.data.AppModel
import com.fingo.finlauncher.ui.components.AlphabetWaveSlider
import com.fingo.finlauncher.ui.components.AppItemRow
import com.fingo.finlauncher.ui.components.AppOptionsBottomSheet
import com.fingo.finlauncher.ui.components.MinimalHeader
import com.fingo.finlauncher.ui.components.SearchBar
import com.fingo.finlauncher.ui.theme.AccentCyan
import com.fingo.finlauncher.ui.theme.TextPrimary
import com.fingo.finlauncher.ui.theme.TextSecondary
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
    var isDrawerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForOptions by remember { mutableStateOf<AppModel?>(null) }
    
    val drawerListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Handle Android system back button
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

    // Index mapping for alphabet wave slider
    val letterIndexMap = remember(groupedApps, searchQuery) {
        val map = mutableMapOf<Char, Int>()
        if (searchQuery.isBlank()) {
            // Index 0: SearchBar
            var currentIndex = 1
            groupedApps.forEach { (letter, appList) ->
                map[letter] = currentIndex
                currentIndex += appList.size + 1 // +1 for section header
            }
        }
        map
    }

    val availableLetters = remember(groupedApps) {
        val letters = groupedApps.keys.filter { it != ' ' }
        listOf('★') + (if (letters.isEmpty()) ('A'..'Z').toList() else letters)
    }

    // Root Container - Transparent to show system wallpaper with subtle readability gradient
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x33000000),
                        Color(0x1A000000),
                        Color(0x4D000000)
                    )
                )
            )
    ) {
        // ==========================================
        // 1. HOME VIEW (Only Favorite Apps + Clock)
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp, bottom = 16.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        // Swipe UP opens All Apps Drawer
                        if (dragAmount < -18f && !isDrawerOpen) {
                            isDrawerOpen = true
                        }
                    }
                }
        ) {
            // Clock, Date, Battery
            MinimalHeader()

            Spacer(modifier = Modifier.height(8.dp))

            // Favorites List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(end = 46.dp) // Leave space for wave slider
            ) {
                items(favoriteApps, key = { "home_fav_${it.packageName}" }) { app ->
                    AppItemRow(
                        app = app,
                        onAppClick = { onLaunchApp(app) },
                        onAppLongClick = { selectedAppForOptions = app }
                    )
                }
            }

            // Swipe Up Indicator at the bottom
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

        // ==========================================
        // 2. ALL APPS DRAWER VIEW (Slide-Up Transition)
        // ==========================================
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(320)
            ) + fadeIn(animationSpec = tween(280)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(280)
            ) + fadeOut(animationSpec = tween(240))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xCC0D0E11),
                                Color(0xEE0D0E11)
                            )
                        )
                    )
                    .padding(top = 32.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            // Swipe DOWN returns to Home View
                            if (dragAmount > 22f && searchQuery.isBlank()) {
                                isDrawerOpen = false
                            }
                        }
                    }
            ) {
                LazyColumn(
                    state = drawerListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 46.dp) // Space for Alphabet Wave Slider
                ) {
                    // Search Bar
                    item(key = "drawer_search") {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Grouped All Apps A-Z
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
        }

        // ==========================================
        // 3. NIAGARA ALPHABET WAVE SLIDER (Fixed on the right)
        // ==========================================
        AlphabetWaveSlider(
            availableLetters = availableLetters,
            modifier = Modifier.align(Alignment.CenterEnd),
            onLetterSelected = { letter ->
                if (letter == '★') {
                    // Return to Home view with favorites
                    isDrawerOpen = false
                } else {
                    // Open drawer and scroll to letter
                    isDrawerOpen = true
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
