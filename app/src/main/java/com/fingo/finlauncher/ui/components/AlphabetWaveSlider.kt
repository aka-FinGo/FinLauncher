package com.fingo.finlauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt

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
    var isDragging by remember { mutableStateOf(false) }
    var columnHeight by remember { mutableFloatStateOf(0f) }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }

    // Wave arc maximum displacement in pixels (155dp deep into the screen like Niagara screenshot)
    val maxWaveArcPx = with(density) { 155.dp.toPx() }
    val waveRadiusPx = with(density) { 220.dp.toPx() }

    fun updateSelection(y: Float) {
        if (columnHeight <= 0f || availableLetters.isEmpty()) return
        val clampedY = y.coerceIn(0f, columnHeight - 1f)
        val itemHeight = columnHeight / availableLetters.size
        val index = (clampedY / itemHeight).toInt().coerceIn(0, availableLetters.size - 1)
        val letter = availableLetters[index]
        if (selectedLetter != letter) {
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
            .width(96.dp) // Wide touch target for thumb ergonomics
            .padding(vertical = 16.dp)
            .onGloballyPositioned { layoutCoordinates ->
                columnHeight = layoutCoordinates.size.height.toFloat()
            }
            .pointerInput(availableLetters) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        touchY = offset.y
                        updateSelection(offset.y)
                        tryAwaitRelease()
                        isDragging = false
                        touchY = -1f
                        selectedLetter = null
                    }
                )
            }
            .pointerInput(availableLetters) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        touchY = offset.y
                        updateSelection(offset.y)
                    },
                    onDragEnd = {
                        isDragging = false
                        touchY = -1f
                        selectedLetter = null
                    },
                    onDragCancel = {
                        isDragging = false
                        touchY = -1f
                        selectedLetter = null
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        touchY = change.position.y
                        updateSelection(change.position.y)
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
            availableLetters.forEachIndexed { index, letter ->
                val itemCenterY = if (count > 0 && columnHeight > 0) {
                    (index + 0.5f) * (columnHeight / count)
                } else -100f

                // Niagara Wave math: cosine bell-curve displacement
                val distance = if (touchY >= 0f) abs(touchY - itemCenterY) else 9999f
                val rawFraction = if (isDragging && distance < waveRadiusPx) {
                    cos((distance / waveRadiusPx) * (PI.toFloat() / 2f)).coerceIn(0f, 1f)
                } else 0f

                // Instantaneous calculation during drag (0ms lag!)
                val targetOffsetX = -rawFraction * maxWaveArcPx
                val targetScale = 1f + (rawFraction * 0.65f)

                // Smooth fade back to resting position on release
                val smoothOffsetX by animateFloatAsState(
                    targetValue = targetOffsetX,
                    animationSpec = tween(durationMillis = if (isDragging) 0 else 180),
                    label = "waveX"
                )

                val smoothScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = tween(durationMillis = if (isDragging) 0 else 180),
                    label = "waveS"
                )

                val isTarget = selectedLetter == letter

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .offset { IntOffset(smoothOffsetX.roundToInt(), 0) }
                        .scale(smoothScale)
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Only show normal text if it's not currently engulfed by the magnified bubble
                    if (!isTarget || !isDragging) {
                        Text(
                            text = letter.toString(),
                            fontSize = if (letter == '☆') 13.sp else 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.88f),
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 3f
                                )
                            )
                        )
                    }
                }
            }
        }

        // Magnified Niagara Coral Bubble at wave crest (1:1 with Screenshot)
        if (isDragging && selectedLetter != null && touchY in 0f..columnHeight) {
            val bubbleOffsetY = (touchY - with(density) { 28.dp.toPx() }).toInt()
            val bubbleOffsetX = -(maxWaveArcPx + with(density) { 8.dp.toPx() }).toInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(bubbleOffsetX, bubbleOffsetY) }
                    .size(56.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFE55B44)), // Niagara Coral accent
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
