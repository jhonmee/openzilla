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
import kotlin.math.abs

/** Cuánto se agranda el elemento levantado. */
private const val LIFT_SCALE = 1.03f

/** Franja junto a cada borde donde la lista empieza a desplazarse sola. */
private val AUTO_SCROLL_ZONE = 56.dp

/** Velocidad máxima del autoscroll, en dp por segundo. */
private val AUTO_SCROLL_MAX_SPEED = 320.dp

/** Hay que arrastrar al menos esto para que el autoscroll entre en juego. */
private val AUTO_SCROLL_ACTIVATION = 8.dp

/**
 * Tiempo mínimo entre dos intercambios.
 *
 * Los vecinos animan su hueco al apartarse, así que mientras dura esa animación la lista
 * informa de posiciones intermedias. Sin esta pausa, esas posiciones a medio camino
 * disparaban el intercambio siguiente, ese volvía a mover a los vecinos, y salía una cascada
 * hasta el final de la lista o un temblor cuando dos intercambios se peleaban entre sí.
 */
private const val SWAP_COOLDOWN_MILLIS = 110L

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
    private val autoScrollMaxSpeedPx: Float,
    private val autoScrollActivationPx: Float,
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
    private var draggedDistance = 0f
    private var lastSwapAt = 0L

    /**
     * A tap is delivered on the same finger-up that ends a drag, so a plain click guard is
     * not enough: clicks are also ignored for a moment right after dropping.
     */
    private var lastDropAt = 0L
    fun shouldIgnoreClick(): Boolean =
        draggingKey != null || System.currentTimeMillis() - lastDropAt < 300L

    /**
     * Where the items actually live, in the same coordinates as `LazyListItemInfo.offset`.
     *
     * Not the raw viewport values: those include the list's content padding, so measuring the
     * edge zones against them left the top zone above the first card. The list then refused
     * to scroll up while the card sat pinned against the top.
     */
    private val contentStart: Float
        get() = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.beforeContentPadding).toFloat()

    private val contentEnd: Float
        get() = (listState.layoutInfo.viewportEndOffset - listState.layoutInfo.afterContentPadding).toFloat()

    internal fun start(key: Any) {
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        draggingKey = key
        draggingIndex = info.index
        draggingHeight = info.size
        floatingCenterY = info.offset + info.size / 2f
        draggedDistance = 0f
        lastSwapAt = 0L
        onLifted()
    }

    internal fun drag(deltaY: Float) {
        if (draggingKey == null) return
        draggedDistance += abs(deltaY)
        val half = draggingHeight / 2f
        val lowest = contentStart + half
        val highest = contentEnd - half
        val moved = floatingCenterY + deltaY
        // El tope evita acumular recorrido invisible fuera de la pantalla: sin él, arrastrar
        // más allá del borde y volver no reaccionaba hasta recuperar todo lo acumulado.
        floatingCenterY = if (lowest <= highest) moved.coerceIn(lowest, highest) else moved
        swapIfNeeded()
    }

    internal fun stop() {
        if (draggingKey == null) return
        draggingKey = null
        draggingIndex = -1
        draggedDistance = 0f
        lastDropAt = System.currentTimeMillis()
        onDropped()
    }

    /** Vertical shift to draw [index] at, in pixels. Only the dragged item ever moves. */
    internal fun offsetFor(index: Int): Float {
        if (index != draggingIndex) return 0f
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return 0f
        return floatingCenterY - (info.offset + info.size / 2f)
    }

    /**
     * Moves the card at most one position, and only once the finger has gone past the middle
     * of the neighbour it is displacing.
     *
     * Both limits matter. One neighbour at a time keeps the movement monotonic — it cannot
     * skip half the list in a single frame — and the halfway rule gives it hysteresis: right
     * after a swap that neighbour sits on the other side, so the condition that triggered it
     * is no longer true and it cannot bounce straight back.
     */
    private fun swapIfNeeded() {
        val from = draggingIndex
        if (from < 0) return
        val now = System.currentTimeMillis()
        if (now - lastSwapAt < SWAP_COOLDOWN_MILLIS) return

        val items = listState.layoutInfo.visibleItemsInfo
        val next = items.firstOrNull { it.index == from + 1 }
        val previous = items.firstOrNull { it.index == from - 1 }
        val target = when {
            next != null && floatingCenterY > next.offset + next.size / 2f -> from + 1
            previous != null && floatingCenterY < previous.offset + previous.size / 2f -> from - 1
            else -> return
        }

        lastSwapAt = now
        onMove(from, target)
        draggingIndex = target
        onSwapped()
    }

    /**
     * Scroll speed in pixels per second, or 0 when there is no reason to scroll: the card is
     * comfortably inside the viewport, the finger has barely moved, or the list has nothing
     * left to give in that direction.
     */
    private fun autoScrollVelocity(): Float {
        if (draggingKey == null) return 0f
        // Levantar una tarjeta que ya estaba pegada a un borde no debe ponerse a desplazar
        // sola: hace falta que el dedo se haya movido de verdad.
        if (draggedDistance < autoScrollActivationPx) return 0f

        val half = draggingHeight / 2f
        val top = floatingCenterY - half
        val bottom = floatingCenterY + half
        val start = contentStart
        val end = contentEnd
        return when {
            // canScrollForward/Backward evitan seguir empujando contra un extremo, que era de
            // donde salía el temblor al llegar arriba del todo.
            bottom > end - autoScrollZonePx && listState.canScrollForward ->
                ((bottom - (end - autoScrollZonePx)) / autoScrollZonePx).coerceAtMost(1f) * autoScrollMaxSpeedPx
            top < start + autoScrollZonePx && listState.canScrollBackward ->
                -((start + autoScrollZonePx - top) / autoScrollZonePx).coerceAtMost(1f) * autoScrollMaxSpeedPx
            else -> 0f
        }
    }

    internal suspend fun runAutoScroll() {
        var previousFrame = 0L
        while (draggingKey != null) {
            var step = 0f
            withFrameNanos { frame ->
                if (previousFrame != 0L) {
                    // Por segundo y no por fotograma: medido por fotograma, la lista corría al
                    // doble en un móvil de 120 Hz. Se recorta el intervalo para que un atasco
                    // puntual no dé un tirón al recuperarse.
                    val seconds = ((frame - previousFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    step = autoScrollVelocity() * seconds
                }
                previousFrame = frame
            }
            if (step != 0f) {
                listState.scrollBy(step)
                swapIfNeeded()
            }
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
    val speedPx = with(density) { AUTO_SCROLL_MAX_SPEED.toPx() }
    val activationPx = with(density) { AUTO_SCROLL_ACTIVATION.toPx() }

    val state = remember(listState, zonePx, speedPx, activationPx) {
        ReorderState(
            listState = listState,
            autoScrollZonePx = zonePx,
            autoScrollMaxSpeedPx = speedPx,
            autoScrollActivationPx = activationPx,
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
