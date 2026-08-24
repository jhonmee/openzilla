package com.openzilla.app.util

import kotlin.math.roundToInt

/**
 * Cuánto de la racha perdida tarda la planta en recuperarse.
 *
 * Proporcional a propósito: perder dos días y perder tres meses no deberían costar lo mismo.
 * Los topes evitan los dos extremos absurdos — que una racha de una hora deje la planta seca
 * un minuto, o que perder un año la deje marrón medio año.
 */
private const val RECOVERY_FRACTION = 0.08
private const val MIN_RECOVERY_MILLIS = 12L * 3_600_000L
private const val MAX_RECOVERY_MILLIS = 10L * 24 * 3_600_000L

/** Cuánto adelanta cada riego, en fracción del total de la recuperación. */
private const val WATERING_BOOST = 0.15

fun recoveryDurationFor(lostStreakMillis: Long): Long =
    (lostStreakMillis.coerceAtLeast(0) * RECOVERY_FRACTION).toLong()
        .coerceIn(MIN_RECOVERY_MILLIS, MAX_RECOVERY_MILLIS)

/** Cuánto tiempo de recuperación adelanta un riego de esta planta. */
fun wateringBoostFor(lostStreakMillis: Long): Long =
    (recoveryDurationFor(lostStreakMillis) * WATERING_BOOST).toLong()

/** Sólo se puede regar una vez al día; regar diez veces seguidas no revive nada. */
fun canWaterToday(lastWateredAt: Long, now: Long = System.currentTimeMillis()): Boolean =
    lastWateredAt <= 0L || dayStartOf(lastWateredAt) < dayStartOf(now)

/**
 * What a habit's plant looks like right now.
 *
 * A relapse does not wipe the pot back to a seed. What you built stays there, dried out, and
 * comes back over the next few days as the new streak grows into the space it left. The point
 * is that falling once costs you the colour, not the plant.
 *
 * @param dryness 0f healthy, 1f completely dried out.
 * @param recoveryRemaining milliseconds left until it is green again, or null if it is fine.
 */
data class PlantCondition(
    val stage: GrowthStage,
    val growth: Float,
    val dryness: Float,
    val recoveryRemaining: Long?,
    val recoveryTotal: Long
) {
    val recovering: Boolean get() = recoveryRemaining != null
}

/**
 * @param previousStreakMillis how long the streak that just broke lasted, or null if this
 *   habit has never been reset — nothing to wither in that case.
 * @param recoveryBonusMillis time already gained by watering.
 */
fun plantCondition(
    startedAt: Long,
    previousStreakMillis: Long?,
    recoveryBonusMillis: Long = 0L,
    now: Long = System.currentTimeMillis()
): PlantCondition {
    val elapsed = (now - startedAt).coerceAtLeast(0)
    val current = growthStageFor(elapsed)
    val previous = previousStreakMillis?.let { growthStageFor(it) }

    // Si la planta anterior no era mayor que la de ahora no hay nada que marchitar: la racha
    // nueva ya ha alcanzado a la vieja.
    if (previousStreakMillis == null || previous == null || previous.ordinal <= current.ordinal) {
        return PlantCondition(current, current.progressWithin(elapsed), 0f, null, 0L)
    }

    val total = recoveryDurationFor(previousStreakMillis)
    val effective = elapsed + recoveryBonusMillis.coerceAtLeast(0)
    val recovery = (effective.toFloat() / total).coerceIn(0f, 1f)

    if (recovery >= 1f) {
        return PlantCondition(current, current.progressWithin(elapsed), 0f, null, total)
    }

    // El tamaño baja poco a poco desde lo que había hasta lo que toca por la racha nueva, así
    // que al terminar de hidratarse ambos coinciden y no hay ningún salto.
    val steps = ((previous.ordinal - current.ordinal) * recovery).roundToInt()
    val shownOrdinal = (previous.ordinal - steps).coerceAtLeast(current.ordinal)
    val shown = GrowthStage.entries[shownOrdinal]

    return PlantCondition(
        stage = shown,
        growth = if (shown == current) current.progressWithin(elapsed) else 1f,
        dryness = 1f - recovery,
        recoveryRemaining = (total - effective).coerceAtLeast(0),
        recoveryTotal = total
    )
}

/**
 * The streak that was interrupted most recently, or null if the habit was never reset.
 * Taken from the history rows, so nothing extra needs storing.
 */
fun lastBrokenStreakMillis(pastStreaks: List<Pair<Long, Long>>): Long? =
    pastStreaks.maxByOrNull { it.second }?.let { (start, end) -> (end - start).coerceAtLeast(0) }
