package com.openzilla.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.openzilla.app.R
import com.openzilla.app.util.goalLabelRes

/**
 * Human-readable name for a goal length.
 *
 * Values on the ladder have a written label of their own ("1 day", "3 months"); anything
 * else — a habit imported from a file with an arbitrary number, say — falls back to a
 * generic "N hours" / "N days", which is why this needs resources and cannot be a plain
 * string in the util layer.
 */
@Composable
fun goalLabel(hours: Int): String {
    goalLabelRes(hours)?.let { return stringResource(it) }
    val days = hours / 24
    return when {
        hours < 24 || hours % 24 != 0 -> stringResource(R.string.goal_hours_generic, hours)
        else -> stringResource(R.string.goal_days_generic, days)
    }
}
