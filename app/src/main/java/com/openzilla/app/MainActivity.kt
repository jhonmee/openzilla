package com.openzilla.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openzilla.app.data.AppSettings
import com.openzilla.app.data.SettingsRepository
import com.openzilla.app.data.ThemeMode
import com.openzilla.app.ui.LocalHapticsEnabled
import com.openzilla.app.ui.navigation.OpenZillaNavHost
import com.openzilla.app.ui.settings.PinLockScreen
import com.openzilla.app.ui.theme.OpenZillaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * The one and only Activity. It just hosts Compose; all real logic lives in ViewModels and
 * the repository layer below, which is what keeps this class — the thing the Android
 * lifecycle can most easily leak — tiny and stateless.
 */
class MainActivity : ComponentActivity() {

    /**
     * The chosen language has to be in place before any resource is read, and that happens
     * here — earlier than [onCreate]. Reading it needs a blocking call, which is acceptable
     * exactly once at startup for a tiny local file that DataStore then keeps in memory.
     */
    override fun attachBaseContext(newBase: Context) {
        val tag = runCatching {
            runBlocking { SettingsRepository(newBase).settings.first().language.tag }
        }.getOrNull()
        super.attachBaseContext(if (tag == null) newBase else newBase.withLanguage(tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as OpenZillaApp

        // Los ajustes se leen aquí, antes de componer nada. Colgarlos de un Flow con valores
        // por defecto hacía que los primeros frames se dibujaran con el tema equivocado y el
        // color correcto entrara después: un cambio breve pero perfectamente visible.
        val initialSettings = runCatching {
            runBlocking { app.settingsRepository.settings.first() }
        }.getOrDefault(AppSettings())

        // El fondo de la ventana se ajusta al tema ya resuelto para que tampoco haya un
        // cambio de claro a oscuro antes del primer frame de Compose.
        window.setBackgroundDrawable(ColorDrawable(windowBackgroundFor(initialSettings)))

        setContent {
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = initialSettings)

            OpenZillaTheme(
                mode = settings.themeMode,
                seedLight = Color(settings.seedColorLight),
                seedDark = Color(settings.seedColorDark),
                dynamicColor = settings.dynamicColor
            ) {
                CompositionLocalProvider(LocalHapticsEnabled provides settings.hapticsEnabled) {
                    OpenZillaRoot(app, settings)
                }
            }
        }
    }

    private fun windowBackgroundFor(settings: AppSettings): Int {
        val systemInDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        return when (settings.themeMode) {
            ThemeMode.OLED -> 0xFF000000.toInt()
            ThemeMode.DARK -> 0xFF121212.toInt()
            ThemeMode.LIGHT -> 0xFFF7F7F8.toInt()
            ThemeMode.SYSTEM -> if (systemInDark) 0xFF121212.toInt() else 0xFFF7F7F8.toInt()
        }
    }
}

private fun Context.withLanguage(tag: String): Context {
    val locale = Locale.forLanguageTag(tag)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}

/** Walks up the context chain to the hosting Activity, needed to restart it on a language change. */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
private fun OpenZillaRoot(app: OpenZillaApp, settings: AppSettings) {
    var unlocked by remember { mutableStateOf(!app.pinManager.isPinSet()) }
    if (unlocked) {
        OpenZillaNavHost(settings = settings)
    } else {
        PinLockScreen(pinManager = app.pinManager, onUnlocked = { unlocked = true })
    }
}
