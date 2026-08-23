package com.openzilla.app.util

import androidx.annotation.StringRes
import com.openzilla.app.R
import java.util.concurrent.TimeUnit

data class ElapsedParts(val days: Long, val hours: Long, val minutes: Long, val seconds: Long)

fun elapsedParts(startedAt: Long, now: Long = System.currentTimeMillis()): ElapsedParts {
    val diff = (now - startedAt).coerceAtLeast(0)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
    return ElapsedParts(days, hours, minutes, seconds)
}

/** Short form for a plain duration: "3d 4h", "5h 12m" or "0m 36s". */
fun formatDurationShort(millis: Long): String {
    val diff = millis.coerceAtLeast(0)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m ${seconds}s"
    }
}

/** Short form like the reference app's "0m 36s" or, once there are days, "3d 4h". */
fun formatElapsedShort(startedAt: Long, now: Long = System.currentTimeMillis()): String =
    formatDurationShort(now - startedAt)

/**
 * One rung of the goal ladder. The user picks the first goal when creating the habit; from
 * then on the app moves to the next rung on its own each time one is reached, so the gauge
 * always measures against something still ahead instead of restarting from zero.
 */
data class GoalStep(val hours: Int, @StringRes val labelRes: Int)

/** Hours are exact (30-day months, 365-day years) so a goal never drifts against the counter. */
val GOAL_SCALE: List<GoalStep> = listOf(
    GoalStep(6, R.string.goal_6h),
    GoalStep(12, R.string.goal_12h),
    GoalStep(24, R.string.goal_1d),
    GoalStep(72, R.string.goal_3d),
    GoalStep(168, R.string.goal_1w),
    GoalStep(336, R.string.goal_2w),
    GoalStep(720, R.string.goal_1mo),
    GoalStep(2_160, R.string.goal_3mo),
    GoalStep(4_368, R.string.goal_6mo),
    GoalStep(8_760, R.string.goal_1y),
    GoalStep(17_520, R.string.goal_2y),
    GoalStep(43_800, R.string.goal_5y)
)

/** Goals offered when creating or editing a habit; past the last one the ladder continues by itself. */
val SELECTABLE_GOALS: List<GoalStep> = GOAL_SCALE.take(8)

/** The ladder's own label for [hours], or null for a value that is not one of its rungs. */
@StringRes
fun goalLabelRes(hours: Int): Int? = GOAL_SCALE.firstOrNull { it.hours == hours }?.labelRes

/**
 * The goal currently being worked toward: the first rung strictly greater than the time
 * already elapsed, never below the goal the user chose. Once the top rung is passed it
 * stays there (the gauge simply reads 100%).
 */
fun currentGoalHours(startedAt: Long, baseGoalHours: Int, now: Long = System.currentTimeMillis()): Int {
    val base = baseGoalHours.coerceAtLeast(1)
    val elapsedHours = (now - startedAt).coerceAtLeast(0).toDouble() / 3_600_000.0
    val ladder = (GOAL_SCALE.map { it.hours } + base).filter { it >= base }.distinct().sorted()
    return ladder.firstOrNull { it > elapsedHours } ?: ladder.last()
}

/** Progress (0f..1f) toward the goal currently in play — it fills up and then rolls over to the next rung. */
fun goalProgress(startedAt: Long, goalHours: Int, now: Long = System.currentTimeMillis()): Float {
    val windowMillis = currentGoalHours(startedAt, goalHours, now).toDouble() * 3_600_000.0
    val elapsed = (now - startedAt).coerceAtLeast(0).toDouble()
    return (elapsed / windowMillis).coerceIn(0.0, 1.0).toFloat()
}

/** Percentage of the goal currently in play, so it always agrees with what the bar shows. */
fun goalPercentText(startedAt: Long, goalHours: Int, now: Long = System.currentTimeMillis()): String =
    String.format("%.1f", goalProgress(startedAt, goalHours, now) * 100f)

/** Time still missing to reach the goal in play, or null once it is already met. */
fun goalRemainingMillis(startedAt: Long, goalHours: Int, now: Long = System.currentTimeMillis()): Long? {
    val target = startedAt + currentGoalHours(startedAt, goalHours, now) * 3_600_000L
    val remaining = target - now
    return if (remaining > 0) remaining else null
}
