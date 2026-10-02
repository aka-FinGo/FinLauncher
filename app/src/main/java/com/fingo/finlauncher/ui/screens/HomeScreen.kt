package com.fingo.finlauncher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.fingo.finlauncher.ui.theme.DarkBackground
import com.fingo.finlauncher.ui.theme.TextTertiary
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForOptions by remember { mutableStateOf<AppModel?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Filter apps based on search query and hidden state
    val visibleApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) {
            apps.filter { !it.isHidden }
        } else {
            apps.filter {
                !it.isHidden && it.label.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val favoriteApps = remember(visibleApps, searchQuery) {
        if (searchQuery.isBlank()) {
            visibleApps.filter { it.isFavorite }
        } else emptyList()
    }

    // Group remaining apps by first letter
    val groupedApps = remember(visibleApps, searchQuery) {
        if (searchQuery.isBlank()) {
            visibleApps
                .groupBy { it.firstLetter }
                .toSortedMap()
        } else {
            sortedMapOf(' ' to visibleApps)
        }
    }

    // Map letters to index in the list for smooth wave scrolling
    val letterIndexMap = remember(favoriteApps, groupedApps, searchQuery) {
        val map = mutableMapOf<Char, Int>()
        if (searchQuery.isBlank()) {
            // Index 0: Header, Index 1: SearchBar, Index 2: Favorites Header (if any)
            var currentIndex = 2
            if (favoriteApps.isNotEmpty()) {
                currentIndex += favoriteApps.size + 1 // +1 for "Barcha ilovalar" header
            }
            groupedApps.forEach { (letter, appList) ->
                map[letter] = currentIndex
                currentIndex += appList.size + 1 // +1 for letter section header
            }
        }
        map
    }

    val availableLetters = remember(groupedApps) {
        val keys = groupedApps.keys.filter { it != ' ' }
        if (keys.isEmpty()) ('A'..'Z').toList() else keys
    }

    Scaffold(
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 40.dp) // Leave space for wave slider on the right
            ) {
                // 1. Clock, Date, Battery Header
                item(key = "header") {
                    MinimalHeader()
                }

                // 2. Search Bar
                item(key = "search") {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 3. Favorites Section (When not searching)
                if (searchQuery.isBlank() && favoriteApps.isNotEmpty()) {
                    item(key = "fav_title") {
                        Text(
                            text = stringResource(R.string.favorites_title),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }

                    items(favoriteApps, key = { "fav_${it.packageName}" }) { app ->
                        AppItemRow(
                            app = app,
                            onAppClick = { onLaunchApp(app) },
                            onAppLongClick = { selectedAppForOptions = app }
                        )
                    }

                    item(key = "all_apps_divider") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.all_apps_title),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                }

                // 4. Alphabet Grouped App List
                groupedApps.forEach { (letter, appList) ->
                    if (searchQuery.isBlank()) {
                        item(key = "header_$letter") {
                            Text(
                                text = letter.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }

                    items(appList, key = { "${it.packageName}_${it.activityName}" }) { app ->
                        AppItemRow(
                            app = app,
                            onAppClick = { onLaunchApp(app) },
                            onAppLongClick = { selectedAppForOptions = app }
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // 5. Niagara Ergonomic Alphabet Wave Slider (Fixed on the right edge)
            if (searchQuery.isBlank()) {
                AlphabetWaveSlider(
                    availableLetters = availableLetters,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onLetterSelected = { letter ->
                        val targetIndex = letterIndexMap[letter]
                        if (targetIndex != null) {
                            coroutineScope.launch {
                                listState.scrollToItem(targetIndex)
                            }
                        }
                    }
                )
            }
        }
    }

    // Long press options modal sheet
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
