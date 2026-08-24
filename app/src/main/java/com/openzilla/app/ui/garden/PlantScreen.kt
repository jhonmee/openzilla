package com.openzilla.app.ui.garden

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openzilla.app.R
import com.openzilla.app.ui.components.rememberNowTicker
import com.openzilla.app.ui.rememberHaptics
import com.openzilla.app.ui.rememberOpenZillaViewModel
import com.openzilla.app.util.PlantSpecies
import com.openzilla.app.util.canWaterToday
import com.openzilla.app.util.formatDurationShort
import com.openzilla.app.util.lastBrokenStreakMillis
import com.openzilla.app.util.plantCondition
import com.openzilla.app.util.quoteOfTheDay
import com.openzilla.app.util.wateringBoostFor
import kotlinx.coroutines.launch

private const val BREEZE_MILLIS = 5200

/** Cuánto empuja el dedo a la planta, y cuánto tarda en volver a su sitio. */
private const val NUDGE_DIVISOR = 160f
private const val NUDGE_LIMIT = 1.6f

/** Duración de la lluvia al regar. */
private const val WATERING_MILLIS = 1100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantScreen(habitId: Long, onOpenCalendar: (Long) -> Unit, onBack: () -> Unit) {
    val viewModel = rememberOpenZillaViewModel { PlantViewModel(it.repository, habitId) }
    val habit by viewModel.habit.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Cada minuto: la cuenta atrás se mide en horas, no hace falta más.
    val nowState = rememberNowTicker(intervalMillis = 60_000L)

    val transition = rememberInfiniteTransition(label = "brisa")
    val breeze = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(BREEZE_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "vaiven"
    )

    // Empujón del dedo: sigue al dedo y vuelve rebotando al soltar.
    val nudge = remember { Animatable(0f) }
    // Lluvia del riego: una sola pasada de 0 a 1 cada vez que se riega.
    val rain = remember { Animatable(0f) }
    var watered by remember { mutableIntStateOf(0) }
    LaunchedEffect(watered) {
        if (watered == 0) return@LaunchedEffect
        rain.snapTo(0f)
        rain.animateTo(1f, tween(WATERING_MILLIS, easing = LinearEasing))
    }

    val current = habit
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = { haptics.tap(); onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        val now = nowState.value
        val previousStreak = remember(history) {
            lastBrokenStreakMillis(history.map { it.streakStart to it.streakEnd })
        }
        val condition = plantCondition(current.startedAt, previousStreak, current.recoveryBonusMillis, now)
        val species = remember(current.id) { PlantSpecies.forHabit(current.id) }
        val revealed = condition.stage.speciesRevealed

        val scheme = MaterialTheme.colorScheme
        val darkTheme = scheme.background.luminance() < 0.5f
        val palette = remember(species, revealed, darkTheme, scheme.primary, scheme.surfaceVariant, scheme.outline) {
            plantPalette(species, revealed, darkTheme, scheme.primary, scheme.surfaceVariant, scheme.outline)
        }

        val quote = remember(context) { quoteOfTheDay(context) }
        val canWater = condition.recovering && canWaterToday(current.lastWateredAt, now)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .pointerInput(current.id) {
                        detectDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    nudge.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        ) { change, amount ->
                            change.consume()
                            scope.launch {
                                nudge.snapTo((nudge.value + amount.x / NUDGE_DIVISOR).coerceIn(-NUDGE_LIMIT, NUDGE_LIMIT))
                            }
                        }
                    }
            ) {
                drawPottedPlant(
                    stage = condition.stage,
                    species = species,
                    growth = condition.growth,
                    // La brisa de siempre más lo que el dedo la esté empujando.
                    sway = swayFor(breeze.value, 0) + nudge.value,
                    dryness = condition.dryness,
                    palette = palette
                )
                if (rain.value > 0f && rain.value < 1f) {
                    drawWatering(rain.value, palette.foliage)
                }
            }

            Text(
                stringResource(R.string.plant_nudge_hint),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (condition.recovering) scheme.error.copy(alpha = 0.10f)
                    else scheme.primary.copy(alpha = 0.12f)
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (condition.recovering) {
                        Text(
                            stringResource(R.string.plant_recovering_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.plant_recovering_in, formatDurationShort(condition.recoveryRemaining ?: 0L)),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        LinearProgressIndicator(
                            progress = { 1f - condition.dryness },
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        )
                    } else {
                        Text(
                            stringResource(R.string.plant_healthy),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (revealed) {
                                stringResource(R.string.garden_species_stage, stringResource(species.nameRes), stringResource(condition.stage.labelRes))
                            } else {
                                stringResource(R.string.species_unknown)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (condition.recovering) {
                Button(
                    onClick = {
                        haptics.confirm()
                        watered++
                        viewModel.water(current, wateringBoostFor(previousStreak ?: 0L))
                    },
                    enabled = canWater,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Icon(Icons.Filled.WaterDrop, contentDescription = null)
                    Text(
                        stringResource(if (canWater) R.string.plant_water else R.string.plant_watered_today),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    stringResource(R.string.plant_water_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            OutlinedButton(
                onClick = { haptics.tap(); onOpenCalendar(current.id) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Text(stringResource(R.string.plant_open_calendar), modifier = Modifier.padding(start = 8.dp))
            }

            Column(
                Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    quote.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center
                )
                Text(
                    "— ${quote.author}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/**
 * Gotas cayendo sobre la planta.
 *
 * Una sola pasada de un valor animado: las gotas van escalonadas por su posición, así que con
 * un único número se dibujan todas en momentos distintos y parece lluvia de verdad.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWatering(progress: Float, tint: Color) {
    val drops = 7
    repeat(drops) { index ->
        val phase = (progress * 1.6f - index * 0.09f).coerceIn(0f, 1f)
        if (phase <= 0f || phase >= 1f) return@repeat
        val x = size.width * (0.28f + 0.44f * index / (drops - 1f))
        val y = size.height * (0.08f + 0.55f * phase)
        drawCircle(
            color = tint.copy(alpha = 0.55f * (1f - phase)),
            radius = size.width * 0.012f,
            center = Offset(x, y)
        )
    }
}
