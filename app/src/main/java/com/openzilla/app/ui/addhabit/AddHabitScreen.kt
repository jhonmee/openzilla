package com.openzilla.app.ui.addhabit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.openzilla.app.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openzilla.app.data.HabitCostType
import com.openzilla.app.ui.components.ConfirmDialog
import com.openzilla.app.ui.rememberOpenZillaViewModel
import com.openzilla.app.ui.goalLabel
import com.openzilla.app.ui.rememberHaptics
import com.openzilla.app.util.HabitCategory
import com.openzilla.app.util.SELECTABLE_GOALS
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class WizardStep(val titleRes: Int) {
    NAME_ICON(R.string.wizard_name_icon),
    TYPE(R.string.wizard_type),
    DATE(R.string.wizard_date),
    GOAL(R.string.wizard_goal)
}

/**
 * @param editingHabitId null starts the "create new" flow (category picker first);
 *   non-null jumps straight into editing that habit's name/icon/type/meta (no date step —
 *   changing "when did this last happen" is a separate, explicit "reset" action elsewhere).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(editingHabitId: Long?, onDone: () -> Unit, onCancel: () -> Unit) {
    val viewModel = rememberOpenZillaViewModel { AddHabitViewModel(it, it.repository, editingHabitId) }
    val haptics = rememberHaptics()
    var showCategoryPicker by remember { mutableStateOf(editingHabitId == null) }
    var stepIndex by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    val steps = remember(viewModel.isEditing) {
        if (viewModel.isEditing) {
            listOf(WizardStep.NAME_ICON, WizardStep.TYPE, WizardStep.GOAL)
        } else {
            listOf(WizardStep.NAME_ICON, WizardStep.TYPE, WizardStep.DATE, WizardStep.GOAL)
        }
    }
    val step = steps[stepIndex.coerceIn(0, steps.lastIndex)]
    val isLastStep = stepIndex >= steps.lastIndex

    if (showCategoryPicker) {
        CategoryPickerScreen(
            onBack = onCancel,
            onPick = { category, categoryLabel ->
                viewModel.setIcon(category.key)
                if (viewModel.state.name.isBlank()) viewModel.setName(categoryLabel)
                viewModel.setCostType(category.suggestedType)
                showCategoryPicker = false
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing && step == WizardStep.NAME_ICON) stringResource(R.string.wizard_edit) else stringResource(step.titleRes)) },
                navigationIcon = {
                    // Al crear un hábito, "atrás" desde el primer paso vuelve a la lista de
                    // categorías (de donde se venía), no cierra el asistente entero.
                    IconButton(onClick = {
                        when {
                            stepIndex > 0 -> stepIndex--
                            !viewModel.isEditing -> showCategoryPicker = true
                            else -> onCancel()
                        }
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.wizard_step, stepIndex + 1, steps.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = {
                    haptics.tap()
                    if (!isLastStep) {
                        stepIndex++
                    } else {
                        viewModel.save(onDone = onDone, onError = { error = it })
                    }
                }) {
                    Text(stringResource(if (!isLastStep) R.string.action_next else if (viewModel.isEditing) R.string.action_save else R.string.action_finish))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (step) {
                WizardStep.NAME_ICON -> NameIconStep(viewModel)
                WizardStep.TYPE -> TypeStep(viewModel)
                WizardStep.DATE -> DateStep(viewModel)
                WizardStep.GOAL -> GoalStep(viewModel)
            }
        }
    }

    error?.let {
        ConfirmDialog(title = stringResource(R.string.wizard_missing_title), message = it, confirmLabel = stringResource(R.string.action_understood), onConfirm = { error = null }, onDismiss = { error = null })
    }
}

@Composable
private fun NameIconStep(viewModel: AddHabitViewModel) {
    var showIconPicker by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.size(96.dp).padding(bottom = 16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            IconButton(onClick = { showIconPicker = true }, modifier = Modifier.fillMaxSize()) {
                Icon(HabitCategory.iconFor(viewModel.state.iconKey), contentDescription = stringResource(R.string.wizard_change_icon), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            }
        }
        Text(stringResource(R.string.wizard_question), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = viewModel.state.name,
            onValueChange = viewModel::setName,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            singleLine = true,
            label = { Text(stringResource(R.string.wizard_name_label)) }
        )
    }

    if (showIconPicker) {
        IconPickerDialog(onDismiss = { showIconPicker = false }, onPick = { key -> viewModel.setIcon(key); showIconPicker = false })
    }
}

@Composable
private fun IconPickerDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
        title = { Text(stringResource(R.string.wizard_pick_icon)) },
        text = {
            LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.size(280.dp)) {
                items(HabitCategory.iconChoices) { (key, icon) ->
                    IconButton(onClick = { onPick(key) }) {
                        Icon(icon, contentDescription = key, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    )
}

@Composable
private fun TypeStep(viewModel: AddHabitViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.wizard_select_type), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
        TypeOption(
            selected = viewModel.state.costType == HabitCostType.MONEY,
            title = stringResource(R.string.type_money),
            description = stringResource(R.string.type_money_desc),
            onSelect = { viewModel.setCostType(HabitCostType.MONEY) }
        ) {
            if (viewModel.state.costType == HabitCostType.MONEY) {
                OutlinedTextField(
                    value = viewModel.state.weeklyAmountText,
                    onValueChange = viewModel::setWeeklyAmount,
                    label = { Text(stringResource(R.string.type_money_question)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true
                )
            }
        }
        TypeOption(
            selected = viewModel.state.costType == HabitCostType.TIME,
            title = stringResource(R.string.type_time),
            description = stringResource(R.string.type_time_desc),
            onSelect = { viewModel.setCostType(HabitCostType.TIME) }
        ) {
            if (viewModel.state.costType == HabitCostType.TIME) {
                OutlinedTextField(
                    value = viewModel.state.weeklyAmountText,
                    onValueChange = viewModel::setWeeklyAmount,
                    label = { Text(stringResource(R.string.type_time_question)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true
                )
            }
        }
        TypeOption(
            selected = viewModel.state.costType == HabitCostType.EVENT,
            title = stringResource(R.string.type_event),
            description = stringResource(R.string.type_event_desc),
            onSelect = { viewModel.setCostType(HabitCostType.EVENT) },
            extra = null
        )
    }
}

@Composable
private fun TypeOption(selected: Boolean, title: String, description: String, onSelect: () -> Unit, extra: (@Composable () -> Unit)? = { }) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                RadioButton(selected = selected, onClick = onSelect)
            }
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            extra?.invoke()
        }
    }
}

@Composable
private fun DateStep(viewModel: AddHabitViewModel) {
    val context = LocalContext.current
    val formatter = remember { SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.wizard_when_last), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
        OutlinedButton(onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = viewModel.state.startedAt }
            DatePickerDialog(context, { _, year, month, day ->
                cal.set(year, month, day)
                TimePickerDialog(context, { _, hour, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    val chosen = cal.timeInMillis
                    // Never allow a future timestamp — it would make progress calculations negative/wrong.
                    viewModel.setStartedAt(chosen.coerceAtMost(System.currentTimeMillis()))
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }) {
            Text(formatter.format(Date(viewModel.state.startedAt)))
        }
        Text(
            stringResource(R.string.wizard_counter_starts),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun GoalStep(viewModel: AddHabitViewModel) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.wizard_first_goal), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.wizard_goal_explain),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        SELECTABLE_GOALS.forEach { goal ->
            val selected = viewModel.state.goalHours == goal.hours
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                onClick = { viewModel.setGoalHours(goal.hours) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(goalLabel(goal.hours), style = MaterialTheme.typography.titleMedium)
                    RadioButton(selected = selected, onClick = { viewModel.setGoalHours(goal.hours) })
                }
            }
        }
    }
}
