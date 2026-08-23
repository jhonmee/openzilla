package com.openzilla.app.util

data class TrophyDef(val label: String, val durationMillis: Long)

val TROPHIES: List<TrophyDef> = listOf(
    TrophyDef("24 horas", 24 * 3_600_000L),
    TrophyDef("3 días", 3L * 24 * 3_600_000L),
    TrophyDef("1 semana", 7L * 24 * 3_600_000L),
    TrophyDef("2 semanas", 14L * 24 * 3_600_000L),
    TrophyDef("1 mes", 30L * 24 * 3_600_000L),
    TrophyDef("3 meses", 90L * 24 * 3_600_000L),
    TrophyDef("6 meses", 182L * 24 * 3_600_000L),
    TrophyDef("1 año", 365L * 24 * 3_600_000L),
    TrophyDef("2 años", 730L * 24 * 3_600_000L)
)
