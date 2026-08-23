package com.openzilla.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openzilla.app.OpenZillaApp

/** Gives Composables access to the app-wide singletons (repository, settings, PIN manager…). */
@Composable
fun openZillaApp(): OpenZillaApp = LocalContext.current.applicationContext as OpenZillaApp

/**
 * Minimal generic ViewModel factory so screens can build `MyViewModel(app.repository, ...)`
 * without pulling in a DI framework for an app this size.
 */
@Composable
inline fun <reified VM : ViewModel> rememberOpenZillaViewModel(crossinline create: (OpenZillaApp) -> VM): VM {
    val app = openZillaApp()
    val factory = remember(app) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return create(app) as T
            }
        }
    }
    return viewModel(factory = factory)
}
