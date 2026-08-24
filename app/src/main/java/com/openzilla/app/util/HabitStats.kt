package com.openzilla.app.util

import com.openzilla.app.data.HabitCostType
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HistoryEntity

/** Lo más corto, lo normal y lo más largo que ha aguantado, más lo que lleva ahora. */
data class StreakRange(
    val shortest: Long,
    val average: Long,
    val longest: Long,
    val current: Long
) {
    /** 0f..1f: dónde cae [current] entre el mínimo y el máximo, para situarlo en la barra. */
    val currentPosition: Float
        get() {
            val span = longest - shortest
            return if (span <= 0L) 1f else ((current - shortest).toFloat() / span).coerceIn(0f, 1f)
        }

    val averagePosition: Float
        get() {
            val span = longest - shortest
            return if (span <= 0L) 1f else ((average - shortest).toFloat() / span).coerceIn(0f, 1f)
        }
}

/**
 * Money (or hours) the habit is no longer costing.
 *
 * Everything here comes from one declared figure — what the habit used to cost per week — and
 * the time actually on record. [relapseCost] is the one estimate: a relapse means the habit
 * happened at least once, priced at a single day of the old rate. It is labelled as an
 * estimate on screen because that is exactly what it is; the app never asks what a relapse
 * really cost, and inventing a precise number would be worse than admitting the approximation.
 */
data class SavingsSummary(
    val perDay: Double,
    val currentStreak: Double,
    val total: Double,
    val monthly: Double,
    val yearly: Double,
    val relapseCost: Double
) {
    val net: Double get() = total - relapseCost
}

enum class DayState { UNTRACKED, CLEAN, RELAPSE }

/** Un día del mapa de constancia. */
data class DayMark(val dayStart: Long, val state: DayState)

data class HabitStats(
    val firstTrackedDay: Long,
    val relapses: Int,
    val range: StreakRange,
    /** null cuando el hábito no tiene un coste declarado (tipo evento o importe vacío). */
    val savings: SavingsSummary?,
    val savingsInHours: Boolean,
    val heatmap: List<DayMark>
)

/** Cuántas semanas enseña el mapa de constancia. */
const val HEATMAP_WEEKS = 12

private const val DAY_MILLIS = 86_400_000L

fun computeHabitStats(
    habit: HabitEntity,
    history: List<HistoryEntity>,
    now: Long = System.currentTimeMillis()
): HabitStats {
    val currentStreak = (now - habit.startedAt).coerceAtLeast(0)
    val pastStreaks = history.map { (it.streakEnd - it.streakStart).coerceAtLeast(0) }
    val allStreaks = pastStreaks + currentStreak

    val range = StreakRange(
        shortest = allStreaks.min(),
        average = allStreaks.sum() / allStreaks.size,
        longest = allStreaks.max(),
        current = currentStreak
    )

    val dayMap = buildHabitDayMap(habit.startedAt, history.map { it.streakStart to it.streakEnd }, now)

    // Las rachas se encadenan sin huecos (una empieza donde acaba la anterior), así que el
    // tiempo limpio total es sencillamente todo lo que hay registrado.
    val totalCleanMillis = allStreaks.sum()

    val amount = habit.weeklyAmount
    val savings = if (amount != null && amount > 0 && habit.costType != HabitCostType.EVENT) {
        val perDay = amount / 7.0
        SavingsSummary(
            perDay = perDay,
            currentStreak = perDay * currentStreak.toDouble() / DAY_MILLIS,
            total = perDay * totalCleanMillis.toDouble() / DAY_MILLIS,
            monthly = perDay * 30,
            yearly = perDay * 365,
            relapseCost = perDay * history.size
        )
    } else {
        null
    }

    return HabitStats(
        firstTrackedDay = dayMap.firstTrackedDay,
        relapses = history.size,
        range = range,
        savings = savings,
        savingsInHours = habit.costType == HabitCostType.TIME,
        heatmap = buildHeatmap(dayMap, now)
    )
}

/**
 * The last [HEATMAP_WEEKS] weeks, one entry per day, oldest first. Days before there was
 * anything to record stay [DayState.UNTRACKED] so the grid does not claim a perfect run that
 * never happened.
 */
private fun buildHeatmap(dayMap: HabitDayMap, now: Long): List<DayMark> {
    val today = dayStartOf(now)
    val days = HEATMAP_WEEKS * 7
    return (0 until days).map { index ->
        // Se normaliza a medianoche: restar días de 24 h exactas se desvía con los cambios
        // de hora, y entonces el día no coincidiría con los del calendario.
        val day = dayStartOf(today - (days - 1 - index) * DAY_MILLIS)
        val state = when {
            day < dayMap.firstTrackedDay -> DayState.UNTRACKED
            day in dayMap.relapseDays -> DayState.RELAPSE
            dayMap.coveredRanges.any { day in it } -> DayState.CLEAN
            else -> DayState.UNTRACKED
        }
        DayMark(day, state)
    }
}
