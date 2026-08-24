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
import com.openzilla.app.util.CanopyShape
import com.openzilla.app.util.GrowthStage
import com.openzilla.app.util.LeafShape
import com.openzilla.app.util.PlantSpecies
import kotlin.math.cos
import kotlin.math.sin

/** Colours the garden draws with, all derived from the active theme and the species tint. */
data class PlantPalette(
    val foliage: Color,
    val foliageDark: Color,
    val bloom: Color,
    val trunk: Color,
    val pot: Color,
    val potRim: Color,
    val soil: Color
)

/**
 * Altura de la planta en cada etapa, en fracción del **espacio libre sobre la maceta** (no
 * del alto total). Los topes están puestos para que la copa más grande quepa entera: medido
 * sobre el alto total, un árbol se salía por arriba del recuadro.
 */
private val STAGE_HEIGHT = floatArrayOf(
    0.04f, 0.14f, 0.22f, 0.30f, 0.38f, 0.44f, 0.48f, 0.52f, 0.58f, 0.64f
)

/** Cuántas hojas lleva cada etapa antes de que aparezca la copa. */
private val STAGE_LEAVES = intArrayOf(0, 2, 4, 4, 6, 8, 8, 10, 10, 12)

/**
 * Draws one potted plant.
 *
 * Everything is plain geometry — no bitmaps, no vector assets — which is what keeps the whole
 * garden at zero bytes of APK and lets it recolour itself with the theme. Each plant is at
 * most a couple of dozen draw calls, and [sway] is read straight from the caller's animated
 * value so a frame of the breeze repaints only the canvases, never the layout.
 *
 * While the species is still a secret every plant is drawn the same generic way; the shape
 * only starts telling them apart once [GrowthStage.speciesRevealed] is true.
 *
 * @param growth 0f..1f inside the current stage, so a plant visibly creeps up before it jumps.
 * @param sway   -1f..1f, the shared breeze.
 */
