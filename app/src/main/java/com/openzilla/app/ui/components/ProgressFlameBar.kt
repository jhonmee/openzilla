package com.openzilla.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.openzilla.app.ui.theme.FlameCore
import com.openzilla.app.ui.theme.FlameMid

/**
 * Straight progress bar with a flame burning at the leading edge — replaces the old circular
 * gauge.
 *
 * Everything is one Canvas: a track, a fill and a handful of shapes for the flame. The
 * flicker is a single animated float that is only ever read *inside* the draw lambda, so an
 * animation frame repaints this one node and never re-runs composition or layout. Passing
 * [animated] = false (used by the habit list) removes the animation altogether, leaving a
 * completely static drawing.
 */
@Composable
fun ProgressFlameBar(
    progress: Float,
    modifier: Modifier = Modifier,
    barHeight: Dp = 14.dp,
    animated: Boolean = true
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val active = MaterialTheme.colorScheme.primary

    // `animated` is fixed per call site, so this conditional never changes shape between
    // recompositions — and when it is false no animation is created or scheduled at all.
    val flicker: State<Float>? = if (animated) {
        val transition = rememberInfiniteTransition(label = "llama")
        transition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.14f,
            animationSpec = infiniteRepeatable(tween(720, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "parpadeo"
        )
    } else {
        null
    }

    // Los dos Path se crean una sola vez y se reutilizan en cada frame: dibujar la llama no
    // genera basura para el recolector aunque esté animándose.
    val flamePath = remember { Path() }
    val corePath = remember { Path() }

    Canvas(modifier = modifier.fillMaxWidth().height(barHeight * 3)) {
        val flame = flicker?.value ?: 1f
        val clamped = progress.coerceIn(0f, 1f)
        val h = barHeight.toPx()
        val radius = h / 2f
        val centerY = size.height / 2f
        val top = centerY - radius
        val fullWidth = size.width

        drawRoundRect(
            color = track,
            topLeft = Offset(0f, top),
            size = Size(fullWidth, h),
            cornerRadius = CornerRadius(radius, radius)
        )

        val fillWidth = fullWidth * clamped
        if (fillWidth > 0.5f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(active.copy(alpha = 0.75f), active),
                    startX = 0f,
                    endX = fullWidth.coerceAtLeast(1f)
                ),
                topLeft = Offset(0f, top),
                size = Size(fillWidth, h),
                cornerRadius = CornerRadius(radius, radius)
            )
        }

        // La llama vive en la punta de lo ya recorrido, sin salirse nunca de la barra.
        val tipX = fillWidth.coerceIn(radius, fullWidth - radius)

        // Halo: un degradado radial suave que tiñe la barra alrededor de la punta.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(FlameMid.copy(alpha = 0.45f * flame), Color.Transparent),
                center = Offset(tipX, centerY),
                radius = h * 1.9f * flame
            ),
            radius = h * 1.9f * flame,
            center = Offset(tipX, centerY)
        )

        // Lengua de fuego hacia arriba.
        val flameHeight = h * 1.7f * flame
        val flameWidth = h * 0.85f
        flamePath.apply {
            reset()
            moveTo(tipX, centerY - flameHeight)
            cubicTo(
                tipX + flameWidth, centerY - flameHeight * 0.45f,
                tipX + flameWidth * 0.8f, centerY + radius * 0.6f,
                tipX, centerY + radius * 0.7f
            )
            cubicTo(
                tipX - flameWidth * 0.8f, centerY + radius * 0.6f,
                tipX - flameWidth, centerY - flameHeight * 0.45f,
                tipX, centerY - flameHeight
            )
            close()
        }
        drawPath(
            path = flamePath,
            brush = Brush.verticalGradient(
                colors = listOf(FlameMid.copy(alpha = 0.95f), active),
                startY = centerY - flameHeight,
                endY = centerY + radius
            )
        )

        // Núcleo claro, más pequeño y desfasado, que es lo que da sensación de fuego vivo.
        val coreHeight = flameHeight * 0.55f
        val coreWidth = flameWidth * 0.45f
        corePath.apply {
            reset()
            moveTo(tipX, centerY - coreHeight)
            cubicTo(
                tipX + coreWidth, centerY - coreHeight * 0.4f,
                tipX + coreWidth * 0.8f, centerY + radius * 0.3f,
                tipX, centerY + radius * 0.4f
            )
            cubicTo(
                tipX - coreWidth * 0.8f, centerY + radius * 0.3f,
                tipX - coreWidth, centerY - coreHeight * 0.4f,
                tipX, centerY - coreHeight
            )
            close()
        }
        drawPath(
            path = corePath,
            brush = Brush.verticalGradient(
                colors = listOf(FlameCore, FlameMid),
                startY = centerY - coreHeight,
                endY = centerY + radius
            )
        )
    }
}
