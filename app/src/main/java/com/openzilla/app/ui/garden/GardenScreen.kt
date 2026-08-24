package com.openzilla.app.ui.garden

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openzilla.app.R
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.ui.components.rememberNowTicker
import com.openzilla.app.ui.rememberHaptics
import com.openzilla.app.ui.rememberOpenZillaViewModel
import com.openzilla.app.util.formatDurationShort
import com.openzilla.app.util.PlantSpecies
import com.openzilla.app.util.growthStageFor

/** Un ciclo entero de brisa. Lento a propósito: se nota vivo sin llamar la atención. */
private const val BREEZE_MILLIS = 5200

private const val COLUMNS = 3

/**
 * The garden: one pot per habit, laid out like the Zen Garden greenhouse in Plants vs.
 * Zombies — a grid of pots you can look over at a glance.
 *
 * It is deliberately read-only. Nothing here writes to the database, and it reuses the same
 * `startedAt` the counters already use, so no matter what the garden does it cannot disturb
 * the time tracking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(onOpenHabit: (Long) -> Unit, onBack: () -> Unit) {
    val viewModel = rememberOpenZillaViewModel { GardenViewModel(it.repository) }
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()

    // Un minuto: las plantas cambian de etapa en horas, no hay nada que refrescar más a menudo.
    val nowState = rememberNowTicker(intervalMillis = 60_000L)

    // Una sola animación para todo el jardín. Cada planta la lee desfasada, así que se mueven
    // de forma distinta sin que haya más de una animación en marcha.
    val transition = rememberInfiniteTransition(label = "brisa")
    val breeze = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(BREEZE_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "vaiven"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.garden_title)) },
                navigationIcon = {
                    IconButton(onClick = { haptics.tap(); onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        if (habits.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.garden_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        val now = nowState.value
        val totalCare = remember(habits, now) {
            habits.sumOf { (now - it.startedAt).coerceAtLeast(0) }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(COLUMNS),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(COLUMNS) }) {
                Column(Modifier.padding(bottom = 12.dp)) {
                    Text(
                        stringResource(R.string.garden_summary, habits.size, formatDurationShort(totalCare)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.garden_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            itemsIndexed(habits, key = { _, habit -> habit.id }) { index, habit ->
                PotCell(
                    habit = habit,
                    index = index,
                    nowState = nowState,
                    breeze = breeze,
                    onClick = { haptics.tap(); onOpenHabit(habit.id) }
                )
            }
        }
    }
}

@Composable
private fun PotCell(
    habit: HabitEntity,
    index: Int,
    nowState: State<Long>,
    breeze: State<Float>,
    onClick: () -> Unit
) {
    val elapsed = (nowState.value - habit.startedAt).coerceAtLeast(0)
    val stage = growthStageFor(elapsed)
    val growth = stage.progressWithin(elapsed)
    // La especie sale del id, no de la base de datos: no hace falta columna ni migración y
    // sigue siendo la misma después de cerrar la app.
    val species = remember(habit.id) { PlantSpecies.forHabit(habit.id) }
    val revealed = stage.speciesRevealed

    val scheme = MaterialTheme.colorScheme
    val darkTheme = scheme.background.luminance() < 0.5f
    val palette = remember(species, revealed, darkTheme, scheme.primary, scheme.surfaceVariant, scheme.outline) {
        plantPalette(species, revealed, darkTheme, scheme.primary, scheme.surfaceVariant, scheme.outline)
    }

    Column(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(0.85f)) {
            // La brisa se lee aquí dentro: un fotograma repinta este lienzo y nada más.
            drawPottedPlant(
                stage = stage,
                species = species,
                growth = growth,
                sway = swayFor(breeze.value, index),
                palette = palette
            )
        }
        Text(
            habit.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )
        Text(
            formatDurationShort(elapsed),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            // Hasta que crece no se sabe qué es: el nombre de la especie es parte del premio.
            if (revealed) {
                stringResource(R.string.garden_species_stage, stringResource(species.nameRes), stringResource(stage.labelRes))
            } else {
                stringResource(R.string.species_unknown)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
