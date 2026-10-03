package com.fingo.finlauncher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.fingo.finlauncher.ui.theme.DarkBackground
import com.fingo.finlauncher.ui.theme.PureBlack
import com.fingo.finlauncher.ui.theme.TextPrimary
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
    onOpenSettings: () -> Unit,
    hapticsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForOptions by remember { mutableStateOf<AppModel?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }

    // System Back button closes search or scrolls back to top
    BackHandler {
        if (isSearchOpen) {
            isSearchOpen = false
            searchQuery = ""
        } else if (listState.firstVisibleItemIndex > 0) {
            coroutineScope.launch { listState.animateScrollToItem(0) }
        }
    }

    // Filter visible apps
    val visibleApps = remember(apps) {
        apps.filter { !it.isHidden }
    }

    val favoriteApps = remember(visibleApps) {
        visibleApps.filter { it.isFavorite }
    }

    // Group apps alphabetically
    val groupedApps = remember(visibleApps) {
        visibleApps
            .groupBy { it.firstLetter }
            .toSortedMap()
    }

    // Fast index mapping for 1:1 Niagara wave slider
    val (letterIndexMap, totalItemCount) = remember(favoriteApps, groupedApps) {
        val map = mutableMapOf<Char, Int>()
        map['☆'] = 0

        var currentIndex = 1 + favoriteApps.size // 1 for header + fav apps
        groupedApps.forEach { (letter, appList) ->
            map[letter] = currentIndex
            currentIndex += 1 + appList.size // 1 for letter header + apps
        }
        map['°'] = currentIndex // footer settings
        Pair(map, currentIndex + 1)
    }

    val availableLetters = remember(groupedApps) {
        val letters = groupedApps.keys.filter { it != ' ' }
        listOf('☆') + (if (letters.isEmpty()) ('A'..'Z').toList() else letters) + listOf('°')
    }

    // Detect if scrolled near bottom to morph FAB into Settings icon
    val isNearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= (totalItemCount - 4).coerceAtLeast(0)
        }
    }

    // Search filtered apps
    val searchFilteredApps = remember(visibleApps, searchQuery) {
        if (searchQuery.isBlank()) visibleApps.take(15) // Recent / suggested apps when empty
        else visibleApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 45f && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    StatusBarHelper.expandNotifications(context)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // ==========================================
        // 1. UNIFIED NIAGARA CONTINUOUS LIST
        // ==========================================
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .padding(end = 40.dp) // Space for the wave slider on the right
        ) {
            // A. Clock, Date, Battery
            item(key = "header_clock") {
                Spacer(modifier = Modifier.height(28.dp))
                MinimalHeader()
                Spacer(modifier = Modifier.height(4.dp))
            }

            // B. Favorite Apps (Directly below clock!)
            items(favoriteApps, key = { "fav_${it.packageName}" }) { app ->
                AppItemRow(
                    app = app,
                    onAppClick = { onLaunchApp(app) },
                    onAppLongClick = { selectedAppForOptions = app }
                )
            }

            // C. Spacing before Alphabetical sections
            item(key = "divider_before_all") {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // D. Alphabetical Grouped Apps (A..Z)
            groupedApps.forEach { (letter, appList) ->
                item(key = "header_$letter") {
                    Text(
                        text = letter.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                items(appList, key = { "app_${it.packageName}_${it.activityName}" }) { app ->
                    AppItemRow(
                        app = app,
                        onAppClick = { onLaunchApp(app) },
                        onAppLongClick = { selectedAppForOptions = app }
                    )
                }
            }

            // E. Footer: FinLauncher Settings & Info (1:1 with Niagara screenshot)
            item(key = "footer_settings") {
                Spacer(modifier = Modifier.height(28.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "FinLauncher",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Recently Installed row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isSearchOpen = true }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33FFB74D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "Recent",
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.recently_installed),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }

                    // Niagara Settings row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenSettings() }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33E55B44)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFFE55B44),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.settings_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // ==========================================
        // 2. NIAGARA WAVE SLIDER (Right Edge)
        // ==========================================
        AlphabetWaveSlider(
            availableLetters = availableLetters,
            hapticsEnabled = hapticsEnabled,
            modifier = Modifier.align(Alignment.CenterEnd),
            onLetterSelected = { letter ->
                val targetIndex = letterIndexMap[letter] ?: 0
                coroutineScope.launch {
                    listState.scrollToItem(targetIndex, 0)
                }
            }
        )

        // ==========================================
        // 3. FLOATING ACTION BUTTON (Niagara Coral FAB)
        // Appears when scrolling down into All Apps (1:1 with Niagara)
        // ==========================================
        AnimatedVisibility(
            visible = !isSearchOpen && listState.firstVisibleItemIndex > 0,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 56.dp, bottom = 28.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    if (isNearBottom) {
                        onOpenSettings()
                    } else {
                        isSearchOpen = true
                    }
                },
                containerColor = Color(0xFFE55B44), // Niagara coral/red accent
                contentColor = PureBlack,
                shape = CircleShape,
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    imageVector = if (isNearBottom) Icons.Outlined.Settings else Icons.Outlined.Search,
                    contentDescription = if (isNearBottom) "Settings" else "Search",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ==========================================
        // 4. NIAGARA SEARCH OVERLAY (1:1 with Screenshot)
        // ==========================================
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = slideInVertically(initialOffsetY = { -it / 3 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it / 3 }) + fadeOut()
        ) {
            LaunchedEffect(isSearchOpen) {
                if (isSearchOpen) {
                    searchFocusRequester.requestFocus()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF50D0E11))
                    .padding(top = 28.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search Bar Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_hint),
                                    color = TextSecondary,
                                    fontSize = 16.sp
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0x33FFFFFF),
                                unfocusedContainerColor = Color(0x22FFFFFF),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = TextSecondary
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(searchFocusRequester)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = {
                            isSearchOpen = false
                            searchQuery = ""
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Search",
                                tint = TextPrimary
                            )
                        }
                    }

                    // Search Results
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                    ) {
                        items(searchFilteredApps, key = { "search_${it.packageName}_${it.activityName}" }) { app ->
                            AppItemRow(
                                app = app,
                                onAppClick = {
                                    isSearchOpen = false
                                    onLaunchApp(app)
                                },
                                onAppLongClick = { selectedAppForOptions = app }
                            )
                        }
                    }
                }
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
