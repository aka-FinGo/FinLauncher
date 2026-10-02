package com.fingo.finlauncher.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fingo.finlauncher.data.AppModel
import com.fingo.finlauncher.ui.theme.AccentCyan
import com.fingo.finlauncher.ui.theme.DarkBackground
import com.fingo.finlauncher.ui.theme.PureBlack
import com.fingo.finlauncher.ui.theme.TextPrimary
import com.fingo.finlauncher.ui.theme.TextSecondary

@Composable
fun OnboardingFavoritesScreen(
    apps: List<AppModel>,
    onComplete: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPackages = remember {
        val initial = mutableSetOf<String>()
        apps.forEach { app ->
            val p = app.packageName.lowercase()
            if (p.contains("dialer") || p.contains("phone") || p.contains("messaging") ||
                p.contains("telegram") || p.contains("chrome") || p.contains("vending") ||
                p.contains("gallery") || p.contains("camera")
            ) {
                if (initial.size < 6) initial.add(app.packageName)
            }
        }
        mutableStateOf(initial)
    }

    val selectedList = remember(apps, selectedPackages.value) {
        apps.filter { selectedPackages.value.contains(it.packageName) }
    }

    val unselectedList = remember(apps, selectedPackages.value) {
        apps.filter { !selectedPackages.value.contains(it.packageName) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF00D0E11))
            .padding(top = 28.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Top Pill: "X selected · 8 recommended"
            item(key = "top_pill") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${selectedPackages.value.size} tanlandi · 8 tavsiya etiladi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Info Card 1: Star
            item(key = "info_card_1") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x22E55B44)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = "Star",
                            tint = Color(0xFFE55B44),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Sevimli ilovalar tezkor kirish uchun asosiy ekranda chiqadi",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Info Card 2: CheckCircle
            item(key = "info_card_2") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x22E55B44)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Check",
                            tint = Color(0xFFE55B44),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Ko'pchilik eng ko'p ishlatadigan 4 tadan 8 tagacha ilovalarni tanlaydi",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Section: Selected
            if (selectedList.isNotEmpty()) {
                item(key = "section_selected") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tanlanganlar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(selectedList, key = { "sel_${it.packageName}" }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val current = selectedPackages.value.toMutableSet()
                                current.remove(app.packageName)
                                selectedPackages.value = current
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = true,
                            onCheckedChange = {
                                val current = selectedPackages.value.toMutableSet()
                                current.remove(app.packageName)
                                selectedPackages.value = current
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFE55B44),
                                checkmarkColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (app.iconBitmap != null) {
                            Image(
                                bitmap = app.iconBitmap,
                                contentDescription = app.label,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = app.label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section: Suggestions
            item(key = "section_suggestions") {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Boshqa ilovalar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(unselectedList, key = { "unsel_${it.packageName}" }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val current = selectedPackages.value.toMutableSet()
                            current.add(app.packageName)
                            selectedPackages.value = current
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = false,
                        onCheckedChange = {
                            val current = selectedPackages.value.toMutableSet()
                            current.add(app.packageName)
                            selectedPackages.value = current
                        },
                        colors = CheckboxDefaults.colors(
                            uncheckedColor = Color(0x66FFFFFF)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (app.iconBitmap != null) {
                        Image(
                            bitmap = app.iconBitmap,
                            contentDescription = app.label,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = app.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // Floating Done Button at bottom right (matches Niagara screenshot!)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFE55B44))
                .clickable { onComplete(selectedPackages.value) }
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tayyor",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
