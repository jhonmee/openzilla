package com.openzilla.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openzilla.app.data.HabitCostType
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.ReasonEntity
import com.openzilla.app.util.quoteOfTheDay
import java.util.concurrent.TimeUnit

@Composable
fun MotivationTab(
    habit: HabitEntity,
    reasons: List<ReasonEntity>,
    currencySymbol: String,
    onAddReason: (String) -> Unit,
    onDeleteReason: (ReasonEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val quote = remember { quoteOfTheDay() }

    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, contentDescription = "Añadir motivo") } }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Frase del día", style = MaterialTheme.typography.titleMedium)
                        Text(quote.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                        Text("— ${quote.author}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            item {
                SavingsCard(habit, currencySymbol)
            }
            item {
                Text("Tus motivos para dejarlo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            }
            if (reasons.isEmpty()) {
                item { Text("Aún no has escrito ningún motivo. Toca + para añadir uno.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(reasons, key = { it.id }) { reason ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(reason.text, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onDeleteReason(reason) }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar motivo") }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddReasonDialog(onDismiss = { showAddDialog = false }, onConfirm = { text -> onAddReason(text); showAddDialog = false })
    }
}

@Composable
private fun SavingsCard(habit: HabitEntity, currencySymbol: String) {
    val elapsedDays = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - habit.startedAt) / (60.0 * 24.0)
    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Lo que llevas ganado", style = MaterialTheme.typography.titleMedium)
            val amount = habit.weeklyAmount
            when {
                habit.costType == HabitCostType.MONEY && amount != null && amount > 0 -> {
                    val saved = amount / 7.0 * elapsedDays
                    Text("Has ahorrado aproximadamente $currencySymbol${"%.2f".format(saved)}", modifier = Modifier.padding(top = 8.dp))
                }
                habit.costType == HabitCostType.TIME && amount != null && amount > 0 -> {
                    val savedHours = amount / 7.0 * elapsedDays
                    Text("Has recuperado aproximadamente ${"%.1f".format(savedHours)} horas de tu tiempo", modifier = Modifier.padding(top = 8.dp))
                }
                else -> {
                    Text("Llevas ${"%.1f".format(elapsedDays)} días eligiendo una vida más saludable.", modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun AddReasonDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo motivo") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("¿Por qué quieres dejarlo?") }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
