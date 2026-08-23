package com.openzilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Month grid showing, for each day, whether the habit's current streak already covered it
 * (a filled dot) — a simplified, non-editable version of the reference app's calendar.
 */
@Composable
fun CalendarGrid(
    monthAnchor: Calendar,
    streakStartMillis: Long,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthLabel = remember(monthAnchor.timeInMillis) {
        SimpleDateFormat("MMMM yyyy", Locale("es")).format(monthAnchor.time).replaceFirstChar { it.uppercase() }
    }
    val firstDay = (monthAnchor.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val startOffset = firstDay.get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 1 -> 0 offset
    val daysInMonth = monthAnchor.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance()
    val streakStartDay = dayStart(streakStartMillis)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevMonth) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Mes anterior") }
            Text(monthLabel, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onNextMonth) { Icon(Icons.Filled.ChevronRight, contentDescription = "Mes siguiente") }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("dom", "lun", "mar", "mié", "jue", "vie", "sáb").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
            }
        }
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        var dayCounter = 1
        repeat(rows) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = rowIndex * 7 + col
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (cellIndex >= startOffset && dayCounter <= daysInMonth) {
                            val cellCal = (monthAnchor.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayCounter) }
                            val cellDayStart = dayStart(cellCal.timeInMillis)
                            val isCovered = cellDayStart in streakStartDay..dayStart(today.timeInMillis)
                            val isToday = isSameDay(cellCal, today)
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .then(if (isCovered && !isToday) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape) else Modifier)
                                    .then(if (isToday) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    dayCounter.toString(),
                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}

private fun dayStart(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun isSameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
