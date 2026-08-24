package com.openzilla.app.util

import androidx.annotation.StringRes
import com.openzilla.app.R

/**
 * How grown a habit's plant is in the garden.
 *
 * The idea is borrowed from PvZ's Zen Garden and from focus apps that grow a tree while you
 * stay on task: something alive that visibly rewards patience. Here the only thing that
 * feeds it is the streak already on record — the garden never stores state of its own, it
 * is a second reading of the same `startedAt` the counters use, so it cannot drift from them
 * and it cannot break them.
 *
 * The rungs match the goal ladder on purpose: reaching a goal is also a visible jump in the
 * garden.
 */
enum class GrowthStage(val minHours: Int, @StringRes val labelRes: Int) {
    SEED(0, R.string.stage_seed),
    SPROUT(6, R.string.stage_sprout),
    SEEDLING(24, R.string.stage_seedling),
    PLANT(72, R.string.stage_plant),
    BUSH(168, R.string.stage_bush),
    SAPLING(720, R.string.stage_sapling),
    TREE(8_760, R.string.stage_tree);

    /** 0f..1f inside this stage; the drawing uses it so growth looks continuous, not stepped. */
    fun progressWithin(elapsedMillis: Long): Float {
        val next = entries.getOrNull(ordinal + 1) ?: return 1f
        val from = minHours * 3_600_000.0
        val to = next.minHours * 3_600_000.0
        val elapsed = elapsedMillis.coerceAtLeast(0).toDouble()
        return ((elapsed - from) / (to - from)).coerceIn(0.0, 1.0).toFloat()
    }
}

fun growthStageFor(elapsedMillis: Long): GrowthStage {
    val hours = elapsedMillis.coerceAtLeast(0).toDouble() / 3_600_000.0
    return GrowthStage.entries.last { hours >= it.minHours }
}
