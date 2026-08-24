package com.openzilla.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.openzilla.app.R
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.openzilla.app.ui.goalLabel
import com.openzilla.app.ui.rememberHaptics
import com.openzilla.app.ui.components.ProgressWaveBar
import com.openzilla.app.ui.components.RelapseTimeDialog
import com.openzilla.app.ui.components.rememberNowTicker
import com.openzilla.app.util.buildHabitDayMap
import com.openzilla.app.util.currentGoalHours
import com.openzilla.app.util.dayStartOf
import com.openzilla.app.util.elapsedParts
import com.openzilla.app.util.formatDurationShort
import com.openzilla.app.util.goalPercentText
import com.openzilla.app.util.goalProgress
import com.openzilla.app.util.goalRemainingMillis
import java.util.Calendar

/** Valor centinela: no es un día concreto, hay que preguntar también la fecha. */
private const val ANY_MOMENT = -1L

@Composable
fun SummaryTab(
    habit: HabitEntity,
    history: List<HistoryEntity>,
    onResetRequested: () -> Unit,
    onRelapseAt: (Long) -> Unit
) {
    // Reloj compartido: se para al irse la app a segundo plano y publica la hora nueva nada
    // más volver, así el contador nunca se queda con un valor viejo en pantalla. Se lee dentro
    // del item, no aquí, para que el tic no recomponga también el calendario.
    val nowState = rememberNowTicker()
    val haptics = rememberHaptics()
    var monthAnchor by remember { mutableStateOf(Calendar.getInstance()) }
    // El día elegido en el calendario, o ANY_MOMENT cuando se pulsa el botón y hay que
    // preguntar también la fecha.
    var pickingRelapseFor by remember { mutableStateOf<Long?>(null) }


    // El mapa de días depende de los datos guardados y del día en curso, no del reloj. El
    // derivedStateOf es la pieza clave: mira el reloj cada segundo, pero sólo avisa cuando
    // cambia el día, así que el calendario se pone al día si la app se queda abierta pasada
    // medianoche sin recomponerse en cada tic.
    val todayStart by remember { derivedStateOf { dayStartOf(nowState.value) } }
    val dayMap = remember(habit.id, habit.startedAt, history, todayStart) {
        buildHabitDayMap(habit.startedAt, history.map { it.streakStart to it.streakEnd }, todayStart)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            val now = nowState.value
            val parts = elapsedParts(habit.startedAt, now)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.summary_time_clean), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    buildString {
                        if (parts.days > 0) append("${parts.days}d ")
                        append("${parts.hours}h ${parts.minutes}m ${parts.seconds}s")
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                ProgressWaveBar(progress = goalProgress(habit.startedAt, habit.goalHours, now))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.home_goal, goalLabel(currentGoalHours(habit.startedAt, habit.goalHours, now))),
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
                        ?.let { stringResource(R.string.summary_remaining, formatDurationShort(it)) }
                        ?: stringResource(R.string.summary_max_goal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                OutlinedButton(onClick = { haptics.tap(); pickingRelapseFor = ANY_MOMENT }, modifier = Modifier.padding(top = 20.dp)) {
                    Text(stringResource(R.string.summary_register_relapse))
                }
            }
        }
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            CalendarGrid(
                monthAnchor = monthAnchor,
                coveredRanges = dayMap.coveredRanges,
                relapseDays = dayMap.relapseDays,
                currentStreakStartDay = dayMap.currentStreakStartDay,
                onDayClick = { haptics.tap(); pickingRelapseFor = it },
                onPrevMonth = { monthAnchor = (monthAnchor.clone() as Calendar).apply { add(Calendar.MONTH, -1) } },
                onNextMonth = { monthAnchor = (monthAnchor.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }
            )
        }
    }

    pickingRelapseFor?.let { day ->
        RelapseTimeDialog(
            dayStart = day.takeIf { it != ANY_MOMENT },
            onDismiss = { pickingRelapseFor = null },
            onPicked = { moment ->
                pickingRelapseFor = null
                haptics.confirm()
                onRelapseAt(moment)
            }
        )
    }
}
