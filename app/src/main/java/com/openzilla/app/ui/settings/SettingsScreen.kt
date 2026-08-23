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
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.Composable
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
import com.openzilla.app.ui.rememberOpenZillaViewModel
import com.openzilla.app.ui.theme.PRESET_COLORS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = rememberOpenZillaViewModel {
        SettingsViewModel(it, it.settingsRepository, it.pinManager, it.exportImportManager, it.repository)
    }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }
    var showRemovePinConfirm by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
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
                title = { Text("Ajustes") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item { SectionTitle("Apariencia") }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    ThemeModeOption("Sistema", settings.themeMode == ThemeMode.SYSTEM) { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                    ThemeModeOption("Claro", settings.themeMode == ThemeMode.LIGHT) { viewModel.setThemeMode(ThemeMode.LIGHT) }
                    ThemeModeOption("Oscuro", settings.themeMode == ThemeMode.DARK) { viewModel.setThemeMode(ThemeMode.DARK) }
                }
            }
            item {
                Text("Color — modo claro", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
                ColorSwatchRow(selected = Color(settings.seedColorLight), onSelect = { viewModel.setSeedColorLight(it.toArgb()) })
            }
            item {
                Text("Color — modo oscuro", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                ColorSwatchRow(selected = Color(settings.seedColorDark), onSelect = { viewModel.setSeedColorDark(it.toArgb()) })
            }

            item { SectionTitle("General") }
            item {
                ListItem(
                    headlineContent = { Text("Moneda") },
                    supportingContent = { Text(settings.currencySymbol) },
                    modifier = Modifier.clickable { showCurrencyDialog = true }
                )
            }

            item { SectionTitle("Seguridad") }
            item {
                ListItem(
                    headlineContent = { Text(if (pinIsSet) "Cambiar PIN" else "Activar bloqueo con PIN") },
                    supportingContent = { Text("Protege el acceso a la app con un código local. Nunca sale de tu dispositivo.") },
                    modifier = Modifier.clickable { showPinDialog = true }
                )
            }
            if (pinIsSet) {
                item {
                    ListItem(
                        headlineContent = { Text("Quitar PIN") },
                        modifier = Modifier.clickable { showRemovePinConfirm = true }
                    )
                }
            }

            item { SectionTitle("Notificaciones") }
            item {
                ListItem(
                    headlineContent = { Text("Avisos de progreso") },
                    supportingContent = { Text("Recibe un aviso al alcanzar cada logro") },
                    trailingContent = {
                        Switch(checked = settings.notifyProgress, onCheckedChange = { enabled ->
                            if (enabled) enableNotification { viewModel.setNotifyProgress(true) } else viewModel.setNotifyProgress(false)
                        })
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Motivación diaria") },
                    supportingContent = { Text("Recibe la frase del día") },
                    trailingContent = {
                        Switch(checked = settings.notifyDailyQuote, onCheckedChange = { enabled ->
                            if (enabled) enableNotification { viewModel.setNotifyDailyQuote(true) } else viewModel.setNotifyDailyQuote(false)
                        })
                    }
                )
            }

            item { SectionTitle("Datos") }
            item {
                ListItem(
                    headlineContent = { Text("Exportar datos") },
                    supportingContent = { Text("Guarda una copia local en un archivo que tú eliges") },
                    modifier = Modifier.clickable { exportLauncher.launch("openzilla_backup.json") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Importar datos") },
                    supportingContent = { Text("Reemplaza los datos actuales con los de un archivo exportado antes") },
                    modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json", "text/plain", "text/*")) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Borrar todos los datos", color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { showDeleteAllConfirm = true }
                )
            }

            item { SectionTitle("Acerca de") }
            item {
                Column(Modifier.padding(16.dp)) {
                    Text("OpenZilla", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "100% local y de código abierto. Sin anuncios, sin compras, sin cuentas ni conexión a internet: tus datos nunca salen de este dispositivo salvo que tú los exportes.",
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
            title = "Quitar PIN",
            message = "La app dejará de pedir un código al abrirse.",
            confirmLabel = "Quitar",
            onConfirm = { viewModel.removePin { pinIsSet = false }; showRemovePinConfirm = false },
            onDismiss = { showRemovePinConfirm = false }
        )
    }
    if (showDeleteAllConfirm) {
        ConfirmDialog(
            title = "Borrar todos los datos",
            message = "Se eliminarán todos tus hábitos, motivos e historial de forma permanente e irreversible. Considera exportar una copia antes de continuar.",
            confirmLabel = "Borrar todo",
            onConfirm = { viewModel.deleteAllData { }; showDeleteAllConfirm = false },
            onDismiss = { showDeleteAllConfirm = false }
        )
    }
    if (showCurrencyDialog) {
        CurrencyDialog(current = settings.currencySymbol, onDismiss = { showCurrencyDialog = false }, onConfirm = { viewModel.setCurrency(it); showCurrencyDialog = false })
    }
    pendingImportUri?.let { uri ->
        ConfirmDialog(
            title = "Importar datos",
            message = "Esto reemplazará todos los hábitos, motivos e historial actuales con el contenido del archivo elegido. Esta acción no se puede deshacer.",
            confirmLabel = "Reemplazar",
            onConfirm = { viewModel.importFrom(uri); pendingImportUri = null },
            onDismiss = { pendingImportUri = null }
        )
    }
    message?.let { msg ->
        ConfirmDialog(title = "Datos", message = msg, confirmLabel = "Cerrar", onConfirm = viewModel::clearMessage, onDismiss = viewModel::clearMessage)
    }
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
private fun ColorSwatchRow(selected: Color, onSelect: (Color) -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PRESET_COLORS.forEach { color ->
            val isSelected = color.toArgb() == selected.toArgb()
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color, CircleShape)
                    .border(2.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                    .clickable { onSelect(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Icon(Icons.Filled.Check, contentDescription = "Seleccionado", tint = Color.White)
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
        title = { Text("Elige un PIN") },
        text = {
            Column {
                OutlinedTextField(value = pin, onValueChange = { if (it.length <= 8) pin = it.filter { c -> c.isDigit() } }, label = { Text("PIN (4-8 dígitos)") })
                OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 8) confirmPin = it.filter { c -> c.isDigit() } }, label = { Text("Repite el PIN") }, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(pin) }, enabled = matches) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun CurrencyDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Símbolo de moneda") },
        text = { OutlinedTextField(value = value, onValueChange = { if (it.length <= 4) value = it }, label = { Text("Ej: $, €, £") }) },
        confirmButton = { TextButton(onClick = { onConfirm(value.ifBlank { "$" }) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
