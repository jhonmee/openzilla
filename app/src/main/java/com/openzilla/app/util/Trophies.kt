package com.openzilla.app.util

import androidx.annotation.StringRes
import com.openzilla.app.R

/** [labelRes] instead of a literal so the milestone reads in the user's language. */
data class TrophyDef(@StringRes val labelRes: Int, val durationMillis: Long)

val TROPHIES: List<TrophyDef> = listOf(
    TrophyDef(R.string.trophy_24h, 24 * 3_600_000L),
    TrophyDef(R.string.trophy_3d, 3L * 24 * 3_600_000L),
    TrophyDef(R.string.trophy_1w, 7L * 24 * 3_600_000L),
    TrophyDef(R.string.trophy_2w, 14L * 24 * 3_600_000L),
    TrophyDef(R.string.trophy_1mo, 30L * 24 * 3_600_000L),
    TrophyDef(R.string.trophy_3mo, 90L * 24 * 3_600_000L),
    TrophyDef(R.string.trophy_6mo, 182L * 24 * 3_600_000L),
    TrophyDef(R.string.trophy_1y, 365L * 24 * 3_600_000L),
    TrophyDef(R.string.trophy_2y, 730L * 24 * 3_600_000L)
)
