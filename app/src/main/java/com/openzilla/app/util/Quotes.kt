package com.openzilla.app.util

import android.content.Context
import com.openzilla.app.R
import java.util.Calendar

data class Quote(val text: String, val author: String)

/**
 * Deterministic by calendar day, so the "quote of the day" doesn't need any stored state.
 * The texts live in resources (two arrays paired by position), which is what lets them be
 * translated; the index is picked from the date, never at random.
 */
fun quoteOfTheDay(context: Context, date: Calendar = Calendar.getInstance()): Quote {
    val texts = context.resources.getStringArray(R.array.quote_texts)
    val authors = context.resources.getStringArray(R.array.quote_authors)
    if (texts.isEmpty()) return Quote("", "")
    val dayOfYear = date.get(Calendar.DAY_OF_YEAR) + date.get(Calendar.YEAR)
    val index = dayOfYear % texts.size
    return Quote(texts[index], authors.getOrElse(index) { "" })
}
