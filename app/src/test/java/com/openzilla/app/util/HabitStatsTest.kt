package com.openzilla.app.util

import com.openzilla.app.data.HabitCostType
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitStatsTest {

    private val now = 1_700_000_000_000L
    private val day = 86_400_000L

    private fun habit(
        startedAt: Long,
        costType: HabitCostType = HabitCostType.MONEY,
        weeklyAmount: Double? = 70.0
    ) = HabitEntity(
        id = 1,
        name = "Hábito",
        iconKey = "generic",
        costType = costType,
        weeklyAmount = weeklyAmount,
        startedAt = startedAt,
        goalHours = 24,
        createdAt = startedAt
    )

    private fun entry(id: Long, start: Long, end: Long) =
        HistoryEntity(id = id, habitId = 1, streakStart = start, streakEnd = end)

    @Test
    fun `sin historial el rango es la racha actual repetida`() {
        val stats = computeHabitStats(habit(now - 5 * day), emptyList(), now)
        assertEquals(5 * day, stats.range.shortest)
        assertEquals(5 * day, stats.range.average)
        assertEquals(5 * day, stats.range.longest)
        assertEquals(0, stats.relapses)
    }

    @Test
    fun `el rango tiene en cuenta las rachas pasadas y la actual`() {
        val history = listOf(
            entry(1, now - 30 * day, now - 28 * day),  // 2 días
            entry(2, now - 28 * day, now - 18 * day)   // 10 días
        )
        val stats = computeHabitStats(habit(now - 4 * day), history, now)
        assertEquals(2 * day, stats.range.shortest)
        assertEquals(10 * day, stats.range.longest)
        assertEquals((2 + 10 + 4) * day / 3, stats.range.average)
        assertEquals(4 * day, stats.range.current)
        assertEquals(2, stats.relapses)
    }

    @Test
    fun `una semana limpia ahorra exactamente el importe semanal`() {
        val stats = computeHabitStats(habit(now - 7 * day, weeklyAmount = 70.0), emptyList(), now)
        val savings = stats.savings
        assertNotNull(savings)
        assertEquals(10.0, savings!!.perDay, 0.001)
        assertEquals(70.0, savings.currentStreak, 0.01)
        assertEquals(300.0, savings.monthly, 0.01)
    }

    @Test
    fun `el ahorro total suma todas las rachas, no sólo la de ahora`() {
        val history = listOf(entry(1, now - 20 * day, now - 10 * day)) // 10 días
        val stats = computeHabitStats(habit(now - 10 * day, weeklyAmount = 70.0), history, now)
        // 20 días limpios en total a 10 al día
        assertEquals(200.0, stats.savings!!.total, 0.01)
    }

    @Test
    fun `cada recaída se cobra como un día del ritmo anterior`() {
        val history = listOf(
            entry(1, now - 30 * day, now - 20 * day),
            entry(2, now - 20 * day, now - 10 * day)
        )
        val stats = computeHabitStats(habit(now - 10 * day, weeklyAmount = 70.0), history, now)
        assertEquals(20.0, stats.savings!!.relapseCost, 0.01)
        assertEquals(stats.savings.total - 20.0, stats.savings.net, 0.01)
    }

    @Test
    fun `un hábito de tipo evento no tiene cuentas de ahorro`() {
        val stats = computeHabitStats(habit(now - 5 * day, HabitCostType.EVENT, 50.0), emptyList(), now)
        assertNull(stats.savings)
    }

    @Test
    fun `sin importe declarado tampoco hay cuentas`() {
        val stats = computeHabitStats(habit(now - 5 * day, HabitCostType.MONEY, null), emptyList(), now)
        assertNull(stats.savings)
    }

    @Test
    fun `los hábitos de tiempo se miden en horas`() {
        val stats = computeHabitStats(habit(now - 7 * day, HabitCostType.TIME, 14.0), emptyList(), now)
        assertTrue(stats.savingsInHours)
        assertEquals(14.0, stats.savings!!.currentStreak, 0.01)
    }

    @Test
    fun `el mapa de constancia cubre doce semanas exactas`() {
        val stats = computeHabitStats(habit(now - 5 * day), emptyList(), now)
        assertEquals(HEATMAP_WEEKS * 7, stats.heatmap.size)
        // Lo anterior al primer registro no se pinta como cumplido.
        assertTrue(stats.heatmap.first().state == DayState.UNTRACKED)
        assertTrue(stats.heatmap.last().state != DayState.UNTRACKED)
    }

    @Test
    fun `los días de recaída se marcan en el mapa`() {
        val relapseAt = now - 5 * day
        val history = listOf(entry(1, now - 40 * day, relapseAt))
        val stats = computeHabitStats(habit(relapseAt), history, now)
        val relapseDay = dayStartOf(relapseAt)
        val mark = stats.heatmap.firstOrNull { it.dayStart == relapseDay }
        assertNotNull("el día de la recaída debería estar en el mapa", mark)
        assertEquals(DayState.RELAPSE, mark!!.state)
    }
}
