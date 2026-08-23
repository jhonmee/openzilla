package com.openzilla.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.ui.components.CalendarGrid
import com.openzilla.app.ui.components.GaugeRing
import com.openzilla.app.util.elapsedParts
import com.openzilla.app.util.goalPercentText
import com.openzilla.app.util.goalProgress
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun SummaryTab(habit: HabitEntity, onResetRequested: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // Ticks every second only while this tab is actually on screen — LaunchedEffect is
    // cancelled automatically when the composable leaves composition, so there is nothing
    // left running (and nothing to leak) once the user navigates away.
    LaunchedEffect(habit.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    var monthAnchor by remember { mutableStateOf(Calendar.getInstance()) }
    val parts = elapsedParts(habit.startedAt, now)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                GaugeRing(progress = goalProgress(habit.startedAt, habit.goalHours, now), label = "${goalPercentText(habit.startedAt, habit.goalHours, now)}%")
                Text("Meta de ${habit.goalHours} horas", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Tiempo sin recaídas", style = MaterialTheme.typography.bodyMedium)
                Text(
                    buildString {
                        if (parts.days > 0) append("${parts.days}d ")
                        append("${parts.hours}h ${parts.minutes}m ${parts.seconds}s")
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                OutlinedButton(onClick = onResetRequested) { Text("Registrar recaída / reiniciar") }
            }
        }
        item {
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            CalendarGrid(
                monthAnchor = monthAnchor,
                streakStartMillis = habit.startedAt,
                onPrevMonth = { monthAnchor = (monthAnchor.clone() as Calendar).apply { add(Calendar.MONTH, -1) } },
                onNextMonth = { monthAnchor = (monthAnchor.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }
            )
        }
    }
}
