package com.openzilla.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The species is derived from the habit id and never stored, so the only two things that can
 * go wrong are it changing on its own or every habit landing on the same plant.
 */
class PlantSpeciesTest {

    @Test
    fun `hay diez especies`() {
        assertEquals(10, PlantSpecies.entries.size)
    }

    @Test
    fun `el mismo hábito siempre da la misma especie`() {
        repeat(50) { id ->
            val first = PlantSpecies.forHabit(id.toLong())
            assertEquals(first, PlantSpecies.forHabit(id.toLong()))
        }
    }

    @Test
    fun `hábitos creados seguidos no repiten especie`() {
        val firstTen = (1L..10L).map { PlantSpecies.forHabit(it) }
        assertEquals("los diez primeros hábitos deberían dar diez plantas distintas", 10, firstTen.toSet().size)
    }

    @Test
    fun `las especies se reparten de forma pareja`() {
        val counts = (1L..1000L).map { PlantSpecies.forHabit(it) }.groupingBy { it }.eachCount()
        assertEquals(10, counts.size)
        counts.forEach { (species, count) ->
            assertTrue("$species salió $count veces", count in 80..120)
        }
    }

    @Test
    fun `un id enorme sigue devolviendo una especie válida`() {
        val species = PlantSpecies.forHabit(Long.MAX_VALUE / 2)
        assertTrue(species in PlantSpecies.entries)
    }
}
