package com.openzilla.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val HOUR = 3_600_000L

/**
 * Guards the counter logic that was previously wrong: progress used to wrap back to zero the
 * instant a goal was met, so a habit started exactly 24 h ago with a 24 h goal showed an
 * empty bar instead of a finished one.
 */
class GoalScaleTest {

    private val now = 1_700_000_000_000L

    private fun startedHoursAgo(hours: Double): Long = now - (hours * HOUR).toLong()

    @Test
    fun `bar is nearly full just before the goal`() {
        val progress = goalProgress(startedHoursAgo(23.9833), 24, now)
        assertTrue("esperaba una barra casi llena, fue $progress", progress > 0.99f)
    }

    @Test
    fun `reaching the goal moves up to the next rung instead of resetting`() {
        val started = startedHoursAgo(24.0)
        assertEquals(72, currentGoalHours(started, 24, now))
        assertEquals(1f / 3f, goalProgress(started, 24, now), 0.01f)
    }

    @Test
    fun `goal never drops below the one the user chose`() {
        val started = startedHoursAgo(1.0)
        assertEquals(168, currentGoalHours(started, 168, now))
    }

    @Test
    fun `a goal outside the preset ladder is still honoured`() {
        val started = startedHoursAgo(10.0)
        assertEquals(48, currentGoalHours(started, 48, now))
        assertEquals(72, currentGoalHours(startedHoursAgo(50.0), 48, now))
    }

    @Test
    fun `progress stays full once the top of the ladder is passed`() {
        val started = now - 8L * 365 * 24 * HOUR
        assertEquals(1f, goalProgress(started, 24, now), 0.0001f)
        assertNull(goalRemainingMillis(started, 24, now))
    }

    @Test
    fun `percentage always agrees with the bar`() {
        val started = startedHoursAgo(30.0)
        val fromBar = goalProgress(started, 24, now) * 100f
        assertEquals(fromBar, goalPercentText(started, 24, now).replace(',', '.').toFloat(), 0.1f)
    }

    @Test
    fun `a future start never yields negative progress`() {
        assertEquals(0f, goalProgress(now + 5 * HOUR, 24, now), 0.0001f)
    }

    @Test
    fun `day map covers the current streak and marks every relapse`() {
        val dayMap = buildHabitDayMap(
            startedAt = now - 2 * 24 * HOUR,
            pastStreaks = listOf((now - 10 * 24 * HOUR) to (now - 2 * 24 * HOUR)),
            now = now
        )
        val today = dayStartOf(now)
        assertTrue(dayMap.coveredRanges.any { today in it })
        assertTrue(dayMap.relapseDays.contains(dayStartOf(now - 2 * 24 * HOUR)))
        assertEquals(dayStartOf(now - 10 * 24 * HOUR), dayMap.firstTrackedDay)
    }
}
