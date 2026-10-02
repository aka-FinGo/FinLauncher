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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fingo.finlauncher.ui.theme.AccentCyan
import com.fingo.finlauncher.ui.theme.PureBlack
import com.fingo.finlauncher.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt

@Composable
fun AlphabetWaveSlider(
    modifier: Modifier = Modifier,
    availableLetters: List<Char> = listOf('★') + ('A'..'Z').toList(),
    onLetterSelected: (Char) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var touchY by remember { mutableFloatStateOf(-1f) }
    var isDragging by remember { mutableStateOf(false) }
    var columnHeight by remember { mutableFloatStateOf(0f) }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }

    // Wave arc maximum displacement in pixels (~75dp)
    val maxWaveArcPx = with(density) { 76.dp.toPx() }
    val waveRadiusPx = with(density) { 180.dp.toPx() }

    fun updateSelection(y: Float) {
        if (columnHeight <= 0f || availableLetters.isEmpty()) return
        val clampedY = y.coerceIn(0f, columnHeight - 1f)
        val itemHeight = columnHeight / availableLetters.size
        val index = (clampedY / itemHeight).toInt().coerceIn(0, availableLetters.size - 1)
        val letter = availableLetters[index]
        if (selectedLetter != letter) {
            selectedLetter = letter
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onLetterSelected(letter)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(85.dp) // Wide touch target for thumb ergonomics
            .padding(vertical = 20.dp)
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

                // Direct Niagara Wave math - instantaneous during drag, smooth release on end
                val distance = if (touchY >= 0f) abs(touchY - itemCenterY) else 9999f
                val rawFraction = if (isDragging && distance < waveRadiusPx) {
                    cos((distance / waveRadiusPx) * (PI.toFloat() / 2f)).coerceIn(0f, 1f)
                } else 0f

                // Instant calculation during drag (no animator delay!)
                val targetOffsetX = -rawFraction * maxWaveArcPx
                val targetScale = 1f + (rawFraction * 0.75f)

                // Smooth fade back to 0 on release
                val smoothOffsetX by animateFloatAsState(
                    targetValue = targetOffsetX,
                    animationSpec = tween(durationMillis = if (isDragging) 0 else 200),
                    label = "waveX"
                )

                val smoothScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = tween(durationMillis = if (isDragging) 0 else 200),
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
                    Text(
                        text = letter.toString(),
                        fontSize = if (letter == '★') 14.sp else if (isTarget) 14.sp else 11.sp,
                        fontWeight = if (isTarget) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = when {
                            isTarget -> AccentCyan
                            rawFraction > 0.35f -> TextPrimary
                            letter == '★' -> AccentCyan
                            else -> Color(0xD9FFFFFF)
                        }
                    )
                }
            }
        }

        // Magnified circular thumb badge attached to the wave crest
        if (isDragging && selectedLetter != null && touchY in 0f..columnHeight) {
            val bubbleOffsetY = (touchY - with(density) { 28.dp.toPx() }).toInt()
            val bubbleOffsetX = -(maxWaveArcPx + with(density) { 36.dp.toPx() }).toInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(bubbleOffsetX, bubbleOffsetY) }
                    .size(56.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(AccentCyan),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedLetter.toString(),
                    fontSize = if (selectedLetter == '★') 24.sp else 26.sp,
                    fontWeight = FontWeight.Black,
                    color = PureBlack
                )
            }
        }
    }
}
