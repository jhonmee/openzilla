package com.openzilla.app.util

import com.openzilla.app.data.HabitCostType
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * The general statistics screen only draws what this function returns, so getting the numbers
 * right here is what keeps that screen honest.
 */
class GlobalStatsTest {

    private val now = 1_700_000_000_000L
    private val hour = 3_600_000L
    private val day = 24 * hour

    private fun habit(
        id: Long,
        startedAt: Long,
        costType: HabitCostType = HabitCostType.EVENT,
        weeklyAmount: Double? = null
    ) = HabitEntity(
        id = id,
        name = "Hábito $id",
        iconKey = "generic",
        costType = costType,
        weeklyAmount = weeklyAmount,
        startedAt = startedAt,
        goalHours = 24,
        createdAt = startedAt
    )

    @Test
    fun `sin hábitos no hay nada que enseñar`() {
        val stats = computeGlobalStats(emptyList(), emptyList(), now)
        assertEquals(0, stats.habitCount)
        assertTrue(!stats.hasAnything)
    }

    @Test
    fun `el tiempo acumulado suma las rachas de todos los hábitos`() {
        val habits = listOf(habit(1, now - 2 * day), habit(2, now - 3 * day))
        val stats = computeGlobalStats(habits, emptyList(), now)
        assertEquals(5 * day, stats.totalCleanMillis)
        assertEquals(2, stats.habitCount)
    }

    @Test
    fun `la mejor racha tiene en cuenta también las ya terminadas`() {
        val habits = listOf(habit(1, now - 2 * day))
        val history = listOf(HistoryEntity(id = 1, habitId = 1, streakStart = now - 30 * day, streakEnd = now - 20 * day))
        val stats = computeGlobalStats(habits, history, now)
        assertEquals(10 * day, stats.bestStreakMillis)
    }

    @Test
    fun `las rachas se ordenan de mayor a menor`() {
        val habits = listOf(habit(1, now - day), habit(2, now - 5 * day), habit(3, now - 3 * day))
        val stats = computeGlobalStats(habits, emptyList(), now)
        assertEquals(listOf(2L, 3L, 1L), stats.currentStreaks.map { it.id })
    }

    @Test
    fun `el ahorro se reparte entre dinero y tiempo según el tipo`() {
        val habits = listOf(
            habit(1, now - 7 * day, HabitCostType.MONEY, weeklyAmount = 70.0),
            habit(2, now - 7 * day, HabitCostType.TIME, weeklyAmount = 14.0),
            habit(3, now - 7 * day, HabitCostType.EVENT, weeklyAmount = 99.0)
        )
        val stats = computeGlobalStats(habits, emptyList(), now)
        // Una semana entera equivale exactamente al importe semanal declarado.
        assertEquals(70.0, stats.moneySaved, 0.01)
        assertEquals(14.0, stats.hoursSaved, 0.01)
    }

    @Test
    fun `el reparto por tipo cuenta cada hábito una sola vez`() {
        val habits = listOf(
            habit(1, now, HabitCostType.MONEY),
            habit(2, now, HabitCostType.MONEY),
            habit(3, now, HabitCostType.TIME)
        )
        val stats = computeGlobalStats(habits, emptyList(), now)
        assertEquals(2, stats.countByType[HabitCostType.MONEY])
        assertEquals(1, stats.countByType[HabitCostType.TIME])
        assertEquals(null, stats.countByType[HabitCostType.EVENT])
    }

    @Test
    fun `el gráfico mensual devuelve siempre seis meses, aunque estén vacíos`() {
        val stats = computeGlobalStats(listOf(habit(1, now)), emptyList(), now)
        assertEquals(RELAPSE_MONTHS, stats.relapsesByMonth.size)
        assertTrue(stats.relapsesByMonth.all { it.count == 0 })
        // El último grupo es el mes en curso.
        assertEquals(0, stats.relapsesByMonth.last().monthOffset)
    }

    @Test
    fun `una recaída de este mes cae en el último grupo`() {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        // Un instante del mismo mes, garantizado dentro del rango del mes en curso.
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        val thisMonth = cal.timeInMillis

        val history = listOf(HistoryEntity(id = 1, habitId = 1, streakStart = thisMonth - day, streakEnd = thisMonth))
        val stats = computeGlobalStats(listOf(habit(1, now)), history, now)
        assertEquals(1, stats.relapsesByMonth.last().count)
        assertEquals(1, stats.relapseCount)
    }
}
