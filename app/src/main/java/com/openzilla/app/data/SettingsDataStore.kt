package com.openzilla.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("openzilla_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val seedColorLight: Int = 0xFFE0526B.toInt(),
    val seedColorDark: Int = 0xFFE0526B.toInt(),
    val currencySymbol: String = "$",
    val notifyProgress: Boolean = true,
    val notifyDailyQuote: Boolean = true
)

/**
 * All user preferences, kept in plain (unencrypted) local DataStore — none of this is
 * sensitive on its own. The one genuinely sensitive value, the PIN, never lives here;
 * see [PinManager].
 */
class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SEED_LIGHT = intPreferencesKey("seed_light")
        val SEED_DARK = intPreferencesKey("seed_dark")
        val CURRENCY = stringPreferencesKey("currency")
        val NOTIFY_PROGRESS = booleanPreferencesKey("notify_progress")
        val NOTIFY_QUOTE = booleanPreferencesKey("notify_quote")
    }

    val settings = context.dataStore.data.map { prefs ->
        val default = AppSettings()
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: default.themeMode,
            seedColorLight = prefs[Keys.SEED_LIGHT] ?: default.seedColorLight,
            seedColorDark = prefs[Keys.SEED_DARK] ?: default.seedColorDark,
            currencySymbol = prefs[Keys.CURRENCY] ?: default.currencySymbol,
            notifyProgress = prefs[Keys.NOTIFY_PROGRESS] ?: default.notifyProgress,
            notifyDailyQuote = prefs[Keys.NOTIFY_QUOTE] ?: default.notifyDailyQuote
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setSeedColorLight(argb: Int) = context.dataStore.edit { it[Keys.SEED_LIGHT] = argb }
    suspend fun setSeedColorDark(argb: Int) = context.dataStore.edit { it[Keys.SEED_DARK] = argb }
    suspend fun setCurrency(symbol: String) = context.dataStore.edit { it[Keys.CURRENCY] = symbol }
    suspend fun setNotifyProgress(enabled: Boolean) = context.dataStore.edit { it[Keys.NOTIFY_PROGRESS] = enabled }
    suspend fun setNotifyDailyQuote(enabled: Boolean) = context.dataStore.edit { it[Keys.NOTIFY_QUOTE] = enabled }
}
