package com.openzilla.app.util

import java.util.Calendar

data class Quote(val text: String, val author: String)

private val QUOTES = listOf(
    Quote("Un viaje de mil millas comienza con el primer paso.", "Lao Tzu"),
    Quote("No cuentes los días, haz que los días cuenten.", "Muhammad Ali"),
    Quote("La disciplina es elegir entre lo que quieres ahora y lo que quieres más.", "Anónimo"),
    Quote("Cada día es una nueva oportunidad para cambiar tu vida.", "Anónimo"),
    Quote("El hábito es al principio como una tela de araña; al final, como un cable de acero.", "Proverbio chino"),
    Quote("No se trata de ser perfecto, se trata de no rendirse.", "Anónimo"),
    Quote("Tu futuro se crea por lo que haces hoy, no mañana.", "Robert Kiyosaki"),
    Quote("La libertad empieza donde termina la dependencia.", "Anónimo"),
    Quote("Pequeños pasos diarios llevan a grandes cambios.", "Anónimo"),
    Quote("Eres más fuerte que cualquier excusa.", "Anónimo")
)

/** Deterministic by calendar day, so the "quote of the day" doesn't need any stored state. */
fun quoteOfTheDay(date: Calendar = Calendar.getInstance()): Quote {
    val dayOfYear = date.get(Calendar.DAY_OF_YEAR) + date.get(Calendar.YEAR)
    return QUOTES[dayOfYear % QUOTES.size]
}
