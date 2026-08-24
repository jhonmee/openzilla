package com.openzilla.app.util

import androidx.annotation.StringRes
import com.openzilla.app.R

/** Cómo se dibujan las hojas de una especie. */
enum class LeafShape { OVAL, ROUND, NEEDLE }

/** Qué le sale a la especie cuando llega a árbol. Las de NONE se quedan siempre de hoja. */
enum class CanopyShape { CLUSTER, CONE, FAN, NONE }

/**
 * The ten plants a habit can turn out to be.
 *
 * Which one a habit gets is derived from its id and never stored: no column, no migration,
 * and it stays the same across reinstalls of the same database. The [tint] is blended with
 * the user's accent rather than replacing it, so the garden still answers to the chosen
 * colour while each species remains recognisable.
 */
enum class PlantSpecies(
    @StringRes val nameRes: Int,
    val leaf: LeafShape,
    val canopy: CanopyShape,
    /** Verde (o similar) con el que se tiñe el acento. */
    val tint: Long,
    val canopyScale: Float,
    val leafiness: Float,
    val flowers: Boolean,
    /** Color de flores y frutos, también mezclado con el acento. */
    val bloomTint: Long
) {
    OAK(R.string.species_oak, LeafShape.OVAL, CanopyShape.CLUSTER, 0xFF4C8B3F, 1.0f, 1.0f, false, 0xFFD9A441),
    PINE(R.string.species_pine, LeafShape.NEEDLE, CanopyShape.CONE, 0xFF2E6B4F, 0.95f, 1.2f, false, 0xFF8D6E4A),
    PALM(R.string.species_palm, LeafShape.OVAL, CanopyShape.FAN, 0xFF62A83B, 1.05f, 0.9f, false, 0xFFD98E2B),
    BAMBOO(R.string.species_bamboo, LeafShape.NEEDLE, CanopyShape.NONE, 0xFF3FA06B, 0.75f, 1.3f, false, 0xFFB7C948),
    CHERRY(R.string.species_cherry, LeafShape.ROUND, CanopyShape.CLUSTER, 0xFF5FA05C, 1.0f, 1.0f, true, 0xFFE87FA6),
    OLIVE(R.string.species_olive, LeafShape.OVAL, CanopyShape.CLUSTER, 0xFF7C9A5A, 0.9f, 1.1f, true, 0xFF6B5B8C),
    FERN(R.string.species_fern, LeafShape.NEEDLE, CanopyShape.NONE, 0xFF356B45, 0.8f, 1.5f, false, 0xFF88B04B),
    LAVENDER(R.string.species_lavender, LeafShape.NEEDLE, CanopyShape.NONE, 0xFF6E9A6B, 0.8f, 1.2f, true, 0xFF8E6FC4),
    SUCCULENT(R.string.species_succulent, LeafShape.ROUND, CanopyShape.NONE, 0xFF56A88C, 0.75f, 1.1f, false, 0xFFE8A0B4),
    SUNFLOWER(R.string.species_sunflower, LeafShape.ROUND, CanopyShape.NONE, 0xFF5C9A3C, 0.9f, 0.9f, true, 0xFFE8C33F);

    companion object {
        /**
         * Stable per habit and spread out: multiplying by a prime before taking the remainder
         * stops habits created one after another from all landing on neighbouring species.
         */
        fun forHabit(habitId: Long): PlantSpecies {
            val index = ((habitId * 31 + 17) % entries.size).toInt()
            return entries[if (index < 0) index + entries.size else index]
        }
    }
}
