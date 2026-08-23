package com.openzilla.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A simple open-bottom progress ring, echoing the reference app's "24 horas" gauge — drawn with plain Canvas, no chart library. */
@Composable
fun GaugeRing(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    ringSize: Dp = 180.dp
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val active = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(ringSize)) {
            // Inside this DrawScope lambda, `size` refers to the canvas's own
            // geometry.Size — deliberately not shadowed by a same-named parameter here.
            val strokeWidth = size.minDimension * 0.09f
            val sweepTotal = 300f
            val startAngle = 120f
            drawArc(
                color = track,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = active,
                startAngle = startAngle,
                sweepAngle = sweepTotal * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
