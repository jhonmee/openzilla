package com.openzilla.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Cuánto tarda la onda en recorrer la barra entera. */
private const val WAVE_SWEEP_MILLIS = 2600

/** Opacidad del punto más claro de la onda. Muy baja a propósito: debe insinuarse, no brillar. */
private const val WAVE_ALPHA = 0.28f

/**
 * Straight progress bar with a soft wave travelling along the filled section.
 *
 * The whole thing is three `drawRoundRect` calls: track, fill, and one translucent band whose
 * gradient window slides across the fill. The band is a gradient with transparent ends rather
 * than a clipped shape, so it fades in and out on its own and never spills past the bar's
 * rounded corners.
 *
 * The animated value is read *inside* the draw lambda, so a frame of the wave repaints this
 * one node without re-running composition or layout. With [animated] = false (the habit list)
 * no animation is created at all and the bar is a completely static drawing.
 */
@Composable
fun ProgressWaveBar(
    progress: Float,
    modifier: Modifier = Modifier,
    barHeight: Dp = 14.dp,
    animated: Boolean = true
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val active = MaterialTheme.colorScheme.primary

    // `animated` es fijo en cada sitio donde se usa, así que esta condición nunca cambia de
    // forma entre recomposiciones — y cuando es false no se programa ninguna animación.
    val phase: State<Float>? = if (animated) {
        val transition = rememberInfiniteTransition(label = "onda")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(WAVE_SWEEP_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "avance-onda"
        )
    } else {
        null
    }

    Canvas(modifier = modifier.fillMaxWidth().height(barHeight)) {
        val clamped = progress.coerceIn(0f, 1f)
        val h = size.height
        val radius = h / 2f
        val fullWidth = size.width
        val corner = CornerRadius(radius, radius)

        drawRoundRect(color = track, size = Size(fullWidth, h), cornerRadius = corner)

        val fillWidth = fullWidth * clamped
        if (fillWidth <= 0.5f) return@Canvas

        drawRoundRect(
            color = active,
            size = Size(fillWidth, h),
            cornerRadius = corner
        )

        val travel = phase?.value ?: return@Canvas
        // La banda entra por la izquierda y sale por la derecha; fuera de su ventana el
        // degradado se queda en transparente, así que no hace falta recortar nada.
        val bandWidth = (fullWidth * 0.22f).coerceAtLeast(h * 3f)
        val bandStart = -bandWidth + travel * (fillWidth + bandWidth)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = WAVE_ALPHA), Color.Transparent),
                startX = bandStart,
                endX = bandStart + bandWidth
            ),
            topLeft = Offset.Zero,
            size = Size(fillWidth, h),
            cornerRadius = corner
        )
    }
}
