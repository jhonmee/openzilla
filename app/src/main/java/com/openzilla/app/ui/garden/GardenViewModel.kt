package com.openzilla.app.ui.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Sólo lectura, y a propósito: el jardín se dibuja a partir de los mismos hábitos que ya
 * alimentan los contadores y no escribe nada, así que no hay forma de que altere el registro
 * de tiempo por mucho que crezca.
 */
class GardenViewModel(repository: HabitRepository) : ViewModel() {

    val habits: StateFlow<List<HabitEntity>> = repository.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
