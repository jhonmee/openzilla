package com.openzilla.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Withering is the one place where the garden shows something other than the plain streak,
 * so it is worth pinning down: a relapse must never wipe the pot, and the dried plant must
 * meet the new growth exactly when it finishes recovering.
 */
class PlantConditionTest {

    private val hour = 3_600_000L
    private val day = 24 * hour
    private val now = 1_700_000_000_000L

    @Test
    fun `sin recaídas previas la planta está sana`() {
        val condition = plantCondition(startedAt = now - 10 * day, previousStreakMillis = null, now = now)
        assertEquals(0f, condition.dryness, 0.001f)
        assertFalse(condition.recovering)
        assertEquals(growthStageFor(10 * day), condition.stage)
    }

    @Test
    fun `perder mucho cuesta más de recuperar que perder poco`() {
        val corta = recoveryDurationFor(2 * day)
        val media = recoveryDurationFor(30 * day)
        val larga = recoveryDurationFor(90 * day)
        assertTrue("$corta debería ser menor que $media", corta < media)
        assertTrue("$media debería ser menor que $larga", media < larga)
    }

    @Test
    fun `la recuperación tiene suelo y techo`() {
        // Una racha de una hora no deja la planta seca un minuto...
        assertEquals(12 * hour, recoveryDurationFor(hour))
        // ...ni perder cinco años la deja marrón media vida.
        assertEquals(10 * day, recoveryDurationFor(5 * 365 * day))
    }

    @Test
    fun `recién recaído la planta conserva su tamaño pero seca del todo`() {
        val previous = 200 * day
        val condition = plantCondition(startedAt = now, previousStreakMillis = previous, now = now)
        assertEquals(growthStageFor(previous), condition.stage)
        assertEquals(1f, condition.dryness, 0.001f)
        assertTrue(condition.recovering)
        assertEquals(recoveryDurationFor(previous), condition.recoveryRemaining)
    }

    @Test
    fun `a mitad de la recuperación está a medio secar`() {
        val previous = 200 * day
        val elapsed = recoveryDurationFor(previous) / 2
        val condition = plantCondition(startedAt = now - elapsed, previousStreakMillis = previous, now = now)
        assertEquals(0.5f, condition.dryness, 0.02f)
        assertNotNull(condition.recoveryRemaining)
    }

    @Test
    fun `al terminar de hidratarse coincide con la racha nueva, sin saltos`() {
        val previous = 200 * day
        val elapsed = recoveryDurationFor(previous)
        val condition = plantCondition(startedAt = now - elapsed, previousStreakMillis = previous, now = now)
        assertEquals(0f, condition.dryness, 0.001f)
        assertNull(condition.recoveryRemaining)
        assertEquals(growthStageFor(elapsed), condition.stage)
    }

    @Test
    fun `regar adelanta la recuperación`() {
        val previous = 200 * day
        val elapsed = day
        val sinRegar = plantCondition(now - elapsed, previous, 0L, now)
        val regado = plantCondition(now - elapsed, previous, wateringBoostFor(previous), now)
        assertTrue("regar debería dejarla menos seca", regado.dryness < sinRegar.dryness)
        assertTrue(regado.recoveryRemaining!! < sinRegar.recoveryRemaining!!)
    }

    @Test
    fun `regar lo suficiente termina la recuperación`() {
        val previous = 200 * day
        val total = recoveryDurationFor(previous)
        val condition = plantCondition(now, previous, total, now)
        assertEquals(0f, condition.dryness, 0.001f)
        assertNull(condition.recoveryRemaining)
    }

    @Test
    fun `sólo se puede regar una vez al día`() {
        assertTrue("nunca regada", canWaterToday(0L, now))
        assertFalse("ya regada hoy", canWaterToday(now - hour, now))
        assertTrue("regada ayer", canWaterToday(now - 2 * day, now))
    }

    @Test
    fun `la planta nunca se ve más pequeña de lo que le toca por su racha`() {
        val previous = 200 * day
        listOf(0L, hour, day, 2 * day, 10 * day).forEach { elapsed ->
            val condition = plantCondition(now - elapsed, previous, 0L, now)
            assertTrue(
                "con $elapsed ms salió ${condition.stage}",
                condition.stage.ordinal >= growthStageFor(elapsed).ordinal
            )
        }
    }

    @Test
    fun `si la racha rota era más corta que la actual no hay nada que marchitar`() {
        val condition = plantCondition(startedAt = now - 50 * day, previousStreakMillis = hour, now = now)
        assertEquals(0f, condition.dryness, 0.001f)
        assertFalse(condition.recovering)
    }

    @Test
    fun `la racha rota sale del historial`() {
        assertNull(lastBrokenStreakMillis(emptyList()))
        val streaks = listOf(now - 30 * day to now - 20 * day, now - 20 * day to now - 5 * day)
        assertEquals(15 * day, lastBrokenStreakMillis(streaks))
    }
}
