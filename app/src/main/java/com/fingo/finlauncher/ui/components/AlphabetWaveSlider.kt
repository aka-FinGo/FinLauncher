package com.fingo.finlauncher.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Reverse-Engineered from Niagara Launcher 1.16.31 (classes NaZLkiZcQ4E9Qc7F5WGdQ & Y8RBzXNjmCEZkb)
 * 1. Wave distribution: Gaussian exponential formula: 2 ^ ( (1 / curvature) * (-4 * delta^2) )
 * 2. Magnetic Handle: Cubic easing: ((2 * relY)^3 / 2) * itemHeight for tactile letter snapping
 * 3. Physics: Damping ratio 0.5f, Stiffness 220f spring easing on release
 */
@Composable
fun AlphabetWaveSlider(
    modifier: Modifier = Modifier,
    availableLetters: List<Char> = listOf('☆') + ('A'..'Z').toList() + listOf('°'),
    hapticsEnabled: Boolean = true,
    onLetterSelected: (Char) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var touchY by remember { mutableFloatStateOf(-1f) }
    var touchXOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var columnHeight by remember { mutableFloatStateOf(0f) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }

    // Base wave arc max displacement in pixels (155dp standard, can extend to 180dp on pull)
    val baseWaveArcPx = with(density) { 155.dp.toPx() }
    val maxWaveArcPx = baseWaveArcPx + touchXOffset.coerceIn(0f, with(density) { 30.dp.toPx() })

    fun updateSelection(x: Float, y: Float, boxWidth: Float) {
        if (columnHeight <= 0f || availableLetters.isEmpty()) return
        val clampedY = y.coerceIn(0f, columnHeight - 1f)
        val itemHeight = columnHeight / availableLetters.size
        val index = (clampedY / itemHeight).toInt().coerceIn(0, availableLetters.size - 1)
        val letter = availableLetters[index]

        // Dynamic horizontal pull to expand wave depth when pulling left
        touchXOffset = (boxWidth - x - with(density) { 30.dp.toPx() }).coerceAtLeast(0f)

        if (selectedIndex != index) {
            selectedIndex = index
            selectedLetter = letter
            if (hapticsEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            onLetterSelected(letter)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(105.dp) // Wide thumb target zone for ergonomic edge gestures
            .padding(vertical = 16.dp)
            .onGloballyPositioned { layoutCoordinates ->
                columnHeight = layoutCoordinates.size.height.toFloat()
            }
            .pointerInput(availableLetters) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        touchY = offset.y
                        updateSelection(offset.x, offset.y, size.width.toFloat())
                        tryAwaitRelease()
                        isDragging = false
                        touchY = -1f
                        touchXOffset = 0f
                        selectedIndex = -1
                        selectedLetter = null
                    }
                )
            }
            .pointerInput(availableLetters) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        touchY = offset.y
                        updateSelection(offset.x, offset.y, size.width.toFloat())
                    },
                    onDragEnd = {
                        isDragging = false
                        touchY = -1f
                        touchXOffset = 0f
                        selectedIndex = -1
                        selectedLetter = null
                    },
                    onDragCancel = {
                        isDragging = false
                        touchY = -1f
                        touchXOffset = 0f
                        selectedIndex = -1
                        selectedLetter = null
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        touchY = change.position.y
                        updateSelection(change.position.x, change.position.y, size.width.toFloat())
                    }
                )
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.End
        ) {
            val count = availableLetters.size
            val countF = count.toFloat()

            availableLetters.forEachIndexed { index, letter ->
                val itemFraction = (index + 0.5f) / countF
                val touchFraction = if (columnHeight > 0f && touchY >= 0f) {
                    (touchY / columnHeight).coerceIn(0f, 1f)
                } else -10f

                val delta = itemFraction - touchFraction

                // True Niagara Gaussian formula: 2 ^ ( (1 / curvature) * (-4 * delta^2) )
                val curvature = 0.26f // Curvature parameter derived from Niagara APK
                val exponent = (1.0 / curvature) * (-4.0 * delta * delta)
                val rawFraction = if (isDragging && exponent > -7.0) {
                    2.0.pow(exponent).toFloat().coerceIn(0f, 1f)
                } else 0f

                val targetOffsetX = -rawFraction * maxWaveArcPx
                val targetScale = 1f + (rawFraction * 0.70f)

                // Niagara Spring Physics on release (Damping 0.5f, Stiffness 220f)
                val smoothOffsetX by animateFloatAsState(
                    targetValue = targetOffsetX,
                    animationSpec = if (isDragging) tween(0) else spring(
                        dampingRatio = 0.5f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "waveX"
                )

                val smoothScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = if (isDragging) tween(0) else spring(
                        dampingRatio = 0.5f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "waveS"
                )

                val isTarget = selectedIndex == index

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .offset { IntOffset(smoothOffsetX.roundToInt(), 0) }
                        .scale(smoothScale)
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isTarget || !isDragging) {
                        Text(
                            text = letter.toString(),
                            fontSize = if (letter == '☆') 13.sp else 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.90f),
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 3f
                                )
                            )
                        )
                    }
                }
            }
        }

        // Niagara Coral Bubble with Cubic Magnetic Snapping (1:1 with NaZLkiZcQ4E9Qc7F5WGdQ.java)
        if (isDragging && selectedIndex >= 0 && selectedLetter != null && columnHeight > 0f) {
            val itemHeight = columnHeight / availableLetters.size
            val letterCenterY = (selectedIndex + 0.5f) * itemHeight
            val relY = ((touchY - letterCenterY) / itemHeight).coerceIn(-0.5f, 0.5f)

            // Cubic magnetic easing: snaps bubble to center of current letter
            val snappedY = letterCenterY + (((relY * 2.0).pow(3.0) / 2.0).toFloat() * itemHeight)

            val bubbleOffsetY = (snappedY - with(density) { 28.dp.toPx() }).toInt()
            val bubbleOffsetX = -(maxWaveArcPx + with(density) { 6.dp.toPx() }).toInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(bubbleOffsetX, bubbleOffsetY) }
                    .size(56.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFE55B44)), // Genuine Niagara Coral accent
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedLetter.toString(),
                    fontSize = if (selectedLetter == '☆') 26.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
