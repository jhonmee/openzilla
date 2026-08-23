package com.openzilla.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openzilla.app.data.AppSettings
import com.openzilla.app.data.ExportImportManager
import com.openzilla.app.data.HabitRepository
import com.openzilla.app.data.PinManager
import com.openzilla.app.data.SettingsRepository
import com.openzilla.app.data.ThemeMode
import com.openzilla.app.notification.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val pinManager: PinManager,
    private val exportImportManager: ExportImportManager,
    private val habitRepository: HabitRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    fun clearMessage() { _message.value = null }

    fun isPinSet() = pinManager.isPinSet()

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setSeedColorLight(argb: Int) = viewModelScope.launch { settingsRepository.setSeedColorLight(argb) }
    fun setSeedColorDark(argb: Int) = viewModelScope.launch { settingsRepository.setSeedColorDark(argb) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    fun setCurrency(symbol: String) = viewModelScope.launch { settingsRepository.setCurrency(symbol) }

    fun setNotifyDailyQuote(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setNotifyDailyQuote(enabled)
        NotificationScheduler.setDailyQuoteEnabled(context, enabled)
    }

    fun setNotifyProgress(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setNotifyProgress(enabled)
        NotificationScheduler.setProgressChecksEnabled(context, enabled)
    }

    fun setPin(pin: String, onDone: () -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.Default) { pinManager.setPin(pin) }
        onDone()
    }

    fun removePin(onDone: () -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.Default) { pinManager.clearPin() }
        onDone()
    }

    fun exportTo(uri: Uri) = viewModelScope.launch {
        exportImportManager.exportTo(uri)
            .onSuccess { _message.value = "Copia exportada correctamente" }
            .onFailure { _message.value = "No se pudo exportar: ${it.message}" }
    }

    fun importFrom(uri: Uri) = viewModelScope.launch {
        exportImportManager.importFrom(uri)
            .onSuccess { count -> _message.value = "Importados $count hábitos. Se reemplazaron los datos anteriores." }
            .onFailure { _message.value = "No se pudo importar: ${it.message}" }
    }

    fun deleteAllData(onDone: () -> Unit) = viewModelScope.launch {
        habitRepository.deleteEverything()
            .onSuccess { onDone() }
            .onFailure { _message.value = "No se pudo borrar todo: ${it.message}" }
    }
}
