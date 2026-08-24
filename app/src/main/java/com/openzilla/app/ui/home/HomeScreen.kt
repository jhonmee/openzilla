package com.openzilla.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openzilla.app.R
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.ui.components.ConfirmDialog
import com.openzilla.app.ui.components.ProgressWaveBar
import com.openzilla.app.ui.components.rememberNowTicker
import com.openzilla.app.ui.components.rememberReorderState
import com.openzilla.app.ui.components.reorderableItem
import com.openzilla.app.ui.goalLabel
import com.openzilla.app.ui.rememberHaptics
import com.openzilla.app.ui.rememberOpenZillaViewModel
import com.openzilla.app.util.HabitCategory
import com.openzilla.app.util.currentGoalHours
import com.openzilla.app.util.formatElapsedShort
import com.openzilla.app.util.goalPercentText
import com.openzilla.app.util.goalProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddHabit: () -> Unit,
    onOpenHabit: (Long) -> Unit,
    onOpenGarden: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val viewModel = rememberOpenZillaViewModel { HomeViewModel(it, it.repository) }
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<HabitEntity?>(null) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val haptics = rememberHaptics()
    val listState = rememberLazyListState()

    // Un solo reloj para toda la lista, en vez de una corrutina por tarjeta. Se guarda el
    // State sin leerlo aquí: quien lo lee es cada tarjeta, así el tic de cada segundo
    // recompone las tarjetas y no toda la pantalla.
    val nowState = rememberNowTicker()

    // Selección múltiple. Es un modo aparte a propósito: mientras está activo, arrastrar
    // queda desactivado, así los dos gestos nunca compiten por la misma pulsación larga.
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val selectionMode = selected.isNotEmpty()

    // Copia local sobre la que se reordena en vivo. Mientras se arrastra no se pisa con lo
    // que llega de la base de datos; al soltar se guarda y la base de datos vuelve a mandar.
    var ordered by remember { mutableStateOf(habits) }

    val reorderState = rememberReorderState(
        listState = listState,
        onMove = { from, to ->
            ordered = ordered?.toMutableList()?.apply { add(to, removeAt(from)) }
        },
        onDropped = { viewModel.saveOrder(ordered.orEmpty().map { it.id }) { message -> error = message } },
        onLifted = { haptics.longPress() },
        onSwapped = { haptics.tick() }
    )

    // Ojo con la clave: si esto también reaccionara al soltar, la lista parpadearía al orden
    // viejo durante el instante que tarda la base de datos en emitir el nuevo.
    LaunchedEffect(habits) {
        if (reorderState.draggingKey == null) ordered = habits
    }

    // Un hábito borrado desde otro sitio no debe quedarse marcado en la selección.
    LaunchedEffect(habits) {
        val alive = habits?.map { it.id }?.toSet() ?: return@LaunchedEffect
        if (selected.isNotEmpty()) selected = selected intersect alive
    }

    BackHandler(enabled = selectionMode) { selected = emptySet() }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text(stringResource(R.string.selection_count, selected.size)) },
                    navigationIcon = {
                        IconButton(onClick = { haptics.tap(); selected = emptySet() }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.selection_exit))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            haptics.tap()
                            selected = ordered.orEmpty().map { it.id }.toSet()
                        }) {
                            Icon(Icons.Filled.DoneAll, contentDescription = stringResource(R.string.select_all))
                        }
                        IconButton(onClick = { haptics.tap(); confirmDeleteSelected = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    )
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = { haptics.tap(); onOpenGarden() }) {
                            Icon(Icons.Filled.Park, contentDescription = stringResource(R.string.home_garden))
                        }
                        IconButton(onClick = { haptics.tap(); onOpenStats() }) {
                            Icon(Icons.Filled.QueryStats, contentDescription = stringResource(R.string.home_stats))
                        }
                        IconButton(onClick = { haptics.tap(); onOpenSettings() }) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.home_settings))
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                // Colores explícitos: por defecto el FAB usa primaryContainer, que sin definir
                // se quedaba en el morado base de Material en vez de seguir al acento elegido.
                FloatingActionButton(
                    onClick = { haptics.tap(); onAddHabit() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.home_add_habit))
                }
            }
        }
    ) { padding ->
        val current = ordered
        when {
            // Todavía no ha llegado nada: se deja el fondo limpio. Dura un par de frames y es
            // preferible a enseñar el mensaje de "no hay hábitos" a quien sí los tiene.
            current == null -> Box(Modifier.fillMaxSize().padding(padding))

            current.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.home_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                itemsIndexed(current, key = { _, habit -> habit.id }) { index, habit ->
                    val isDragged = reorderState.draggingKey == habit.id
                    Box(
                        modifier = Modifier
                            // La tarjeta agarrada la coloca el dedo; las demás sí animan su
                            // hueco al apartarse, que es lo que hace legible el movimiento.
                            .then(if (isDragged) Modifier else Modifier.animateItem())
                            // Sin arrastre mientras se seleccionan varios: es lo que evita que
                            // una pulsación larga signifique dos cosas a la vez.
                            .then(
                                if (selectionMode) Modifier
                                else Modifier.reorderableItem(reorderState, habit.id, index)
                            )
                    ) {
                        HabitCard(
                            habit = habit,
                            nowState = nowState,
                            lifted = isDragged,
                            selectionMode = selectionMode,
                            isSelected = habit.id in selected,
                            onClick = {
                                // Al soltar tras arrastrar llega también un "toque"; se ignora
                                // para no abrir el hábito que se acaba de mover.
                                if (reorderState.shouldIgnoreClick()) return@HabitCard
                                haptics.tap()
                                if (selectionMode) {
                                    selected = if (habit.id in selected) selected - habit.id else selected + habit.id
                                } else {
                                    onOpenHabit(habit.id)
                                }
                            },
                            onStartSelection = { haptics.longPress(); selected = setOf(habit.id) },
                            onReset = { haptics.confirm(); viewModel.resetHabit(habit) { error = it } },
                            onDelete = { haptics.tap(); pendingDelete = habit }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { habit ->
        ConfirmDialog(
            title = stringResource(R.string.delete_habit_title, habit.name),
            message = stringResource(R.string.delete_habit_message),
            onConfirm = {
                haptics.confirm()
                viewModel.deleteHabit(habit) { error = it }
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    if (confirmDeleteSelected) {
        ConfirmDialog(
            title = stringResource(R.string.selection_delete_title, selected.size),
            message = stringResource(R.string.selection_delete_message),
            onConfirm = {
                haptics.confirm()
                viewModel.deleteHabits(selected) { error = it }
                selected = emptySet()
                confirmDeleteSelected = false
            },
            onDismiss = { confirmDeleteSelected = false }
        )
    }

    error?.let { msg ->
        ConfirmDialog(
            title = stringResource(R.string.error_title),
            message = msg,
            confirmLabel = stringResource(R.string.action_close),
            onConfirm = { error = null },
            onDismiss = { error = null }
        )
    }
}

@Composable
private fun HabitCard(
    habit: HabitEntity,
    nowState: State<Long>,
    lifted: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onStartSelection: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    val now = nowState.value
    var menuOpen by remember { mutableStateOf(false) }
    val category = HabitCategory.iconFor(habit.iconKey)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = if (lifted) 8.dp else 1.dp),
        // Sólo se tocan los colores cuando la tarjeta está marcada; el resto del tiempo se
        // deja el valor por defecto tal cual, sin copiarlo a mano.
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(category, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(habit.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                }
                if (selectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                } else {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more_options)) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.select_action)) }, onClick = { menuOpen = false; onStartSelection() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.home_reset_counter)) }, onClick = { menuOpen = false; onReset() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = { menuOpen = false; onDelete() })
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.home_since, formatElapsedShort(habit.startedAt, now)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            ProgressWaveBar(
                progress = goalProgress(habit.startedAt, habit.goalHours, now),
                barHeight = 10.dp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.home_goal, goalLabel(currentGoalHours(habit.startedAt, habit.goalHours, now))),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${goalPercentText(habit.startedAt, habit.goalHours, now)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
