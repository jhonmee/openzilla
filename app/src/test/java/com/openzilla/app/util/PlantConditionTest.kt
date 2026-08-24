package com.openzilla.app.util

import org.junit.Assert.assertEquals
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
        assertEquals(growthStageFor(10 * day), condition.stage)
    }

    @Test
    fun `recién recaído la planta conserva su tamaño pero seca del todo`() {
        val previous = 200 * day // era un árbol
        val condition = plantCondition(startedAt = now, previousStreakMillis = previous, now = now)
        assertEquals(growthStageFor(previous), condition.stage)
        assertEquals(1f, condition.dryness, 0.001f)
    }

    @Test
    fun `a mitad de la recuperación está a medio secar y a medio encoger`() {
        val previous = 200 * day
        val elapsed = RECOVERY_MILLIS / 2
        val condition = plantCondition(startedAt = now - elapsed, previousStreakMillis = previous, now = now)
        assertEquals(0.5f, condition.dryness, 0.02f)
        val big = growthStageFor(previous).ordinal
        val small = growthStageFor(elapsed).ordinal
        assertTrue(
            "esperaba una etapa intermedia, fue ${condition.stage}",
            condition.stage.ordinal in small..big
        )
    }

    @Test
    fun `al terminar de hidratarse coincide con la racha nueva, sin saltos`() {
        val previous = 200 * day
        val condition = plantCondition(startedAt = now - RECOVERY_MILLIS, previousStreakMillis = previous, now = now)
        assertEquals(0f, condition.dryness, 0.001f)
        assertEquals(growthStageFor(RECOVERY_MILLIS), condition.stage)
    }

    @Test
    fun `la planta nunca se ve más pequeña de lo que le toca por su racha`() {
        val previous = 200 * day
        listOf(0L, hour, day, 2 * day, RECOVERY_MILLIS, 10 * day).forEach { elapsed ->
            val condition = plantCondition(now - elapsed, previous, now)
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
    }

    @Test
    fun `la racha rota sale del historial`() {
        assertNull(lastBrokenStreakMillis(emptyList()))
        val streaks = listOf(now - 30 * day to now - 20 * day, now - 20 * day to now - 5 * day)
        assertEquals(15 * day, lastBrokenStreakMillis(streaks))
    }
}
