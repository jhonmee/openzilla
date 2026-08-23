package com.openzilla.app.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.openzilla.app.data.PinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shown before anything else when the user has a PIN set. Verification happens fully
 * on-device via [PinManager] — no network, no external calls of any kind.
 */
@Composable
fun PinLockScreen(pinManager: PinManager, onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var errorShown by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column {
                Text("OpenZilla está bloqueada", style = MaterialTheme.typography.titleLarge)
                Text("Introduce tu PIN para continuar", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) { pin = it.filter { c -> c.isDigit() }; errorShown = false } },
                    label = { Text("PIN") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = errorShown,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorShown) {
                    Text("PIN incorrecto", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
                Button(
                    enabled = !checking && pin.isNotEmpty(),
                    onClick = {
                        checking = true
                        scope.launch {
                            val ok = withContext(Dispatchers.Default) { pinManager.verifyPin(pin) }
                            checking = false
                            if (ok) onUnlocked() else errorShown = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) { Text("Desbloquear") }
            }
        }
    }
}
