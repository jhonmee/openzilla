package com.openzilla.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HistoryEntity
import com.openzilla.app.ui.components.CalendarGrid
import com.openzilla.app.ui.components.ConfirmDialog
import com.openzilla.app.ui.components.ProgressFlameBar
import com.openzilla.app.util.buildHabitDayMap
import com.openzilla.app.util.currentGoalHours
import com.openzilla.app.util.dayStartOf
import com.openzilla.app.util.elapsedParts
import com.openzilla.app.util.formatDurationShort
import com.openzilla.app.util.goalLabel
import com.openzilla.app.util.goalPercentText
import com.openzilla.app.util.goalProgress
import com.openzilla.app.util.goalRemainingMillis
import com.openzilla.app.util.middayOf
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SummaryTab(
    habit: HabitEntity,
    history: List<HistoryEntity>,
    onResetRequested: () -> Unit,
    onRelapseAt: (Long) -> Unit
) {
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
    var pendingRelapseDay by remember { mutableStateOf<Long?>(null) }
    val parts = elapsedParts(habit.startedAt, now)

    // El mapa de días depende de los datos guardados y del día en curso, no del reloj: no se
    // recalcula con cada tic del contador, sólo al cambiar el hábito, su historial o la fecha
    // (para que el calendario se ponga al día si la app se queda abierta pasada medianoche).
    val todayStart = dayStartOf(now)
    val dayMap = remember(habit.id, habit.startedAt, history, todayStart) {
        buildHabitDayMap(habit.startedAt, history.map { it.streakStart to it.streakEnd }, now)
    }
    val dateFormat = remember { SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es")) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Tiempo sin recaídas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    buildString {
                        if (parts.days > 0) append("${parts.days}d ")
                        append("${parts.hours}h ${parts.minutes}m ${parts.seconds}s")
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                ProgressFlameBar(progress = goalProgress(habit.startedAt, habit.goalHours, now))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Meta: ${goalLabel(currentGoalHours(habit.startedAt, habit.goalHours, now))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${goalPercentText(habit.startedAt, habit.goalHours, now)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    goalRemainingMillis(habit.startedAt, habit.goalHours, now)
                        ?.let { "Te faltan ${formatDurationShort(it)}" }
                        ?: "¡Meta máxima alcanzada!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                OutlinedButton(onClick = onResetRequested, modifier = Modifier.padding(top = 20.dp)) {
                    Text("Registrar recaída ahora")
                }
            }
        }
        item {
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            CalendarGrid(
                monthAnchor = monthAnchor,
                coveredRanges = dayMap.coveredRanges,
                relapseDays = dayMap.relapseDays,
                currentStreakStartDay = dayMap.currentStreakStartDay,
                onDayClick = { pendingRelapseDay = it },
                onPrevMonth = { monthAnchor = (monthAnchor.clone() as Calendar).apply { add(Calendar.MONTH, -1) } },
                onNextMonth = { monthAnchor = (monthAnchor.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }
            )
        }
    }

    pendingRelapseDay?.let { day ->
        ConfirmDialog(
            title = "Registrar recaída",
            message = "Se marcará una recaída el ${dateFormat.format(Date(day))}. La racha actual se guardará en el historial y el contador arrancará desde ese día.",
            confirmLabel = "Registrar",
            onConfirm = { onRelapseAt(middayOf(day)); pendingRelapseDay = null },
            onDismiss = { pendingRelapseDay = null }
        )
    }
}
