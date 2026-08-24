package com.openzilla.app.util

import kotlin.math.roundToInt

/** Cuánto tarda una planta seca en recuperar el verde después de una recaída. */
const val RECOVERY_MILLIS = 3L * 24 * 3_600_000L

/**
 * What a habit's plant looks like right now.
 *
 * A relapse does not wipe the pot back to a seed. What you built stays there, dried out, and
 * comes back over the next few days as the new streak grows into the space it left. The point
 * is that falling once costs you the colour, not the plant.
 *
 * @param dryness 0f healthy, 1f completely dried out.
 */
data class PlantCondition(val stage: GrowthStage, val growth: Float, val dryness: Float)

/**
 * @param previousStreakMillis how long the streak that just broke lasted, or null if this
 *   habit has never been reset — nothing to wither in that case.
 */
fun plantCondition(
    startedAt: Long,
    previousStreakMillis: Long?,
    now: Long = System.currentTimeMillis()
): PlantCondition {
    val elapsed = (now - startedAt).coerceAtLeast(0)
    val current = growthStageFor(elapsed)
    val previous = previousStreakMillis?.let { growthStageFor(it) }

    // Si la planta anterior no era mayor que la de ahora no hay nada que marchitar: la racha
    // nueva ya ha alcanzado a la vieja.
    if (previous == null || previous.ordinal <= current.ordinal) {
        return PlantCondition(current, current.progressWithin(elapsed), 0f)
    }

    val recovery = (elapsed.toFloat() / RECOVERY_MILLIS).coerceIn(0f, 1f)
    // El tamaño baja poco a poco desde lo que había hasta lo que toca por la racha nueva, así
    // que al terminar de hidratarse ambos coinciden y no hay ningún salto.
    val steps = ((previous.ordinal - current.ordinal) * recovery).roundToInt()
    val shownOrdinal = (previous.ordinal - steps).coerceAtLeast(current.ordinal)
    val shown = GrowthStage.entries[shownOrdinal]

    return PlantCondition(
        stage = shown,
        growth = if (shown == current) current.progressWithin(elapsed) else 1f,
        dryness = 1f - recovery
    )
}

/**
 * The streak that was interrupted most recently, or null if the habit was never reset.
 * Taken from the history rows, so nothing extra needs storing.
 */
fun lastBrokenStreakMillis(pastStreaks: List<Pair<Long, Long>>): Long? =
    pastStreaks.maxByOrNull { it.second }?.let { (start, end) -> (end - start).coerceAtLeast(0) }
