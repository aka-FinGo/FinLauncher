package com.fingo.finlauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fingo.finlauncher.ui.theme.AccentCyan
import com.fingo.finlauncher.ui.theme.PureBlack
import com.fingo.finlauncher.ui.theme.TextPrimary
import com.fingo.finlauncher.ui.theme.TextTertiary
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AlphabetWaveSlider(
    modifier: Modifier = Modifier,
    availableLetters: List<Char> = ('A'..'Z').toList() + listOf('#'),
    onLetterSelected: (Char) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var touchY by remember { mutableFloatStateOf(-1f) }
    var isDragging by remember { mutableStateOf(false) }
    var columnHeight by remember { mutableFloatStateOf(0f) }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }

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
            .width(42.dp)
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

                // Niagara wave distortion formula
                val distance = if (touchY >= 0f) abs(touchY - itemCenterY) else 9999f
                val waveRadius = 140f
                val waveFraction = if (distance < waveRadius) {
                    (1f - (distance / waveRadius))
                } else 0f

                val offsetXAnim by animateFloatAsState(
                    targetValue = if (isDragging) -waveFraction * 26f else 0f,
                    animationSpec = spring(stiffness = 600f),
                    label = "waveOffset"
                )

                val scaleAnim by animateFloatAsState(
                    targetValue = if (isDragging) 1f + (waveFraction * 0.7f) else 1f,
                    animationSpec = spring(stiffness = 600f),
                    label = "waveScale"
                )

                val isTargetLetter = selectedLetter == letter

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .offset { IntOffset(offsetXAnim.roundToInt(), 0) }
                        .scale(scaleAnim)
                        .padding(end = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        fontSize = if (isTargetLetter) 14.sp else 10.sp,
                        fontWeight = if (isTargetLetter) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isTargetLetter -> AccentCyan
                            waveFraction > 0.3f -> TextPrimary
                            else -> TextTertiary
                        }
                    )
                }
            }
        }

        // Niagara floating magnified thumb bubble
        if (isDragging && selectedLetter != null && touchY in 0f..columnHeight) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = -75.dp.roundToPx(),
                            y = (touchY - 26.dp.roundToPx()).toInt()
                        )
                    }
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(AccentCyan),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedLetter.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = PureBlack
                )
            }
        }
    }
}
