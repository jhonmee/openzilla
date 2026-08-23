package com.openzilla.app.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openzilla.app.R
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

class HabitDetailViewModel(
    private val context: Context,
    private val repository: HabitRepository,
    private val habitId: Long
) : ViewModel() {

    // Arranca con lo que ya se sabe del hábito (viniendo de la lista, siempre hay algo), así
    // la pantalla se dibuja completa desde el primer frame.
    val habit: StateFlow<HabitEntity?> = repository.observeHabit(habitId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.cachedHabit(habitId))

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
            repository.addReason(habitId, trimmed).onFailure { _errors.value = it.message ?: context.getString(R.string.error_reason_failed) }
        }
    }

    fun deleteReason(reason: ReasonEntity) {
        viewModelScope.launch {
            repository.deleteReason(reason).onFailure { _errors.value = it.message ?: context.getString(R.string.error_delete_failed) }
        }
    }

    fun resetHabit() {
        val current = habit.value ?: return
        viewModelScope.launch {
            repository.resetHabit(current, System.currentTimeMillis()).onFailure { _errors.value = it.message ?: context.getString(R.string.error_reset_failed) }
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
            repository.resetHabit(current, at).onFailure { _errors.value = it.message ?: context.getString(R.string.error_relapse_failed) }
        }
    }

    fun deleteHabit(onDone: () -> Unit) {
        val current = habit.value ?: return
        viewModelScope.launch {
            repository.deleteHabit(current).onSuccess { onDone() }.onFailure { _errors.value = it.message ?: context.getString(R.string.error_delete_failed) }
        }
    }
}
