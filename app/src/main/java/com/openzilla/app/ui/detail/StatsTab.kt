package com.openzilla.app.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HistoryEntity
import com.openzilla.app.util.formatElapsedShort

@Composable
fun StatsTab(habit: HabitEntity, history: List<HistoryEntity>) {
    val now = System.currentTimeMillis()
    val currentStreak = now - habit.startedAt
    val longestPast = history.maxOfOrNull { it.streakEnd - it.streakStart } ?: 0L
    val longest = maxOf(currentStreak, longestPast)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                StatBox("Racha actual", formatElapsedShort(habit.startedAt, now))
                StatBox("Mejor racha", formatElapsedShort(now - longest, now))
                StatBox("Recaídas", history.size.toString())
            }
        }
        item {
            Text("Historial de rachas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        }
        if (history.isEmpty()) {
            item { Text("Todavía no hay rachas anteriores registradas.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            item {
                StreakBarChart(history.takeLast(12).map { it.streakEnd - it.streakStart })
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StreakBarChart(streaksMillis: List<Long>) {
    val maxValue = (streaksMillis.maxOrNull() ?: 1L).coerceAtLeast(1L)
    val barColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val barWidth = size.width / (streaksMillis.size * 1.5f)
        streaksMillis.forEachIndexed { index, value ->
            val barHeight = (value.toFloat() / maxValue.toFloat()) * size.height
            val x = index * barWidth * 1.5f
            drawRect(
                color = barColor,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
}
