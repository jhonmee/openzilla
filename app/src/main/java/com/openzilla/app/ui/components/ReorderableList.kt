package com.openzilla.app.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/** Cuánto se agranda el elemento levantado. */
private const val LIFT_SCALE = 1.03f

/** Franja junto a cada borde donde la lista empieza a desplazarse sola. */
private val AUTO_SCROLL_ZONE = 72.dp

/** Desplazamiento máximo por fotograma durante el autoscroll. */
private val AUTO_SCROLL_MAX_STEP = 14.dp

/**
 * Drag-and-drop reordering for a `LazyColumn`, small enough to keep in the project rather
 * than adding a dependency for it.
 *
 * The trick that keeps it accurate while the list scrolls under the finger: the dragged item
 * is tracked by its centre **in viewport coordinates** ([floatingCenterY]), a value only the
 * finger moves. Every other position is read fresh from the list's own layout, so scrolling
 * shifts the neighbours while the dragged card stays exactly where the finger is holding it.
 */
class ReorderState internal constructor(
    private val listState: LazyListState,
    private val autoScrollZonePx: Float,
    private val autoScrollMaxStepPx: Float,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onDropped: () -> Unit,
    private val onLifted: () -> Unit,
    private val onSwapped: () -> Unit
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set

    private var draggingIndex by mutableIntStateOf(-1)
    private var floatingCenterY by mutableFloatStateOf(0f)
    private var draggingHeight by mutableIntStateOf(0)

    /**
     * A tap is delivered on the same finger-up that ends a drag, so a plain click guard is
     * not enough: clicks are also ignored for a moment right after dropping.
     */
    private var lastDropAt = 0L
    fun shouldIgnoreClick(): Boolean =
        draggingKey != null || System.currentTimeMillis() - lastDropAt < 300L

    internal fun start(key: Any) {
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        draggingKey = key
        draggingIndex = info.index
        draggingHeight = info.size
        floatingCenterY = info.offset + info.size / 2f
        onLifted()
    }

    internal fun drag(deltaY: Float) {
        if (draggingKey == null) return
        floatingCenterY += deltaY
        swapIfNeeded()
    }

    internal fun stop() {
        if (draggingKey == null) return
        draggingKey = null
        draggingIndex = -1
        lastDropAt = System.currentTimeMillis()
        onDropped()
    }

    /** Vertical shift to draw [index] at, in pixels. Only the dragged item ever moves. */
    internal fun offsetFor(index: Int): Float {
        if (index != draggingIndex) return 0f
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return 0f
        return floatingCenterY - (info.offset + info.size / 2f)
    }

    private fun swapIfNeeded() {
        val from = draggingIndex
        if (from < 0) return
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.index != from && floatingCenterY >= item.offset && floatingCenterY <= item.offset + item.size
        } ?: return
        onMove(from, target.index)
        draggingIndex = target.index
        onSwapped()
    }

    /**
     * One scroll step for the current frame, or 0 when the card is comfortably inside the
     * viewport. Speed grows with how far into the edge zone the card has been pushed, which
     * makes short corrections easy and long journeys quick.
     */
    private fun autoScrollStep(): Float {
        if (draggingKey == null) return 0f
        val info = listState.layoutInfo
        val top = floatingCenterY - draggingHeight / 2f
        val bottom = floatingCenterY + draggingHeight / 2f
        val start = info.viewportStartOffset.toFloat()
        val end = info.viewportEndOffset.toFloat()
        return when {
            bottom > end - autoScrollZonePx ->
                (((bottom - (end - autoScrollZonePx)) / autoScrollZonePx).coerceAtMost(1f)) * autoScrollMaxStepPx
            top < start + autoScrollZonePx ->
                -(((start + autoScrollZonePx - top) / autoScrollZonePx).coerceAtMost(1f)) * autoScrollMaxStepPx
            else -> 0f
        }
    }

    internal suspend fun runAutoScroll() {
        while (draggingKey != null) {
            val step = autoScrollStep()
            if (step != 0f) {
                // scrollBy devuelve lo que realmente se pudo desplazar: al llegar al final de
                // la lista devuelve 0 y el bucle se queda quieto en vez de forcejear.
                listState.scrollBy(step)
                swapIfNeeded()
            }
            withFrameNanos { }
        }
    }
}

@Composable
fun rememberReorderState(
    listState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit,
    onDropped: () -> Unit,
    onLifted: () -> Unit = {},
    onSwapped: () -> Unit = {}
): ReorderState {
    val move by rememberUpdatedState(onMove)
    val dropped by rememberUpdatedState(onDropped)
    val lifted by rememberUpdatedState(onLifted)
    val swapped by rememberUpdatedState(onSwapped)

    val density = LocalDensity.current
    val zonePx = with(density) { AUTO_SCROLL_ZONE.toPx() }
    val stepPx = with(density) { AUTO_SCROLL_MAX_STEP.toPx() }

    val state = remember(listState, zonePx, stepPx) {
        ReorderState(
            listState = listState,
            autoScrollZonePx = zonePx,
            autoScrollMaxStepPx = stepPx,
            onMove = { from, to -> move(from, to) },
            onDropped = { dropped() },
            onLifted = { lifted() },
            onSwapped = { swapped() }
        )
    }

    // El autoscroll sólo existe mientras hay algo agarrado; al soltar, la corrutina termina.
    LaunchedEffect(state.draggingKey) {
        if (state.draggingKey != null) state.runAutoScroll()
    }
    return state
}

/**
 * Applies to each row: the long-press gesture that picks it up, the offset that follows the
 * finger, and the small lift while it is being carried.
 */
fun Modifier.reorderableItem(state: ReorderState, key: Any, index: Int): Modifier {
    val dragging = state.draggingKey == key
    return this
        // El gesto va antes que graphicsLayer a propósito: dentro de la capa, el pequeño
        // escalado del "levantar" deformaría los desplazamientos y el arrastre se desviaría.
        .pointerInput(key) {
            detectDragGesturesAfterLongPress(
                onDragStart = { state.start(key) },
                onDrag = { change, amount ->
                    change.consume()
                    state.drag(amount.y)
                },
                onDragEnd = { state.stop() },
                onDragCancel = { state.stop() }
            )
        }
        .zIndex(if (dragging) 1f else 0f)
        .graphicsLayer {
            // Leer el desplazamiento aquí (y no en la composición) hace que arrastrar sólo
            // repinte esta tarjeta: no se recompone ni se vuelve a medir nada.
            translationY = state.offsetFor(index)
            val scale = if (dragging) LIFT_SCALE else 1f
            scaleX = scale
            scaleY = scale
        }
}
