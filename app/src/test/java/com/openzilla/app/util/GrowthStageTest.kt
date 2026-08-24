package com.openzilla.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The garden reads the same streak the counters do, so these thresholds are the only thing
 * standing between "my plant grew" and "my plant is wrong".
 */
class GrowthStageTest {

    private val hour = 3_600_000L
    private val day = 24 * hour

    @Test
    fun `un hábito recién creado es una semilla`() {
        assertEquals(GrowthStage.SEED, growthStageFor(0))
        assertEquals(GrowthStage.SEED, growthStageFor(5 * hour))
    }

    @Test
    fun `cada umbral cambia de etapa justo al cumplirse`() {
        assertEquals(GrowthStage.SPROUT, growthStageFor(6 * hour))
        assertEquals(GrowthStage.SEEDLING, growthStageFor(12 * hour))
        assertEquals(GrowthStage.YOUNG, growthStageFor(day))
        assertEquals(GrowthStage.GROWING, growthStageFor(3 * day))
        assertEquals(GrowthStage.MATURE, growthStageFor(7 * day))
        assertEquals(GrowthStage.BUDDING, growthStageFor(14 * day))
        assertEquals(GrowthStage.FLOWERING, growthStageFor(30 * day))
        assertEquals(GrowthStage.SAPLING, growthStageFor(90 * day))
        assertEquals(GrowthStage.TREE, growthStageFor(365 * day))
    }

    @Test
    fun `justo antes de un umbral se sigue en la etapa anterior`() {
        assertEquals(GrowthStage.SEED, growthStageFor(6 * hour - 1))
        assertEquals(GrowthStage.YOUNG, growthStageFor(3 * day - 1))
        assertEquals(GrowthStage.SAPLING, growthStageFor(365 * day - 1))
    }

    @Test
    fun `una racha enorme se queda en árbol, no se sale de la escala`() {
        assertEquals(GrowthStage.TREE, growthStageFor(20 * 365 * day))
        assertEquals(1f, GrowthStage.TREE.progressWithin(20 * 365 * day), 0.001f)
    }

    @Test
    fun `un tiempo negativo no rompe nada`() {
        assertEquals(GrowthStage.SEED, growthStageFor(-1000))
    }

    @Test
    fun `el progreso dentro de una etapa va de cero a uno`() {
        assertEquals(0f, GrowthStage.SPROUT.progressWithin(6 * hour), 0.001f)
        assertEquals(1f, GrowthStage.SPROUT.progressWithin(12 * hour), 0.001f)
        val mid = GrowthStage.SPROUT.progressWithin(9 * hour)
        assertTrue("esperaba un valor intermedio, fue $mid", mid > 0.4f && mid < 0.6f)
    }

    @Test
    fun `la especie no se descubre hasta la plántula`() {
        assertFalse(GrowthStage.SEED.speciesRevealed)
        assertFalse(GrowthStage.SPROUT.speciesRevealed)
        assertTrue(GrowthStage.SEEDLING.speciesRevealed)
        assertTrue(GrowthStage.TREE.speciesRevealed)
    }

    @Test
    fun `las etapas están ordenadas por tiempo`() {
        val hours = GrowthStage.entries.map { it.minHours }
        assertEquals(hours.sorted(), hours)
        assertEquals(10, GrowthStage.entries.size)
    }
}
