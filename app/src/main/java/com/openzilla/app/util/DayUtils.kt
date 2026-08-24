package com.openzilla.app.util

import java.util.Calendar

/** Midnight (local time) of the day the given instant falls on. */
fun dayStartOf(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/**
 * Everything the calendar needs to paint a habit, expressed in whole local days.
 *
 * [coveredRanges] are the day spans the user actually held the habit (every past streak plus
 * the one running now), [relapseDays] the days a streak was broken on, and [firstTrackedDay]
 * the first day there is any record of — before it the calendar has nothing to say.
 */
data class HabitDayMap(
    val coveredRanges: List<LongRange>,
    val relapseDays: Set<Long>,
    val firstTrackedDay: Long,
    val currentStreakStartDay: Long
)

/**
 * @param pastStreaks each finished streak as (start, end) in millis — i.e. the history table.
 */
fun buildHabitDayMap(
    startedAt: Long,
    pastStreaks: List<Pair<Long, Long>>,
    now: Long = System.currentTimeMillis()
): HabitDayMap {
    val currentStart = dayStartOf(startedAt)
    val ranges = ArrayList<LongRange>(pastStreaks.size + 1)
    val relapses = HashSet<Long>(pastStreaks.size)

    pastStreaks.forEach { (start, end) ->
        val from = dayStartOf(start)
        val to = dayStartOf(end)
        if (to >= from) ranges.add(from..to)
        relapses.add(to)
    }
    ranges.add(currentStart..dayStartOf(now))

    val firstDay = minOf(currentStart, ranges.minOf { it.first })
    return HabitDayMap(
        coveredRanges = ranges,
        relapseDays = relapses,
        firstTrackedDay = firstDay,
        currentStreakStartDay = currentStart
    )
}
