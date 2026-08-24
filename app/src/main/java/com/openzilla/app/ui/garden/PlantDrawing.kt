package com.openzilla.app.ui.garden

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import com.openzilla.app.util.GrowthStage
import kotlin.math.sin

/** Colours the garden draws with, all taken from the active theme so the accent carries through. */
data class PlantPalette(
    val foliage: Color,
    val foliageDark: Color,
    val trunk: Color,
    val pot: Color,
    val potRim: Color,
    val soil: Color
)

/**
 * Altura de la planta para cada etapa, en fracción del **espacio libre sobre la maceta** (no
 * del alto total). Los topes están puestos para que la copa más grande quepa entera: medido
 * sobre el alto total, un árbol se salía por arriba del recuadro.
 */
private val STAGE_HEIGHT = floatArrayOf(0.04f, 0.16f, 0.28f, 0.40f, 0.34f, 0.52f, 0.62f)

/**
 * Draws one potted plant.
 *
 * Everything is plain geometry — no bitmaps, no vector assets — which is what keeps the whole
 * garden at zero bytes of APK and lets it recolour itself with the theme. Each plant is at
 * most a dozen draw calls, and [sway] is read straight from the caller's animated value so a
 * frame of the breeze repaints only the canvases, never the layout.
 *
 * @param growth 0f..1f inside the current stage, so a plant visibly creeps up before it jumps.
 * @param sway   -1f..1f, the shared breeze.
 */
fun DrawScope.drawPottedPlant(
    stage: GrowthStage,
    growth: Float,
    sway: Float,
    palette: PlantPalette
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f

    // --- Maceta: trapecio + borde, en la parte baja del dibujo ---
    val potTop = h * 0.72f
    val potBottom = h * 0.96f
    val potTopHalf = w * 0.20f
    val potBottomHalf = w * 0.14f
    val pot = Path().apply {
        moveTo(cx - potTopHalf, potTop)
        lineTo(cx + potTopHalf, potTop)
        lineTo(cx + potBottomHalf, potBottom)
        lineTo(cx - potBottomHalf, potBottom)
        close()
    }
    drawPath(pot, palette.pot)

    val rimHeight = h * 0.055f
    drawRect(
        color = palette.potRim,
        topLeft = Offset(cx - w * 0.235f, potTop - rimHeight),
        size = Size(w * 0.47f, rimHeight)
    )
    drawOval(
        color = palette.soil,
        topLeft = Offset(cx - w * 0.20f, potTop - rimHeight - h * 0.018f),
        size = Size(w * 0.40f, h * 0.036f)
    )

    // --- Planta ---
    val base = potTop - rimHeight
    val stageHeight = STAGE_HEIGHT[stage.ordinal]
    val nextHeight = STAGE_HEIGHT.getOrElse(stage.ordinal + 1) { stageHeight }
    // Se interpola hacia la etapa siguiente: entre hito e hito la planta sigue subiendo un
    // poco, en vez de quedarse idéntica y pegar un salto de golpe.
    val plantHeight = base * (stageHeight + (nextHeight - stageHeight) * growth * 0.35f)
    val topY = base - plantHeight
    val swayX = sway * w * 0.06f * stageHeight

    if (stage == GrowthStage.SEED) {
        drawCircle(palette.foliageDark, radius = w * 0.035f, center = Offset(cx, base - h * 0.012f))
        return
    }

    val trunkWidth = when (stage) {
        GrowthStage.SPROUT, GrowthStage.SEEDLING -> w * 0.022f
        GrowthStage.PLANT, GrowthStage.BUSH -> w * 0.032f
        GrowthStage.SAPLING -> w * 0.055f
        else -> w * 0.075f
    }
    val stem = Path().apply {
        moveTo(cx, base)
        quadraticBezierTo(cx + swayX * 0.35f, base - plantHeight * 0.55f, cx + swayX, topY)
    }
    drawPath(
        stem,
        color = if (stage >= GrowthStage.SAPLING) palette.trunk else palette.foliageDark,
        style = Stroke(width = trunkWidth, cap = StrokeCap.Round)
    )

    when (stage) {
        GrowthStage.SPROUT, GrowthStage.SEEDLING, GrowthStage.PLANT -> {
            val leaves = when (stage) {
                GrowthStage.SPROUT -> 2
                GrowthStage.SEEDLING -> 4
                else -> 6
            }
            val leafLength = w * (0.13f + 0.025f * stage.ordinal)
            repeat(leaves) { index ->
                val t = (index + 1f) / (leaves + 1f)
                val y = base - plantHeight * t
                val x = cx + swayX * t
                val toRight = index % 2 == 0
                rotate(degrees = if (toRight) -35f else 35f, pivot = Offset(x, y)) {
                    drawOval(
                        color = if (index % 2 == 0) palette.foliage else palette.foliageDark,
                        topLeft = Offset(if (toRight) x else x - leafLength, y - leafLength * 0.26f),
                        size = Size(leafLength, leafLength * 0.52f)
                    )
                }
            }
        }

        GrowthStage.BUSH, GrowthStage.SAPLING, GrowthStage.TREE -> {
            val canopy = when (stage) {
                GrowthStage.BUSH -> w * 0.17f
                GrowthStage.SAPLING -> w * 0.19f
                else -> w * 0.24f
            }
            val top = Offset(cx + swayX, topY)
            // Tres o cinco círculos solapados: es lo más barato que sigue leyéndose como copa.
            drawCircle(palette.foliageDark, canopy, Offset(top.x - canopy * 0.62f, top.y + canopy * 0.45f))
            drawCircle(palette.foliageDark, canopy, Offset(top.x + canopy * 0.62f, top.y + canopy * 0.45f))
            drawCircle(palette.foliage, canopy * 1.1f, Offset(top.x, top.y + canopy * 0.15f))
            if (stage == GrowthStage.TREE) {
                drawCircle(palette.foliage, canopy * 0.75f, Offset(top.x - canopy * 0.9f, top.y - canopy * 0.15f))
                drawCircle(palette.foliage, canopy * 0.75f, Offset(top.x + canopy * 0.9f, top.y - canopy * 0.15f))
            }
        }

        GrowthStage.SEED -> Unit
    }
}

/** Breeze offset for one plant: same wave for everyone, desfasada por su posición. */
fun swayFor(phase: Float, index: Int): Float = sin(phase + index * 0.7f)

/** Builds the palette from two theme colours, so light and dark both stay readable. */
fun plantPalette(primary: Color, surfaceVariant: Color, outline: Color, onSurfaceVariant: Color) = PlantPalette(
    foliage = primary,
    foliageDark = lerp(primary, Color.Black, 0.28f),
    trunk = lerp(onSurfaceVariant, Color.Black, 0.25f),
    pot = lerp(outline, Color.Black, 0.10f),
    potRim = outline,
    soil = lerp(surfaceVariant, Color.Black, 0.35f)
)