fun DrawScope.drawPottedPlant(
    stage: GrowthStage,
    species: PlantSpecies,
    growth: Float,
    sway: Float,
    dryness: Float,
    palette: PlantPalette
) {
    val dry = dryness.coerceIn(0f, 1f)
    // La planta seca conserva su forma y pierde el color: es lo que hace que una recaída se
    // vea como un revés del que se vuelve, y no como haber empezado de cero.
    val shown = palette.copy(
        foliage = lerp(palette.foliage, DRY_LEAF, dry * 0.88f),
        foliageDark = lerp(palette.foliageDark, DRY_LEAF_DARK, dry * 0.88f)
    )
    val w = size.width
    val h = size.height
    val cx = w / 2f

    // --- Maceta: trapecio + borde, en la parte baja del dibujo ---
    val potTop = h * 0.72f
    val potBottom = h * 0.96f
    val pot = Path().apply {
        moveTo(cx - w * 0.20f, potTop)
        lineTo(cx + w * 0.20f, potTop)
        lineTo(cx + w * 0.14f, potBottom)
        lineTo(cx - w * 0.14f, potBottom)
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

    val base = potTop - rimHeight
    if (stage == GrowthStage.SEED) {
        drawCircle(shown.foliageDark, radius = w * 0.035f, center = Offset(cx, base - h * 0.012f))
        return
    }

    val revealed = stage.speciesRevealed
    val stageHeight = STAGE_HEIGHT[stage.ordinal]
    val nextHeight = STAGE_HEIGHT.getOrElse(stage.ordinal + 1) { stageHeight }
    // Se interpola hacia la etapa siguiente: entre hito e hito la planta sigue subiendo un
    // poco, en vez de quedarse idéntica y pegar un salto de golpe.
    val plantHeight = base * (stageHeight + (nextHeight - stageHeight) * growth * 0.35f)
    val topY = base - plantHeight
    val swayX = sway * w * 0.06f * stageHeight

    val hasCanopy = revealed && species.canopy != CanopyShape.NONE && stage >= GrowthStage.SAPLING
    val trunkWidth = when {
        hasCanopy && stage == GrowthStage.TREE -> w * 0.075f
        hasCanopy -> w * 0.055f
        stage >= GrowthStage.MATURE -> w * 0.032f
        else -> w * 0.022f
    }

    val stem = Path().apply {
        moveTo(cx, base)
        quadraticBezierTo(cx + swayX * 0.35f, base - plantHeight * 0.55f, cx + swayX, topY)
    }
    drawPath(
        stem,
        color = if (hasCanopy) shown.trunk else shown.foliageDark,
        style = Stroke(width = trunkWidth, cap = StrokeCap.Round)
    )

    val top = Offset(cx + swayX, topY)
    if (hasCanopy) {
        // La copa seca encoge un poco, además de perder el verde.
        drawCanopy(species, top, w * 0.22f * species.canopyScale * (1f - 0.14f * dry), shown)
    } else {
        val leafShape = if (revealed) species.leaf else LeafShape.OVAL
        val full = (STAGE_LEAVES[stage.ordinal] * if (revealed) species.leafiness else 1f)
        // Secándose se caen hojas, no sólo se apagan.
        val count = (full * (1f - 0.35f * dry)).toInt().coerceAtLeast(2)
        drawLeaves(leafShape, count, cx, base, plantHeight, swayX, w, dry, shown)
    }

    // Hojas caídas sobre la tierra cuando lleva un rato seca.
    if (dry > 0.35f) {
        repeat(3) { index ->
            val offset = (index - 1) * w * 0.11f
            drawOval(
                color = shown.foliageDark,
                topLeft = Offset(cx + offset - w * 0.045f, base - h * 0.004f),
                size = Size(w * 0.09f, h * 0.016f)
            )
        }
    }

    // --- Capullos y flores ---
    if (revealed && species.flowers && stage >= GrowthStage.BUDDING && dry < 0.5f) {
        val bloomed = stage >= GrowthStage.FLOWERING
        val radius = w * (if (bloomed) 0.045f else 0.028f)
        val spots = if (bloomed) 5 else 3
        repeat(spots) { index ->
            val t = 0.45f + 0.5f * index / spots
            val y = base - plantHeight * t
            val x = cx + swayX * t + (if (index % 2 == 0) 1f else -1f) * w * 0.12f
            if (bloomed) drawFlower(Offset(x, y), radius, shown) else drawCircle(shown.bloom, radius, Offset(x, y))
        }
    }
}

private fun DrawScope.drawLeaves(
    shape: LeafShape,
    count: Int,
    cx: Float,
    base: Float,
    plantHeight: Float,
    swayX: Float,
    w: Float,
    dry: Float,
    palette: PlantPalette
) {
    val length = when (shape) {
        LeafShape.OVAL -> w * 0.17f
        LeafShape.ROUND -> w * 0.13f
        LeafShape.NEEDLE -> w * 0.20f
    }
    val thickness = when (shape) {
        LeafShape.OVAL -> 0.52f
        LeafShape.ROUND -> 0.95f
        LeafShape.NEEDLE -> 0.22f
    }
    repeat(count) { index ->
        val t = (index + 1f) / (count + 1f)
        val y = base - plantHeight * t
        val x = cx + swayX * t
        val toRight = index % 2 == 0
        // Con la planta seca las hojas van cayendo hasta apuntar hacia abajo.
        val angle = if (toRight) -35f + 70f * dry else 35f - 70f * dry
        rotate(degrees = angle, pivot = Offset(x, y)) {
            drawOval(
                color = if (toRight) palette.foliage else palette.foliageDark,
                topLeft = Offset(if (toRight) x else x - length, y - length * thickness / 2f),
                size = Size(length, length * thickness)
            )
        }
    }
}

private fun DrawScope.drawCanopy(species: PlantSpecies, top: Offset, radius: Float, palette: PlantPalette) {
    when (species.canopy) {
        CanopyShape.CONE -> {
            // Pino: tres triángulos apilados, el de abajo más ancho.
            repeat(3) { level ->
                val scale = 1f - level * 0.22f
                val halfWidth = radius * 1.15f * scale
                val height = radius * 1.1f
                val centerY = top.y + radius * 0.75f - level * height * 0.55f
                val cone = Path().apply {
                    moveTo(top.x, centerY - height)
                    lineTo(top.x + halfWidth, centerY)
                    lineTo(top.x - halfWidth, centerY)
                    close()
                }
                drawPath(cone, if (level % 2 == 0) palette.foliageDark else palette.foliage)
            }
        }

        CanopyShape.FAN -> {
            // Palmera: hojas largas saliendo en abanico desde la punta del tronco.
            repeat(6) { index ->
                // Todas hacia arriba: con angulos positivos apuntaban hacia abajo y la copa
                // parecia un asterisco en vez de una palmera.
                val angle = Math.toRadians((-165.0 + index * 30.0)).toFloat()
                val length = radius * 1.5f
                val end = Offset(top.x + cos(angle) * length, top.y + sin(angle) * length * 0.7f)
                drawLine(
                    color = if (index % 2 == 0) palette.foliage else palette.foliageDark,
                    start = top,
                    end = end,
                    strokeWidth = radius * 0.34f,
                    cap = StrokeCap.Round
                )
            }
        }

        else -> {
            // Copa redonda: círculos solapados, lo más barato que sigue leyéndose como copa.
            drawCircle(palette.foliageDark, radius, Offset(top.x - radius * 0.62f, top.y + radius * 0.45f))
            drawCircle(palette.foliageDark, radius, Offset(top.x + radius * 0.62f, top.y + radius * 0.45f))
            drawCircle(palette.foliage, radius * 1.1f, Offset(top.x, top.y + radius * 0.15f))
            drawCircle(palette.foliage, radius * 0.75f, Offset(top.x - radius * 0.9f, top.y - radius * 0.15f))
            drawCircle(palette.foliage, radius * 0.75f, Offset(top.x + radius * 0.9f, top.y - radius * 0.15f))
        }
    }
}

/** Cinco pétalos y un centro: suficiente para que se lea como flor a este tamaño. */
private fun DrawScope.drawFlower(center: Offset, radius: Float, palette: PlantPalette) {
    repeat(5) { index ->
        val angle = (Math.PI * 2 * index / 5).toFloat()
        val petal = Offset(center.x + cos(angle) * radius * 0.7f, center.y + sin(angle) * radius * 0.7f)
        drawCircle(palette.bloom, radius * 0.55f, petal)
    }
    drawCircle(lerp(palette.bloom, Color.White, 0.55f), radius * 0.4f, center)
}

/** Breeze offset for one plant: same wave for everyone, desfasada por su posición. */
fun swayFor(phase: Float, index: Int): Float = sin(phase + index * 0.7f)

/** Verde genérico mientras la especie sigue siendo un secreto. */
private val UNKNOWN_TINT = Color(0xFF4C8B3F)

/** Tonos de hoja seca. No salen del tema: una hoja marchita es marrón en cualquier paleta. */
private val DRY_LEAF = Color(0xFFB08A46)
private val DRY_LEAF_DARK = Color(0xFF7A5B32)

/** Marrón de tronco y tierra; no se saca del tema porque un tronco gris no lee como tronco. */
private val WOOD = Color(0xFF6B4A2F)

/**
 * Cuánto del acento entra en la hoja.
 *
 * Poco, y por un motivo concreto: mezclar a partes iguales un verde y un acento cálido no da
 * un verde cálido, da un marrón sucio — con el coral por defecto el jardín entero salía
 * embarrado. Con este peso la hoja sigue siendo verde y el acento se nota; donde sí manda es
 * en la maceta y en las flores, que aceptan cualquier color sin dejar de leerse.
 */
private const val ACCENT_IN_FOLIAGE = 0.18f

fun plantPalette(
    species: PlantSpecies,
    revealed: Boolean,
    darkTheme: Boolean,
    primary: Color,
    surfaceVariant: Color,
    outline: Color
): PlantPalette {
    val green = if (revealed) Color(species.tint) else UNKNOWN_TINT
    val tinted = lerp(green, primary, ACCENT_IN_FOLIAGE)
    // En oscuro los verdes se apagan contra el fondo negro, así que suben un poco.
    val foliage = if (darkTheme) lerp(tinted, Color.White, 0.14f) else tinted
    return PlantPalette(
        foliage = foliage,
        foliageDark = lerp(foliage, Color.Black, 0.26f),
        bloom = lerp(Color(species.bloomTint), primary, 0.35f),
        trunk = lerp(WOOD, primary, 0.12f),
        pot = lerp(outline, primary, 0.25f),
        potRim = lerp(outline, primary, 0.12f),
        soil = lerp(surfaceVariant, Color.Black, 0.42f)
    )
}
