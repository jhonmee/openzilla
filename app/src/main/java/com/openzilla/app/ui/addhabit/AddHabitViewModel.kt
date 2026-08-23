package com.openzilla.app.ui.addhabit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openzilla.app.data.HabitCostType
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HabitRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class AddHabitUiState(
    val name: String = "",
    val iconKey: String = "generic",
    val costType: HabitCostType = HabitCostType.EVENT,
    val weeklyAmountText: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val goalHours: Int = 24,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

class AddHabitViewModel(
    private val repository: HabitRepository,
    private val editingHabitId: Long?
) : ViewModel() {
    var state by mutableStateOf(AddHabitUiState())
        private set

    var isEditing = false
        private set

    init {
        if (editingHabitId != null) {
            isEditing = true
            viewModelScope.launch {
                // Load the current value once — this screen edits a local draft; it should
                // not keep re-syncing from the database while the user is mid-edit.
                repository.observeHabit(editingHabitId).firstOrNull()?.let {
                    state = AddHabitUiState(
                        name = it.name,
                        iconKey = it.iconKey,
                        costType = it.costType,
                        weeklyAmountText = it.weeklyAmount?.toString() ?: "",
                        startedAt = it.startedAt,
                        goalHours = it.goalHours,
                        createdAt = it.createdAt,
                        sortOrder = it.sortOrder
                    )
                }
            }
        }
    }

    fun setName(v: String) { state = state.copy(name = v) }
    fun setIcon(v: String) { state = state.copy(iconKey = v) }
    fun setCostType(v: HabitCostType) { state = state.copy(costType = v) }
    fun setWeeklyAmount(v: String) { state = state.copy(weeklyAmountText = v) }
    fun setStartedAt(v: Long) { state = state.copy(startedAt = v) }
    fun setGoalHours(v: Int) { state = state.copy(goalHours = v) }

    fun save(onDone: () -> Unit, onError: (String) -> Unit) {
        val trimmedName = state.name.trim()
        if (trimmedName.isEmpty()) { onError("Ponle un nombre al hábito"); return }
        val amount = state.weeklyAmountText.replace(',', '.').toDoubleOrNull()
        if (state.costType == HabitCostType.MONEY && amount != null && amount < 0) {
            onError("El gasto no puede ser negativo"); return
        }

        viewModelScope.launch {
            val result = if (isEditing && editingHabitId != null) {
                repository.updateHabit(
                    HabitEntity(
                        id = editingHabitId,
                        name = trimmedName,
                        iconKey = state.iconKey,
                        costType = state.costType,
                        weeklyAmount = if (state.costType == HabitCostType.MONEY || state.costType == HabitCostType.TIME) amount else null,
                        startedAt = state.startedAt,
                        goalHours = state.goalHours,
                        createdAt = state.createdAt,
                        sortOrder = state.sortOrder
                    )
                )
            } else {
                repository.addHabit(
                    HabitEntity(
                        name = trimmedName,
                        iconKey = state.iconKey,
                        costType = state.costType,
                        weeklyAmount = if (state.costType == HabitCostType.MONEY || state.costType == HabitCostType.TIME) amount else null,
                        startedAt = state.startedAt,
                        goalHours = state.goalHours
                    )
                ).map { }
            }
            result.onSuccess { onDone() }.onFailure { onError(it.message ?: "No se pudo guardar") }
        }
    }
}
