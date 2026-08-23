package com.openzilla.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HabitRepository
import com.openzilla.app.data.HistoryEntity
import com.openzilla.app.data.ReasonEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitDetailViewModel(private val repository: HabitRepository, private val habitId: Long) : ViewModel() {

    val habit: StateFlow<HabitEntity?> = repository.observeHabit(habitId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val reasons: StateFlow<List<ReasonEntity>> = repository.observeReasons(habitId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.observeHistory(habitId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _errors = MutableStateFlow<String?>(null)
    val errors = _errors.asStateFlow()

    fun clearError() { _errors.value = null }

    fun addReason(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addReason(habitId, trimmed).onFailure { _errors.value = it.message ?: "No se pudo guardar el motivo" }
        }
    }

    fun deleteReason(reason: ReasonEntity) {
        viewModelScope.launch {
            repository.deleteReason(reason).onFailure { _errors.value = it.message ?: "No se pudo eliminar" }
        }
    }

    fun resetHabit() {
        val current = habit.value ?: return
        viewModelScope.launch {
            repository.resetHabit(current, System.currentTimeMillis()).onFailure { _errors.value = it.message ?: "No se pudo reiniciar" }
        }
    }

    /**
     * Records a relapse on a day the user picked in the calendar. The instant is clamped to
     * the current streak: a relapse can never land before the streak started nor in the
     * future, so the counter cannot end up negative and past history is never rewritten.
     */
    fun registerRelapseAt(millis: Long) {
        val current = habit.value ?: return
        val now = System.currentTimeMillis()
        val lower = minOf(current.startedAt, now)
        val at = millis.coerceIn(lower, now)
        viewModelScope.launch {
            repository.resetHabit(current, at).onFailure { _errors.value = it.message ?: "No se pudo registrar la recaída" }
        }
    }

    fun deleteHabit(onDone: () -> Unit) {
        val current = habit.value ?: return
        viewModelScope.launch {
            repository.deleteHabit(current).onSuccess { onDone() }.onFailure { _errors.value = it.message ?: "No se pudo eliminar" }
        }
    }
}
