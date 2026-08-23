package com.openzilla.app.util

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

/** Short form like the reference app's "0m 36s" or, once there are days, "3d 4h". */
fun formatElapsedShort(startedAt: Long, now: Long = System.currentTimeMillis()): String {
    val p = elapsedParts(startedAt, now)
    return when {
        p.days > 0 -> "${p.days}d ${p.hours}h"
        p.hours > 0 -> "${p.hours}h ${p.minutes}m"
        else -> "${p.minutes}m ${p.seconds}s"
    }
}

/** Progress (0f..1f) toward the habit's goal window (default 24h), wrapping every window. */
fun goalProgress(startedAt: Long, goalHours: Int, now: Long = System.currentTimeMillis()): Float {
    val windowMillis = goalHours.coerceAtLeast(1) * 3_600_000L
    val elapsed = (now - startedAt).coerceAtLeast(0)
    return ((elapsed % windowMillis).toFloat() / windowMillis.toFloat()).coerceIn(0f, 1f)
}

fun goalPercentText(startedAt: Long, goalHours: Int, now: Long = System.currentTimeMillis()): String {
    val elapsed = (now - startedAt).coerceAtLeast(0)
    val windowMillis = goalHours.coerceAtLeast(1) * 3_600_000L
    val pct = (elapsed.toDouble() / windowMillis.toDouble() * 100.0).coerceAtMost(9999.0)
    return String.format("%.1f", pct)
}
