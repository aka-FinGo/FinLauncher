package com.fingo.finlauncher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.fingo.finlauncher.ui.theme.AccentCyan
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
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForOptions by remember { mutableStateOf<AppModel?>(null) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }

    // System Back button closes search or scrolls to top
    BackHandler {
        if (isSearchOpen) {
            isSearchOpen = false
            searchQuery = ""
        } else if (listState.firstVisibleItemIndex > 0) {
            coroutineScope.launch { listState.scrollToItem(0) }
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

    // Fast index mapping for 1-to-1 Niagara wave slider
    // Index 0 is Header (Clock + Date)
    // Indexes 1 .. favoriteApps.size are Favorites
    // Next indexes are Alphabet headers and apps
    val letterIndexMap = remember(favoriteApps, groupedApps) {
        val map = mutableMapOf<Char, Int>()
        // '☆' points to top (Favorites)
        map['☆'] = 0
        map['★'] = 0

        var currentIndex = 1 + favoriteApps.size
        groupedApps.forEach { (letter, appList) ->
            map[letter] = currentIndex
            currentIndex += 1 + appList.size // 1 for header + apps count
        }
        map
    }

    val availableLetters = remember(groupedApps) {
        val letters = groupedApps.keys.filter { it != ' ' }
        listOf('☆') + (if (letters.isEmpty()) ('A'..'Z').toList() else letters) + listOf('°')
    }

    // Search filtered apps
    val searchFilteredApps = remember(visibleApps, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else visibleApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
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
                .padding(end = 56.dp) // Space for the wave slider on the right
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        // Swipe DOWN at the top expands system notifications!
                        if (dragAmount > 22f && listState.firstVisibleItemIndex == 0) {
                            StatusBarHelper.expandNotifications(context)
                        }
                    }
                }
        ) {
            // A. Clock, Date, Battery
            item(key = "header_clock") {
                Spacer(modifier = Modifier.height(24.dp))
                MinimalHeader()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // B. Favorite Apps (Directly below clock!)
            items(favoriteApps, key = { "fav_${it.packageName}" }) { app ->
                AppItemRow(
                    app = app,
                    onAppClick = { onLaunchApp(app) },
                    onAppLongClick = { selectedAppForOptions = app }
                )
            }

            // C. Divider spacer before Alphabetical sections
            item(key = "divider_before_all") {
                Spacer(modifier = Modifier.height(28.dp))
            }

            // D. Alphabetical Grouped Apps (A..Z)
            groupedApps.forEach { (letter, appList) ->
                item(key = "header_$letter") {
                    Text(
                        text = letter.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
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

            // E. Footer: FinLauncher Settings & Info
            item(key = "footer_settings") {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "FinLauncher",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenSettings() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x3300E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "FinLauncher Sozlamalari (Pro Bepul)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(90.dp))
            }
        }

        // ==========================================
        // 2. NIAGARA WAVE SLIDER (Always on Right Edge)
        // ==========================================
        AlphabetWaveSlider(
            availableLetters = availableLetters,
            modifier = Modifier.align(Alignment.CenterEnd),
            onLetterSelected = { letter ->
                val targetIndex = letterIndexMap[letter] ?: letterIndexMap['☆'] ?: 0
                coroutineScope.launch {
                    listState.scrollToItem(targetIndex)
                }
            }
        )

        // ==========================================
        // 3. FLOATING SEARCH BUTTON (Bottom Right FAB)
        // ==========================================
        if (!isSearchOpen) {
            FloatingActionButton(
                onClick = { isSearchOpen = true },
                containerColor = Color(0xFFE55B44), // Niagara coral/red accent
                contentColor = PureBlack,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 65.dp, bottom = 28.dp)
                    .size(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ==========================================
        // 4. NIAGARA SEARCH OVERLAY
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
                    // Search Bar
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
                                    text = "Ilovalarni qidirish…",
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
