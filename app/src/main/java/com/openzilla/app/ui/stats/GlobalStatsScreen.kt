package com.openzilla.app.ui.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openzilla.app.R
import com.openzilla.app.data.HabitCostType
import com.openzilla.app.ui.components.rememberNowTicker
import com.openzilla.app.ui.rememberHaptics
import com.openzilla.app.ui.rememberOpenZillaViewModel
import com.openzilla.app.util.HabitCategory
import com.openzilla.app.util.MonthlyRelapses
import com.openzilla.app.util.computeGlobalStats
import com.openzilla.app.util.formatDurationShort
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val RANKING_BAR_HEIGHT = 10.dp
private val MONTH_CHART_HEIGHT = 120.dp
private val DONUT_SIZE = 150.dp

/** Una sola animación de entrada para toda la pantalla: las barras crecen a la vez y ya está. */
private const val REVEAL_MILLIS = 700

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalStatsScreen(currencySymbol: String, onBack: () -> Unit) {
    val viewModel = rememberOpenZillaViewModel { GlobalStatsViewModel(it.repository) }
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()

    // Medio minuto basta: aquí todo se mide en días y horas.
    val nowState = rememberNowTicker(intervalMillis = 30_000L)
    val now = nowState.value
    val stats = remember(habits, history, now) { computeGlobalStats(habits, history, now) }

    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val reveal by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(REVEAL_MILLIS, easing = FastOutSlowInEasing),
        label = "aparicion"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gstats_title)) },
                navigationIcon = {
                    IconButton(onClick = { haptics.tap(); onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        if (!stats.hasAnything) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.gstats_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    StatCard(Icons.Filled.Timeline, stringResource(R.string.gstats_habits), stats.habitCount.toString(), Modifier.weight(1f), highlight = true)
                    StatCard(Icons.Filled.LocalFireDepartment, stringResource(R.string.gstats_clean_time), formatDurationShort(stats.totalCleanMillis), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    StatCard(Icons.Filled.EmojiEvents, stringResource(R.string.gstats_best), formatDurationShort(stats.bestStreakMillis), Modifier.weight(1f))
                    StatCard(Icons.Filled.Replay, stringResource(R.string.gstats_relapses), stats.relapseCount.toString(), Modifier.weight(1f))
                }
            }

            item { SectionHeader(R.string.gstats_ranking, R.string.gstats_ranking_hint) }
            item { StreakRanking(stats.currentStreaks, reveal) }

            item { SectionHeader(R.string.gstats_by_month, R.string.gstats_by_month_hint) }
            item {
                if (stats.relapseCount == 0) {
                    Text(
                        stringResource(R.string.gstats_no_relapses),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    MonthlyChart(stats.relapsesByMonth, reveal)
                }
            }

            if (stats.moneySaved > 0 || stats.hoursSaved > 0) {
                item { SectionHeader(R.string.gstats_savings, null) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        if (stats.moneySaved > 0) {
                            StatCard(
                                Icons.Filled.EmojiEvents,
                                stringResource(R.string.gstats_money),
                                currencySymbol + "%.2f".format(stats.moneySaved),
                                Modifier.weight(1f),
                                highlight = true
                            )
                        }
                        if (stats.hoursSaved > 0) {
                            StatCard(
                                Icons.Filled.Timeline,
                                stringResource(R.string.gstats_time),
                                stringResource(R.string.gstats_hours, "%.1f".format(stats.hoursSaved)),
                                Modifier.weight(1f),
                                highlight = true
                            )
                        }
                    }
                }
            }

            item { SectionHeader(R.string.gstats_by_type, null) }
            item { TypeDonut(stats.countByType, reveal) }
            item { Box(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(titleRes: Int, hintRes: Int?) {
    Column(Modifier.padding(top = 28.dp, bottom = 12.dp)) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (hintRes != null) {
            Text(
                stringResource(hintRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Barras horizontales, una por hábito, proporcionales a la racha más larga. */
@Composable
private fun StreakRanking(streaks: List<com.openzilla.app.util.HabitStreak>, reveal: Float) {
    val longest = (streaks.maxOfOrNull { it.millis } ?: 0L).coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        streaks.forEach { streak ->
            val fraction = (streak.millis.toFloat() / longest.toFloat()).coerceIn(0f, 1f) * reveal
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        HabitCategory.iconFor(streak.iconKey),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        streak.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    Text(
                        formatDurationShort(streak.millis),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(RANKING_BAR_HEIGHT)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(RANKING_BAR_HEIGHT)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

/** Barras verticales con las recaídas de cada uno de los últimos meses. */
@Composable
private fun MonthlyChart(months: List<MonthlyRelapses>, reveal: Float) {
    val maxCount = (months.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val monthFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val calendar = remember { Calendar.getInstance() }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MONTH_CHART_HEIGHT),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            months.forEach { month ->
                val fraction = (month.count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f) * reveal
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        month.count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height((MONTH_CHART_HEIGHT - 24.dp) * fraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (month.count == 0) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            months.forEach { month ->
                calendar.set(Calendar.YEAR, month.year)
                calendar.set(Calendar.MONTH, month.month)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                Text(
                    monthFormat.format(calendar.time),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/** Anillo con el reparto de hábitos por tipo de coste, más una leyenda. */
@Composable
private fun TypeDonut(counts: Map<HabitCostType, Int>, reveal: Float) {
    val total = counts.values.sum().coerceAtLeast(1)
    val scheme = MaterialTheme.colorScheme
    val slices = listOf(
        Triple(HabitCostType.MONEY, R.string.type_money, scheme.primary),
        Triple(HabitCostType.TIME, R.string.type_time, scheme.tertiary),
        Triple(HabitCostType.EVENT, R.string.type_event, scheme.secondary)
    )
    val track = scheme.surfaceVariant

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.size(DONUT_SIZE)) {
            val stroke = size.minDimension * 0.16f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke)
            )
            var start = -90f
            slices.forEach { (type, _, color) ->
                val count = counts[type] ?: 0
                if (count == 0) return@forEach
                val sweep = 360f * count / total * reveal
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Butt)
                )
                start += 360f * count / total
            }
        }
        Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            slices.forEach { (type, labelRes, color) ->
                val count = counts[type] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(if (count == 0) track else color))
                    Text(
                        "${stringResource(labelRes)}: $count",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (count == 0) scheme.onSurfaceVariant else scheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
