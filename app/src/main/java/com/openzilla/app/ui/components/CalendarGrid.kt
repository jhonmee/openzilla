package com.openzilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openzilla.app.util.dayStartOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class DayCell(val dayOfMonth: Int, val dayStart: Long)

/**
 * Month grid for one habit: days held show filled in the accent color, days a streak broke
 * on are marked in the error color, and tapping a day inside the current streak asks to
 * record a relapse there.
 *
 * Only days from the start of the current streak up to today can be tapped: earlier days
 * belong to streaks already closed in the history table, and rewriting those would mean
 * editing records the app deliberately never mutates.
 */
@Composable
fun CalendarGrid(
    monthAnchor: Calendar,
    coveredRanges: List<LongRange>,
    relapseDays: Set<Long>,
    currentStreakStartDay: Long,
    onDayClick: (Long) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthLabel = remember(monthAnchor.timeInMillis) {
        SimpleDateFormat("MMMM yyyy", Locale("es")).format(monthAnchor.time).replaceFirstChar { it.uppercase() }
    }

    // Las celdas del mes se calculan una vez por mes, no en cada recomposición: así no se
    // crea un Calendar por casilla cada vez que la pantalla se redibuja.
    val month = remember(monthAnchor.timeInMillis) {
        val first = (monthAnchor.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val startOffset = first.get(Calendar.DAY_OF_WEEK) - 1 // domingo = 1 -> offset 0
        val daysInMonth = monthAnchor.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cursor = first.clone() as Calendar
        val cells = (1..daysInMonth).map { day ->
            val cell = DayCell(day, cursor.timeInMillis)
            cursor.add(Calendar.DAY_OF_MONTH, 1)
            cell
        }
        startOffset to cells
    }
    val (startOffset, cells) = month
    val todayStart = dayStartOf(System.currentTimeMillis())
    val coveredInMonth = cells.count { cell -> cell.dayStart <= todayStart && coveredRanges.any { cell.dayStart in it } }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevMonth) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Mes anterior") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(monthLabel, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (coveredInMonth == 1) "1 día cumplido" else "$coveredInMonth días cumplidos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onNextMonth) { Icon(Icons.Filled.ChevronRight, contentDescription = "Mes siguiente") }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            listOf("dom", "lun", "mar", "mié", "jue", "vie", "sáb").forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val rows = (startOffset + cells.size + 6) / 7
        repeat(rows) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = rowIndex * 7 + col
                    val dayIndex = cellIndex - startOffset
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        cells.getOrNull(dayIndex)?.let { cell ->
                            DayBubble(
                                cell = cell,
                                isCovered = coveredRanges.any { cell.dayStart in it },
                                isRelapse = cell.dayStart in relapseDays,
                                isToday = cell.dayStart == todayStart,
                                isFuture = cell.dayStart > todayStart,
                                canMark = cell.dayStart in currentStreakStartDay..todayStart,
                                onClick = { onDayClick(cell.dayStart) }
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), MaterialTheme.colorScheme.primary, "Cumplido")
            LegendItem(MaterialTheme.colorScheme.error.copy(alpha = 0.22f), MaterialTheme.colorScheme.error, "Recaída")
            LegendItem(Color.Transparent, MaterialTheme.colorScheme.primary, "Hoy")
        }
        Text(
            "Toca un día de tu racha actual para registrar una recaída en esa fecha.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun DayBubble(
    cell: DayCell,
    isCovered: Boolean,
    isRelapse: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    canMark: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val fill = when {
        isRelapse -> scheme.error.copy(alpha = 0.22f)
        isCovered -> scheme.primary.copy(alpha = 0.22f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isToday -> scheme.primary
        isRelapse -> scheme.error.copy(alpha = 0.7f)
        else -> Color.Transparent
    }
    val textColor = when {
        isFuture -> scheme.onSurfaceVariant.copy(alpha = 0.45f)
        isRelapse -> scheme.error
        isCovered -> scheme.onSurface
        else -> scheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .padding(3.dp)
            .size(36.dp)
            .background(fill, CircleShape)
            .border(if (isToday) 2.dp else 1.5.dp, borderColor, CircleShape)
            .clickable(enabled = canMark, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            cell.dayOfMonth.toString(),
            color = textColor,
            fontWeight = if (isToday || isRelapse) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LegendItem(fill: Color, border: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(fill, CircleShape)
                .border(1.5.dp, border, CircleShape)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
