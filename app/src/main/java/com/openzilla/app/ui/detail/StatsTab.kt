package com.openzilla.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HistoryEntity
import com.openzilla.app.ui.components.rememberNowTicker
import com.openzilla.app.util.formatDurationShort
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_BARS = 8
private val CHART_HEIGHT = 140.dp

private data class Bar(val label: String, val millis: Long, val isCurrent: Boolean)

@Composable
fun StatsTab(habit: HabitEntity, history: List<HistoryEntity>) {
    // Aquí las cifras se muestran en días y horas, así que medio minuto sobra; el reloj
    // compartido se encarga además de refrescar al volver de segundo plano.
    val now by rememberNowTicker(intervalMillis = 30_000L)

    val currentStreak = (now - habit.startedAt).coerceAtLeast(0)
    val pastStreaks = history.map { (it.streakEnd - it.streakStart).coerceAtLeast(0) }
    val longest = maxOf(currentStreak, pastStreaks.maxOrNull() ?: 0L)
    val average = if (pastStreaks.isEmpty()) currentStreak else (pastStreaks.sum() + currentStreak) / (pastStreaks.size + 1)

    val dayFormat = remember { SimpleDateFormat("d/M", Locale("es")) }
    val fullFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale("es")) }

    val bars = remember(history, currentStreak) {
        history
            .sortedBy { it.streakEnd }
            .takeLast(MAX_BARS)
            .map { Bar(dayFormat.format(Date(it.streakEnd)), (it.streakEnd - it.streakStart).coerceAtLeast(0), false) } +
            Bar("Ahora", currentStreak, true)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatBox(Icons.Filled.LocalFireDepartment, "Racha actual", formatDurationShort(currentStreak), Modifier.weight(1f), highlight = true)
                StatBox(Icons.Filled.EmojiEvents, "Mejor racha", formatDurationShort(longest), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                StatBox(Icons.Filled.Replay, "Recaídas", history.size.toString(), Modifier.weight(1f))
                StatBox(Icons.Filled.Timeline, "Racha media", formatDurationShort(average), Modifier.weight(1f))
            }
        }

        item {
            Text(
                "Historial de rachas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            )
            Text(
                "Cada barra es una racha completa; la última es la que está en marcha.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        item { StreakBarChart(bars) }

        if (history.isNotEmpty()) {
            item {
                Text(
                    "Recaídas registradas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 28.dp, bottom = 8.dp)
                )
            }
            items(history.size) { index ->
                val entry = history[index]
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(fullFormat.format(Date(entry.streakEnd)), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "aguantaste ${formatDurationShort(entry.streakEnd - entry.streakStart)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun StatBox(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Plain bars laid out with normal Compose layout instead of a Canvas: each bar is a rounded
 * Box whose height is a fraction of the tallest one, so labels and colors come for free and
 * there is no custom drawing to keep in sync with the theme.
 */
@Composable
private fun StreakBarChart(bars: List<Bar>) {
    val maxValue = (bars.maxOfOrNull { it.millis } ?: 0L).coerceAtLeast(1L)
    val past = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val current = MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bars.forEach { bar ->
                val fraction = (bar.millis.toFloat() / maxValue.toFloat()).coerceIn(0.03f, 1f)
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatDurationShort(bar.millis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CHART_HEIGHT * fraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (bar.isCurrent) current else past)
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bars.forEach { bar ->
                Text(
                    bar.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bar.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
