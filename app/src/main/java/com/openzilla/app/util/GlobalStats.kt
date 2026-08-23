package com.openzilla.app.util

import com.openzilla.app.data.HabitCostType
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HistoryEntity
import java.util.Calendar

/** One habit's current streak, for the comparison chart. */
data class HabitStreak(val id: Long, val name: String, val iconKey: String, val millis: Long)

/** Relapses recorded in one calendar month. [monthOffset] 0 is the current month, -1 the previous one. */
data class MonthlyRelapses(val monthOffset: Int, val month: Int, val year: Int, val count: Int)

/**
 * Everything the general statistics screen shows, computed in one pass so the screen itself
 * only draws. Pure function of its inputs — no clock, no database, no Android: [now] is
 * passed in, which is what makes it testable.
 */
data class GlobalStats(
    val habitCount: Int,
    val totalCleanMillis: Long,
    val bestStreakMillis: Long,
    val relapseCount: Int,
    val currentStreaks: List<HabitStreak>,
    val relapsesByMonth: List<MonthlyRelapses>,
    val moneySaved: Double,
    val hoursSaved: Double,
    val countByType: Map<HabitCostType, Int>
) {
    val hasAnything: Boolean get() = habitCount > 0
}

/** How many months back the relapse chart looks. */
const val RELAPSE_MONTHS = 6

fun computeGlobalStats(
    habits: List<HabitEntity>,
    history: List<HistoryEntity>,
    now: Long = System.currentTimeMillis()
): GlobalStats {
    val streaks = habits.map { habit ->
        HabitStreak(habit.id, habit.name, habit.iconKey, (now - habit.startedAt).coerceAtLeast(0))
    }

    val bestPast = history.maxOfOrNull { (it.streakEnd - it.streakStart).coerceAtLeast(0) } ?: 0L
    val bestCurrent = streaks.maxOfOrNull { it.millis } ?: 0L

    var money = 0.0
    var hours = 0.0
    habits.forEach { habit ->
        val amount = habit.weeklyAmount ?: return@forEach
        if (amount <= 0) return@forEach
        val days = (now - habit.startedAt).coerceAtLeast(0).toDouble() / 86_400_000.0
        when (habit.costType) {
            HabitCostType.MONEY -> money += amount / 7.0 * days
            HabitCostType.TIME -> hours += amount / 7.0 * days
            HabitCostType.EVENT -> Unit
        }
    }

    return GlobalStats(
        habitCount = habits.size,
        totalCleanMillis = streaks.sumOf { it.millis },
        bestStreakMillis = maxOf(bestPast, bestCurrent),
        relapseCount = history.size,
        currentStreaks = streaks.sortedByDescending { it.millis },
        relapsesByMonth = relapsesByMonth(history, now),
        moneySaved = money,
        hoursSaved = hours,
        countByType = habits.groupingBy { it.costType }.eachCount()
    )
}

/** Relapse counts for the last [RELAPSE_MONTHS] months, oldest first and including empty months. */
private fun relapsesByMonth(history: List<HistoryEntity>, now: Long): List<MonthlyRelapses> {
    // Dos Calendar distintos a propósito: uno marca el mes del grupo y otro recorre las
    // recaídas. Compartir uno solo funcionaría por los pelos y se rompería al primer cambio.
    val bucketCal = Calendar.getInstance()
    val entryCal = Calendar.getInstance()
    val buckets = ArrayList<MonthlyRelapses>(RELAPSE_MONTHS)

    for (offset in -(RELAPSE_MONTHS - 1)..0) {
        bucketCal.timeInMillis = now
        bucketCal.add(Calendar.MONTH, offset)
        val month = bucketCal.get(Calendar.MONTH)
        val year = bucketCal.get(Calendar.YEAR)
        val count = history.count { entry ->
            entryCal.timeInMillis = entry.streakEnd
            entryCal.get(Calendar.MONTH) == month && entryCal.get(Calendar.YEAR) == year
        }
        buckets.add(MonthlyRelapses(offset, month, year, count))
    }
    return buckets
}
