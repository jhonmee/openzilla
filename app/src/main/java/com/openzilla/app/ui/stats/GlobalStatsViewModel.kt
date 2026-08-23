package com.openzilla.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HabitRepository
import com.openzilla.app.data.HistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Just plumbing: it exposes the two lists and nothing else. All the arithmetic lives in
 * `computeGlobalStats`, a pure function the screen calls, which keeps the maths testable
 * without an Android device in the loop.
 */
class GlobalStatsViewModel(repository: HabitRepository) : ViewModel() {

    val habits: StateFlow<List<HabitEntity>> = repository.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.observeAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
