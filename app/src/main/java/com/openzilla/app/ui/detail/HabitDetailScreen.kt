package com.openzilla.app.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.openzilla.app.ui.components.ConfirmDialog
import com.openzilla.app.ui.rememberOpenZillaViewModel
import com.openzilla.app.ui.rememberHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: Long,
    currencySymbol: String,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val viewModel = rememberOpenZillaViewModel { HabitDetailViewModel(it.repository, habitId) }
    val habit by viewModel.habit.collectAsStateWithLifecycle()
    val reasons by viewModel.reasons.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val error by viewModel.errors.collectAsStateWithLifecycle()

    val haptics = rememberHaptics()
    var tab by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    val current = habit
    if (current == null) {
        // Con la caché del repositorio esto casi nunca llega a verse viniendo de la lista. Si
        // aun así la lectura tardara, el indicador espera un cuarto de segundo antes de
        // aparecer: una carga instantánea no debe producir un parpadeo de "cargando".
        var showSpinner by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(250)
            showSpinner = true
        }
        Scaffold(topBar = { TopAppBar(title = { Text("") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás") } }) }) { padding ->
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (showSpinner) CircularProgressIndicator()
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current.name) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás") } },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Editar") }, onClick = { menuOpen = false; onEdit(habitId) })
                        DropdownMenuItem(text = { Text("Reiniciar contador") }, onClick = { menuOpen = false; confirmReset = true })
                        DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menuOpen = false; confirmDelete = true })
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { haptics.tap(); tab = 0 }, icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) }, label = { Text("Resumen") })
                NavigationBarItem(selected = tab == 1, onClick = { haptics.tap(); tab = 1 }, icon = { Icon(Icons.Filled.Lightbulb, contentDescription = null) }, label = { Text("Motivación") })
                NavigationBarItem(selected = tab == 2, onClick = { haptics.tap(); tab = 2 }, icon = { Icon(Icons.Filled.ShowChart, contentDescription = null) }, label = { Text("Progreso") })
                NavigationBarItem(selected = tab == 3, onClick = { haptics.tap(); tab = 3 }, icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) }, label = { Text("Trofeos") })
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> SummaryTab(
                    habit = current,
                    history = history,
                    onResetRequested = { confirmReset = true },
                    onRelapseAt = viewModel::registerRelapseAt
                )
                1 -> MotivationTab(habit = current, reasons = reasons, currencySymbol = currencySymbol, onAddReason = viewModel::addReason, onDeleteReason = viewModel::deleteReason)
                2 -> StatsTab(habit = current, history = history)
                3 -> TrophiesTab(current)
            }
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reiniciar contador",
            message = "Se guardará tu racha actual en el historial y el contador volverá a empezar desde ahora. Esto no borra tus datos anteriores.",
            confirmLabel = "Reiniciar",
            onConfirm = { haptics.confirm(); viewModel.resetHabit(); confirmReset = false },
            onDismiss = { confirmReset = false }
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Eliminar \"${current.name}\"",
            message = "Se borrará este hábito y todo su historial de forma permanente. Esta acción no se puede deshacer.",
            onConfirm = { haptics.confirm(); viewModel.deleteHabit(onDeleted); confirmDelete = false },
            onDismiss = { confirmDelete = false }
        )
    }
    error?.let { msg ->
        ConfirmDialog(title = "Ocurrió un problema", message = msg, confirmLabel = "Cerrar", onConfirm = viewModel::clearError, onDismiss = viewModel::clearError)
    }
}
