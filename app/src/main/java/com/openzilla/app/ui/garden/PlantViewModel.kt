package com.openzilla.app.ui.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HabitRepository
import com.openzilla.app.data.HistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The single plant screen.
 *
 * Watering is the only thing in the whole garden that writes anything, and it only ever
 * touches its own two columns: the moment it was watered and the recovery time earned. It
 * cannot move `startedAt`, so no amount of watering changes what the counters say.
 */
class PlantViewModel(
    private val repository: HabitRepository,
    habitId: Long
) : ViewModel() {

    val habit: StateFlow<HabitEntity?> = repository.observeHabit(habitId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.cachedHabit(habitId))

    val history: StateFlow<List<HistoryEntity>> = repository.observeHistory(habitId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun water(habit: HabitEntity, boostMillis: Long) {
        viewModelScope.launch {
            repository.waterPlant(habit, System.currentTimeMillis(), boostMillis)
        }
    }
}
