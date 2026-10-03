package com.fingo.finlauncher.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * 1:1 Niagara Launcher Wave Slider (Hardware-Accelerated Canvas Implementation)
 *
 * Reverse-engineered directly from bitpit.launcher classes:
 * - b.NaZLkiZcQ4E9Qc7F5WGdQ.java
 * - b.Y8RBzXNjmCEZkb.java
 * - bitpit.launcher.scrollbar.ScrollbarView.java
 * - bitpit.launcher.scrollbar.ScrollbarHandle.java
 *
 * Key physics:
 * 1. Wave Arc: Gaussian function f = 2^( (1/c) * (-4 * delta^2) ) with curvature c = 0.26f
 * 2. Magnetic Handle: Cubic easing ((2 * relY)^3 / 2) * itemHeight for tactile letter snapping
 * 3. Spring Physics: Damping ratio 0.5f, Stiffness MediumLow spring easing on release
 * 4. Zero Recomposition: Drawn directly via Canvas at native 120 FPS
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
    val coroutineScope = rememberCoroutineScope()

    val waveProgress = remember { Animatable(0f) }
    var touchY by remember { mutableFloatStateOf(-1f) }
    var touchX by remember { mutableFloatStateOf(-1f) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }

    // Pre-allocated Android paints for 120 FPS zero-allocation drawing
    val letterPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 1f, 0x88000000.toInt())
        }
    }

    val bubblePaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = 0xFFE55B44.toInt() // Niagara coral accent
            style = Paint.Style.FILL
            setShadowLayer(14f, 0f, 4f, 0x55000000.toInt())
        }
    }

    val bubbleTextPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }

    // Density-dependent pixel sizes
    val touchStripWidthPx = with(density) { 65.dp.toPx() }
    val baseMarginRightPx = with(density) { 15.dp.toPx() }
    val baseWaveArcPx = with(density) { 145.dp.toPx() }
    val bubbleRadiusPx = with(density) { 26.dp.toPx() }
    val bubbleTextSizePx = with(density) { 25.sp.toPx() }
    val starBubbleTextSizePx = with(density) { 28.sp.toPx() }
    val baseLetterTextSizePx = with(density) {
        if (availableLetters.size > 22) 10.5.sp.toPx() else 12.5.sp.toPx()
    }

    bubbleTextPaint.textSize = bubbleTextSizePx

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(220.dp)
            .pointerInput(availableLetters) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()

                    // Only activate if touched within the right touch strip zone
                    if (down.position.x < width - touchStripWidthPx) {
                        return@awaitEachGesture
                    }

                    down.consume()
                    touchY = down.position.y
                    touchX = down.position.x

                    coroutineScope.launch {
                        waveProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.70f,
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                    }

                    if (availableLetters.isNotEmpty() && height > 0f) {
                        val itemHeight = height / availableLetters.size
                        val initialIdx = (touchY / itemHeight).toInt().coerceIn(0, availableLetters.size - 1)
                        if (selectedIndex != initialIdx) {
                            selectedIndex = initialIdx
                            selectedLetter = availableLetters[initialIdx]
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onLetterSelected(availableLetters[initialIdx])
                        }
                    }

                    // Continuously track dragging motion
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        if (event.type == PointerEventType.Release || event.type == PointerEventType.Unknown) {
                            change.consume()
                            break
                        }

                        if (change.isConsumed) {
                            // Let gesture proceed even if nested components attempt consume
                        }

                        change.consume()
                        touchY = change.position.y
                        touchX = change.position.x

                        if (availableLetters.isNotEmpty() && height > 0f) {
                            val itemHeight = height / availableLetters.size
                            val currentIdx = (touchY / itemHeight).toInt().coerceIn(0, availableLetters.size - 1)
                            if (selectedIndex != currentIdx) {
                                selectedIndex = currentIdx
                                selectedLetter = availableLetters[currentIdx]
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onLetterSelected(availableLetters[currentIdx])
                            }
                        }
                    }

                    // Drag finished: spring back cleanly
                    coroutineScope.launch {
                        waveProgress.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.50f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        selectedIndex = -1
                        selectedLetter = null
                        touchY = -1f
                        touchX = -1f
                    }
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val count = availableLetters.size
        if (count == 0 || canvasHeight <= 0f) return@Canvas

        val itemHeight = canvasHeight / count
        val baseX = canvasWidth - baseMarginRightPx
        val progress = waveProgress.value

        // Dynamic horizontal pull: expands arc inwards as finger drags leftwards
        val pullLeftDistance = if (touchX > 0f) (canvasWidth - touchX - baseMarginRightPx).coerceAtLeast(0f) else 0f
        val dynamicWaveArc = (baseWaveArcPx + pullLeftDistance * 0.40f).coerceIn(baseWaveArcPx, baseWaveArcPx * 1.35f)

        val native = drawContext.canvas.nativeCanvas

        // 1. Draw each letter along the Gaussian wave arc
        for (i in 0 until count) {
            val letter = availableLetters[i]
            val letterCenterY = (i + 0.5f) * itemHeight

            // Normalized distance for Niagara Gaussian curve
            val delta = if (touchY >= 0f) (letterCenterY - touchY) / canvasHeight else 10f
            val curvature = 0.26f // Niagara curvature constant
            val exponent = (1.0 / curvature) * (-4.0 * delta * delta)
            val gaussFraction = if (progress > 0f && exponent > -8.0) {
                2.0.pow(exponent).toFloat().coerceIn(0f, 1f)
            } else 0f

            val displacementX = gaussFraction * dynamicWaveArc * progress
            val drawX = baseX - displacementX

            // Scale font and alpha based on Gaussian intensity
            val scale = 1f + (gaussFraction * 0.45f * progress)
            val alpha = (0.75f + (gaussFraction * 0.25f)).coerceIn(0.6f, 1f)

            letterPaint.textSize = baseLetterTextSizePx * scale
            letterPaint.alpha = (alpha * 255).toInt()

            // Vertical centering using font metrics
            val textCenterY = letterCenterY - ((letterPaint.descent() + letterPaint.ascent()) / 2f)
            native.drawText(letter.toString(), drawX, textCenterY, letterPaint)
        }

        // 2. Draw Niagara Coral Handle (Selected Bubble) with Cubic Magnetic Snapping
        if (progress > 0.05f && selectedIndex in 0 until count && selectedLetter != null) {
            val letterCenterY = (selectedIndex + 0.5f) * itemHeight
            val relY = ((touchY - letterCenterY) / itemHeight).coerceIn(-0.5f, 0.5f)

            // Cubic polynomial magnetic snapping: keeps coral bubble anchored to letter center
            val snappedY = letterCenterY + (((relY * 2.0).pow(3.0) / 2.0).toFloat() * itemHeight)

            // Peak displacement at the selected letter
            val delta = (letterCenterY - touchY) / canvasHeight
            val exponent = (1.0 / 0.26f) * (-4.0 * delta * delta)
            val peakGauss = if (exponent > -8.0) 2.0.pow(exponent).toFloat().coerceIn(0f, 1f) else 0f
            val peakDisplacementX = peakGauss * dynamicWaveArc * progress

            // Bubble sits directly to the left of the wave crest
            val bubbleCenterX = baseX - (peakDisplacementX + bubbleRadiusPx + with(density) { 10.dp.toPx() })

            // Draw coral circular bubble
            bubblePaint.alpha = (progress * 255).toInt()
            native.drawCircle(bubbleCenterX, snappedY, bubbleRadiusPx, bubblePaint)

            // Draw white glyph inside coral bubble
            val isStar = selectedLetter == '☆'
            bubbleTextPaint.textSize = if (isStar) starBubbleTextSizePx else bubbleTextSizePx
            bubbleTextPaint.alpha = (progress * 255).toInt()
            val textCenterY = snappedY - ((bubbleTextPaint.descent() + bubbleTextPaint.ascent()) / 2f)
            native.drawText(selectedLetter.toString(), bubbleCenterX, textCenterY, bubbleTextPaint)
        }
    }
}
