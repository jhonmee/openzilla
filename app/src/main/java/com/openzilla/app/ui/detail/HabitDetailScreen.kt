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
import androidx.compose.ui.res.stringResource
import com.openzilla.app.R
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
    val viewModel = rememberOpenZillaViewModel { HabitDetailViewModel(it, it.repository, habitId) }
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
        Scaffold(topBar = { TopAppBar(title = { Text("") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) } }) }) { padding ->
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
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more_options)) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_edit)) }, onClick = { menuOpen = false; onEdit(habitId) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.reset_title)) }, onClick = { menuOpen = false; confirmReset = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = { menuOpen = false; confirmDelete = true })
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { haptics.tap(); tab = 0 }, icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) }, label = { Text(stringResource(R.string.tab_summary)) })
                NavigationBarItem(selected = tab == 1, onClick = { haptics.tap(); tab = 1 }, icon = { Icon(Icons.Filled.Lightbulb, contentDescription = null) }, label = { Text(stringResource(R.string.tab_motivation)) })
                NavigationBarItem(selected = tab == 2, onClick = { haptics.tap(); tab = 2 }, icon = { Icon(Icons.Filled.ShowChart, contentDescription = null) }, label = { Text(stringResource(R.string.tab_progress)) })
                NavigationBarItem(selected = tab == 3, onClick = { haptics.tap(); tab = 3 }, icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) }, label = { Text(stringResource(R.string.tab_trophies)) })
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
                2 -> StatsTab(habit = current, history = history, currencySymbol = currencySymbol)
                3 -> TrophiesTab(current)
            }
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = stringResource(R.string.reset_title),
            message = stringResource(R.string.reset_message),
            confirmLabel = stringResource(R.string.reset_confirm),
            onConfirm = { haptics.confirm(); viewModel.resetHabit(); confirmReset = false },
            onDismiss = { confirmReset = false }
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_habit_title, current.name),
            message = stringResource(R.string.delete_habit_message),
            onConfirm = { haptics.confirm(); viewModel.deleteHabit(onDeleted); confirmDelete = false },
            onDismiss = { confirmDelete = false }
        )
    }
    error?.let { msg ->
        ConfirmDialog(title = stringResource(R.string.error_title), message = msg, confirmLabel = stringResource(R.string.action_close), onConfirm = viewModel::clearError, onDismiss = viewModel::clearError)
    }
}
