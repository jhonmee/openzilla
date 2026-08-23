package com.openzilla.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.openzilla.app.R
import com.openzilla.app.data.AppLanguage
import com.openzilla.app.findActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openzilla.app.data.ThemeMode
import com.openzilla.app.ui.components.ConfirmDialog
import com.openzilla.app.ui.rememberHaptics
import com.openzilla.app.ui.rememberOpenZillaViewModel
import com.openzilla.app.ui.theme.PRESET_COLORS
import com.openzilla.app.ui.theme.dynamicColorAvailable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = rememberOpenZillaViewModel {
        SettingsViewModel(it, it.settingsRepository, it.pinManager, it.exportImportManager, it.repository)
    }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()

    var showPinDialog by remember { mutableStateOf(false) }
    var showRemovePinConfirm by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var pinIsSet by remember { mutableStateOf(viewModel.isPinSet()) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let { viewModel.exportTo(it) }
    }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        // Importing replaces everything currently stored — hold the file and confirm first
        // rather than wiping the user's existing data the moment they pick a file.
        uri?.let { pendingImportUri = it }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingNotifToggle by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingNotifToggle?.invoke()
        pendingNotifToggle = null
    }
    // On Android 13+, posting notifications needs a runtime permission grant first; below
    // that, the permission is granted automatically at install time.
    fun enableNotification(setter: () -> Unit) {
        val needsRuntimePermission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        val alreadyGranted = !needsRuntimePermission ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            setter()
        } else {
            pendingNotifToggle = setter
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item { SectionTitle(stringResource(R.string.section_appearance)) }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    ThemeModeOption(stringResource(R.string.theme_system), settings.themeMode == ThemeMode.SYSTEM) { haptics.tap(); viewModel.setThemeMode(ThemeMode.SYSTEM) }
                    ThemeModeOption(stringResource(R.string.theme_light), settings.themeMode == ThemeMode.LIGHT) { haptics.tap(); viewModel.setThemeMode(ThemeMode.LIGHT) }
                    ThemeModeOption(stringResource(R.string.theme_dark), settings.themeMode == ThemeMode.DARK) { haptics.tap(); viewModel.setThemeMode(ThemeMode.DARK) }
                    ThemeModeOption(stringResource(R.string.theme_oled), settings.themeMode == ThemeMode.OLED) { haptics.tap(); viewModel.setThemeMode(ThemeMode.OLED) }
                }
            }
            if (dynamicColorAvailable) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.dynamic_colors_title)) },
                        supportingContent = { Text(stringResource(R.string.dynamic_colors_desc)) },
                        trailingContent = {
                            Switch(checked = settings.dynamicColor, onCheckedChange = { haptics.tap(); viewModel.setDynamicColor(it) })
                        }
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.color_light),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (settings.dynamicColor) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp)
                )
                ColorSwatchRow(
                    selected = Color(settings.seedColorLight),
                    enabled = !settings.dynamicColor,
                    onSelect = { haptics.tap(); viewModel.setSeedColorLight(it.toArgb()) }
                )
            }
            item {
                Text(
                    stringResource(R.string.color_dark),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (settings.dynamicColor) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
                ColorSwatchRow(
                    selected = Color(settings.seedColorDark),
                    enabled = !settings.dynamicColor,
                    onSelect = { haptics.tap(); viewModel.setSeedColorDark(it.toArgb()) }
                )
            }

            item { SectionTitle(stringResource(R.string.section_general)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.language_title)) },
                    supportingContent = { Text(stringResource(languageLabelRes(settings.language))) },
                    modifier = Modifier.clickable { haptics.tap(); showLanguageDialog = true }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.haptics_title)) },
                    supportingContent = { Text(stringResource(R.string.haptics_desc)) },
                    trailingContent = {
                        Switch(
                            checked = settings.hapticsEnabled,
                            onCheckedChange = { enabled ->
                                // Se vibra antes de guardar para que al activarlo se note en el
                                // acto cómo es la respuesta que se acaba de encender.
                                if (enabled) haptics.tapAlways()
                                viewModel.setHapticsEnabled(enabled)
                            }
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.currency_title)) },
                    supportingContent = { Text(settings.currencySymbol) },
                    modifier = Modifier.clickable { showCurrencyDialog = true }
                )
            }

            item { SectionTitle(stringResource(R.string.section_security)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(if (pinIsSet) R.string.pin_change else R.string.pin_activate)) },
                    supportingContent = { Text(stringResource(R.string.pin_desc)) },
                    modifier = Modifier.clickable { showPinDialog = true }
                )
            }
            if (pinIsSet) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.pin_remove)) },
                        modifier = Modifier.clickable { showRemovePinConfirm = true }
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.section_notifications)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.notify_progress)) },
                    supportingContent = { Text(stringResource(R.string.notify_progress_desc)) },
                    trailingContent = {
                        Switch(checked = settings.notifyProgress, onCheckedChange = { enabled ->
                            haptics.tap()
                            if (enabled) enableNotification { viewModel.setNotifyProgress(true) } else viewModel.setNotifyProgress(false)
                        })
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.notify_quote)) },
                    supportingContent = { Text(stringResource(R.string.notify_quote_desc)) },
                    trailingContent = {
                        Switch(checked = settings.notifyDailyQuote, onCheckedChange = { enabled ->
                            haptics.tap()
                            if (enabled) enableNotification { viewModel.setNotifyDailyQuote(true) } else viewModel.setNotifyDailyQuote(false)
                        })
                    }
                )
            }

            item { SectionTitle(stringResource(R.string.section_data)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.data_export)) },
                    supportingContent = { Text(stringResource(R.string.data_export_desc)) },
                    modifier = Modifier.clickable { exportLauncher.launch("openzilla_backup.json") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.data_import)) },
                    supportingContent = { Text(stringResource(R.string.data_import_desc)) },
                    modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json", "text/plain", "text/*")) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.data_delete_all), color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { showDeleteAllConfirm = true }
                )
            }

            item { SectionTitle(stringResource(R.string.section_about)) }
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.about_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin -> viewModel.setPin(pin) { pinIsSet = true; showPinDialog = false } }
        )
    }
    if (showRemovePinConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.pin_remove),
            message = stringResource(R.string.pin_remove_message),
            confirmLabel = stringResource(R.string.pin_remove_confirm),
            onConfirm = { viewModel.removePin { pinIsSet = false }; showRemovePinConfirm = false },
            onDismiss = { showRemovePinConfirm = false }
        )
    }
    if (showDeleteAllConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.data_delete_all),
            message = stringResource(R.string.data_delete_all_message),
            confirmLabel = stringResource(R.string.data_delete_all_confirm),
            onConfirm = { viewModel.deleteAllData { }; showDeleteAllConfirm = false },
            onDismiss = { showDeleteAllConfirm = false }
        )
    }
    if (showLanguageDialog) {
        LanguageDialog(
            current = settings.language,
            onDismiss = { showLanguageDialog = false },
            onConfirm = { language ->
                showLanguageDialog = false
                haptics.tap()
                // Recrear la Activity es lo que hace que los textos ya cargados se relean en
                // el idioma nuevo; sin eso habría que salir y volver a entrar en la app.
                viewModel.setLanguage(language) { context.findActivity()?.recreate() }
            }
        )
    }
    if (showCurrencyDialog) {
        CurrencyDialog(current = settings.currencySymbol, onDismiss = { showCurrencyDialog = false }, onConfirm = { viewModel.setCurrency(it); showCurrencyDialog = false })
    }
    pendingImportUri?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.data_import),
            message = stringResource(R.string.data_import_message),
            confirmLabel = stringResource(R.string.data_import_confirm),
            onConfirm = { viewModel.importFrom(uri); pendingImportUri = null },
            onDismiss = { pendingImportUri = null }
        )
    }
    message?.let { msg ->
        ConfirmDialog(title = stringResource(R.string.data_title), message = msg, confirmLabel = stringResource(R.string.action_close), onConfirm = viewModel::clearMessage, onDismiss = viewModel::clearMessage)
    }
}

@StringRes
private fun languageLabelRes(language: AppLanguage): Int = when (language) {
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.SPANISH -> R.string.language_spanish
    AppLanguage.ENGLISH -> R.string.language_english
}

@Composable
private fun LanguageDialog(current: AppLanguage, onDismiss: () -> Unit, onConfirm: (AppLanguage) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_dialog_title)) },
        text = {
            Column {
                AppLanguage.entries.forEach { language ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(language) }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(selected = language == current, onClick = { onConfirm(language) })
                        Text(stringResource(languageLabelRes(language)), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun ThemeModeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 6.dp)
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ColorSwatchRow(selected: Color, enabled: Boolean, onSelect: (Color) -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PRESET_COLORS.forEach { color ->
            val isSelected = enabled && color.toArgb() == selected.toArgb()
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (enabled) color else color.copy(alpha = 0.28f), CircleShape)
                    .border(2.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                    .clickable(enabled = enabled) { onSelect(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.color_selected), tint = Color.White)
            }
        }
    }
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val matches = pin.length in 4..8 && pin == confirmPin
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pin_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(value = pin, onValueChange = { if (it.length <= 8) pin = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.pin_field)) })
                OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 8) confirmPin = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.pin_repeat)) }, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(pin) }, enabled = matches) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun CurrencyDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.currency_dialog_title)) },
        text = { OutlinedTextField(value = value, onValueChange = { if (it.length <= 4) value = it }, label = { Text(stringResource(R.string.currency_hint)) }) },
        confirmButton = { TextButton(onClick = { onConfirm(value.ifBlank { "$" }) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
