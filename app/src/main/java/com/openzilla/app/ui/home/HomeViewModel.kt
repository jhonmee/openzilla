package com.openzilla.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: HabitRepository) : ViewModel() {

    val habits: StateFlow<List<HabitEntity>> = repository.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteHabit(habit: HabitEntity, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.deleteHabit(habit).onFailure { onError(it.message ?: "No se pudo eliminar") }
        }
    }

    /** Called once when a drag finishes, with the list exactly as the user left it. */
    fun saveOrder(orderedIds: List<Long>, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.saveHabitOrder(orderedIds).onFailure { onError(it.message ?: "No se pudo guardar el orden") }
        }
    }

    fun resetHabit(habit: HabitEntity, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.resetHabit(habit, System.currentTimeMillis()).onFailure { onError(it.message ?: "No se pudo reiniciar") }
        }
    }
}
