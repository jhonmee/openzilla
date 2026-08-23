package com.openzilla.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openzilla.app.data.AppSettings
import com.openzilla.app.ui.navigation.OpenZillaNavHost
import com.openzilla.app.ui.settings.PinLockScreen
import com.openzilla.app.ui.theme.OpenZillaTheme

/**
 * The one and only Activity. It just hosts Compose; all real logic lives in ViewModels and
 * the repository layer below, which is what keeps this class — the thing the Android
 * lifecycle can most easily leak — tiny and stateless.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as OpenZillaApp
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())

            OpenZillaTheme(
                mode = settings.themeMode,
                seedLight = Color(settings.seedColorLight),
                seedDark = Color(settings.seedColorDark),
                dynamicColor = settings.dynamicColor
            ) {
                OpenZillaRoot(app, settings)
            }
        }
    }
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
