package com.openzilla.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.ui.components.ConfirmDialog
import com.openzilla.app.ui.rememberOpenZillaViewModel
import com.openzilla.app.util.HabitCategory
import com.openzilla.app.util.formatElapsedShort
import com.openzilla.app.util.goalProgress
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddHabit: () -> Unit,
    onOpenHabit: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val viewModel = rememberOpenZillaViewModel { HomeViewModel(it.repository) }
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<HabitEntity?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenZilla") },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHabit) { Icon(Icons.Filled.Add, contentDescription = "Añadir hábito") }
        }
    ) { padding ->
        if (habits.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Toca + para registrar el primer hábito que quieres dejar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                items(habits, key = { it.id }) { habit ->
                    HabitCard(
                        habit = habit,
                        onClick = { onOpenHabit(habit.id) },
                        onReset = { viewModel.resetHabit(habit) { error = it } },
                        onDelete = { pendingDelete = habit }
                    )
                }
            }
        }
    }

    pendingDelete?.let { habit ->
        ConfirmDialog(
            title = "Eliminar \"${habit.name}\"",
            message = "Se borrará este hábito y todo su historial de forma permanente. Esta acción no se puede deshacer.",
            onConfirm = {
                viewModel.deleteHabit(habit) { error = it }
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    error?.let { msg ->
        ConfirmDialog(
            title = "Ocurrió un problema",
            message = msg,
            confirmLabel = "Cerrar",
            onConfirm = { error = null },
            onDismiss = { error = null }
        )
    }
}

@Composable
private fun HabitCard(habit: HabitEntity, onClick: () -> Unit, onReset: () -> Unit, onDelete: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(habit.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000) // list view doesn't need per-second precision — saves battery/CPU
        }
    }
    var menuOpen by remember { mutableStateOf(false) }
    val category = HabitCategory.iconFor(habit.iconKey)

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(category, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(habit.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Reiniciar contador") }, onClick = { menuOpen = false; onReset() })
                        DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menuOpen = false; onDelete() })
                    }
                }
            }
            Text(
                "Sin recaídas desde hace ${formatElapsedShort(habit.startedAt, now)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            LinearProgressIndicator(
                progress = { goalProgress(habit.startedAt, habit.goalHours, now) },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        }
    }
}
