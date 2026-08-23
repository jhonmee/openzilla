package com.openzilla.app.ui.addhabit

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.openzilla.app.util.HabitCategory
import kotlinx.coroutines.delay

/** Cuánto se ve resaltada la fila elegida antes de pasar al siguiente paso. */
private const val SELECTION_FEEDBACK_MILLIS = 160L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerScreen(onBack: () -> Unit, onPick: (HabitCategory) -> Unit) {
    // La fila tocada se queda pintada un instante antes de avanzar: sin esto la pantalla
    // cambia en el mismo frame del toque y no da tiempo a ver ninguna respuesta.
    var picked by remember { mutableStateOf<HabitCategory?>(null) }
    LaunchedEffect(picked) {
        val category = picked ?: return@LaunchedEffect
        delay(SELECTION_FEEDBACK_MILLIS)
        onPick(category)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("¿Qué quieres dejar?") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(padding)) {
            items(HabitCategory.entries.toList(), key = { it.key }) { category ->
                val isPicked = picked == category
                val container by animateColorAsState(
                    targetValue = if (isPicked) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else Color.Transparent,
                    label = "categoria-seleccionada"
                )
                ListItem(
                    headlineContent = { Text(category.label) },
                    leadingContent = {
                        Icon(category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    },
                    colors = ListItemDefaults.colors(containerColor = container),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        // Una vez elegida una categoría se ignoran más toques: evita disparar
                        // dos veces el paso siguiente si el usuario toca rápido dos filas.
                        .clickable(enabled = picked == null) { picked = category }
                )
            }
        }
    }
}
