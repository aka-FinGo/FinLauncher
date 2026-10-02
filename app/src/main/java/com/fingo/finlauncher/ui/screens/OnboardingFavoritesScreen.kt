package com.fingo.finlauncher.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fingo.finlauncher.data.AppModel
import com.fingo.finlauncher.ui.components.SearchBar
import com.fingo.finlauncher.ui.components.rememberNativeDrawablePainter
import com.fingo.finlauncher.ui.theme.AccentCyan
import com.fingo.finlauncher.ui.theme.DarkBackground
import com.fingo.finlauncher.ui.theme.PureBlack
import com.fingo.finlauncher.ui.theme.TextPrimary
import com.fingo.finlauncher.ui.theme.TextSecondary
import androidx.compose.foundation.Image

@Composable
fun OnboardingFavoritesScreen(
    apps: List<AppModel>,
    onComplete: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPackages = remember {
        // Pre-select some common apps if present (e.g. Phone, Messages, Browser, Telegram)
        val initial = mutableSetOf<String>()
        apps.forEach { app ->
            val p = app.packageName.lowercase()
            if (p.contains("dialer") || p.contains("messaging") || p.contains("telegram") ||
                p.contains("chrome") || p.contains("camera") || p.contains("whatsapp")
            ) {
                if (initial.size < 6) initial.add(app.packageName)
            }
        }
        mutableStateOf(initial)
    }

    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xE60D0E11),
                        Color(0xF50D0E11)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FinLauncher-ga xush kelibsiz! 👋",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Bosh ekranda ko'rinib turishi uchun sevimli ilovalaringizni tanlang (masalan, 4–8 ta):",
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 0.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Barcha ilovalar (${filteredApps.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Text(
                    text = "Tanlandi: ${selectedPackages.value.size}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isChecked = selectedPackages.value.contains(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val current = selectedPackages.value.toMutableSet()
                                if (isChecked) current.remove(app.packageName)
                                else current.add(app.packageName)
                                selectedPackages.value = current
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val painter = rememberNativeDrawablePainter(app.icon)
                            if (painter != null) {
                                Image(
                                    painter = painter,
                                    contentDescription = app.label,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = app.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                val current = selectedPackages.value.toMutableSet()
                                if (checked) current.add(app.packageName)
                                else current.remove(app.packageName)
                                selectedPackages.value = current
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AccentCyan,
                                checkmarkColor = PureBlack
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onComplete(selectedPackages.value) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Bosh Ekranga O'tish (${selectedPackages.value.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack
                )
            }
        }
    }
}
