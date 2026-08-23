package com.openzilla.app.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openzilla.app.R
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val context: Context, private val repository: HabitRepository) : ViewModel() {

    /**
     * null significa "todavía no ha llegado nada de la base de datos", que no es lo mismo que
     * una lista vacía. Sin esa distinción, al abrir la app se veía durante un frame el mensaje
     * de "no tienes hábitos" antes de aparecer los que sí hay.
     */
    val habits: StateFlow<List<HabitEntity>?> = repository.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun deleteHabit(habit: HabitEntity, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.deleteHabit(habit).onFailure { onError(it.message ?: context.getString(R.string.error_delete_failed)) }
        }
    }

    /** Called once when a drag finishes, with the list exactly as the user left it. */
    fun saveOrder(orderedIds: List<Long>, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.saveHabitOrder(orderedIds).onFailure { onError(it.message ?: context.getString(R.string.error_order_failed)) }
        }
    }

    fun resetHabit(habit: HabitEntity, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.resetHabit(habit, System.currentTimeMillis()).onFailure { onError(it.message ?: context.getString(R.string.error_reset_failed)) }
        }
    }
}
