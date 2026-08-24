package com.openzilla.app.ui.detail

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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openzilla.app.R
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.data.HistoryEntity
import com.openzilla.app.ui.components.rememberNowTicker
import com.openzilla.app.util.DayState
import com.openzilla.app.util.HEATMAP_WEEKS
import com.openzilla.app.util.SavingsSummary
import com.openzilla.app.util.StreakRange
import com.openzilla.app.util.computeHabitStats
import com.openzilla.app.util.formatDurationShort
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_BARS = 8
private val CHART_HEIGHT = 140.dp
private val RANGE_BAR_HEIGHT = 14.dp
private val HEATMAP_CELL = 13.dp

private data class Bar(val label: String, val millis: Long, val isCurrent: Boolean)

@Composable
fun StatsTab(habit: HabitEntity, history: List<HistoryEntity>, currencySymbol: String) {
    // Aquí las cifras se muestran en días y horas, así que medio minuto sobra; el reloj
    // compartido se encarga además de refrescar al volver de segundo plano.
    val now by rememberNowTicker(intervalMillis = 30_000L)

    val stats = remember(habit, history, now) { computeHabitStats(habit, history, now) }
    val dayFormat = remember { SimpleDateFormat("d/M", Locale.getDefault()) }
    val longFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }
    val fullFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }

    val nowLabel = stringResource(R.string.stats_now)
    val bars = remember(history, stats.range.current, nowLabel) {
        history
            .sortedBy { it.streakEnd }
            .takeLast(MAX_BARS)
            .map { Bar(dayFormat.format(Date(it.streakEnd)), (it.streakEnd - it.streakStart).coerceAtLeast(0), false) } +
            Bar(nowLabel, stats.range.current, true)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(
                stringResource(R.string.stats_since, longFormat.format(Date(habit.startedAt))),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatBox(Icons.Filled.LocalFireDepartment, stringResource(R.string.stats_current_streak), formatDurationShort(stats.range.current), Modifier.weight(1f), highlight = true)
                StatBox(Icons.Filled.EmojiEvents, stringResource(R.string.stats_best_streak), formatDurationShort(stats.range.longest), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                StatBox(Icons.Filled.Replay, stringResource(R.string.stats_relapses), stats.relapses.toString(), Modifier.weight(1f))
                StatBox(Icons.Filled.Timeline, stringResource(R.string.stats_average), formatDurationShort(stats.range.average), Modifier.weight(1f))
            }
        }

        item { SectionHeader(R.string.stats_range_title, R.string.stats_range_hint) }
        item { StreakRangeBar(stats.range) }

        item { SectionHeader(R.string.stats_heatmap_title, R.string.stats_heatmap_hint) }
        item { ConsistencyMap(stats.heatmap.map { it.state }) }

        item { SectionHeader(R.string.stats_history_title, R.string.stats_history_hint) }
        item { StreakBarChart(bars) }

        stats.savings?.let { savings ->
            item { SectionHeader(R.string.stats_ledger_title, R.string.stats_ledger_hint) }
            item { Ledger(savings, stats.savingsInHours, currencySymbol) }
        }

        if (history.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.stats_relapses_logged),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 28.dp, bottom = 8.dp)
                )
            }
            items(history.size) { index ->
                val entry = history[index]
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(fullFormat.format(Date(entry.streakEnd)), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.stats_held, formatDurationShort(entry.streakEnd - entry.streakStart)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                }
            }
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
private fun StatBox(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
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

/**
 * The whole spread of streaks on one line: the track runs from the shortest to the longest,
 * the current one fills it up to where it stands, and a notch marks the average.
 *
 * Three numbers that are usually three separate rows, read here as one picture: at a glance
 * you see whether this attempt is going better or worse than usual.
 */
@Composable
private fun StreakRangeBar(range: StreakRange) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(RANGE_BAR_HEIGHT)
                .clip(CircleShape)
                .background(scheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(range.currentPosition.coerceAtLeast(0.02f))
                    .height(RANGE_BAR_HEIGHT)
                    .clip(CircleShape)
                    .background(scheme.primary)
            )
            // La media va como una muesca sobre la barra, no como otra fila de texto.
            Box(
                Modifier
                    .fillMaxWidth(range.averagePosition)
                    .height(RANGE_BAR_HEIGHT),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    Modifier
                        .size(width = 3.dp, height = RANGE_BAR_HEIGHT)
                        .background(scheme.onSurface)
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(R.string.stats_shortest), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Text(formatDurationShort(range.shortest), style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.stats_average), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Text(formatDurationShort(range.average), style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.stats_longest), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Text(formatDurationShort(range.longest), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Twelve weeks as a grid of small squares, one per day: accent for a day held, error colour
 * for the day a streak broke, and empty for anything before there was a record.
 *
 * Rows are weekdays and columns are weeks, so a bad patch shows up as a cluster rather than
 * as a number you have to interpret.
 */
@Composable
private fun ConsistencyMap(days: List<DayState>) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(7) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(HEATMAP_WEEKS) { week ->
                    val index = week * 7 + row
                    val state = days.getOrNull(index) ?: DayState.UNTRACKED
                    Box(
                        Modifier
                            .size(HEATMAP_CELL)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                when (state) {
                                    DayState.CLEAN -> scheme.primary
                                    DayState.RELAPSE -> scheme.error
                                    DayState.UNTRACKED -> scheme.surfaceVariant
                                }
                            )
                    )
                }
            }
        }
    }
}

/** Cifras de dinero (o de horas) según el tipo de hábito. */
@Composable
private fun formatAmount(value: Double, inHours: Boolean, symbol: String): String =
    if (inHours) stringResource(R.string.stats_hours_value, "%.1f".format(value))
    else symbol + "%.2f".format(value)

@Composable
private fun Ledger(savings: SavingsSummary, inHours: Boolean, symbol: String) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        LedgerRow(stringResource(R.string.stats_saved_current), formatAmount(savings.currentStreak, inHours, symbol), scheme.primary)
        LedgerRow(stringResource(R.string.stats_saved_total), formatAmount(savings.total, inHours, symbol), scheme.primary)
        if (savings.relapseCost > 0) {
            LedgerRow(stringResource(R.string.stats_relapse_cost), "− " + formatAmount(savings.relapseCost, inHours, symbol), scheme.error)
            Text(
                stringResource(R.string.stats_relapse_cost_hint),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        LedgerRow(stringResource(R.string.stats_net), formatAmount(savings.net, inHours, symbol), scheme.onSurface, bold = true)

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.primary.copy(alpha = 0.12f))
        ) {
            Column(Modifier.padding(14.dp)) {
                PaceRow(stringResource(R.string.stats_pace_month), formatAmount(savings.monthly, inHours, symbol))
                PaceRow(stringResource(R.string.stats_pace_year), formatAmount(savings.yearly, inHours, symbol))
            }
        }
    }
}

@Composable
private fun LedgerRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}

@Composable
private fun PaceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Plain bars laid out with normal Compose layout instead of a Canvas: each bar is a rounded
 * Box whose height is a fraction of the tallest one, so labels and colors come for free and
 * there is no custom drawing to keep in sync with the theme.
 */
@Composable
private fun StreakBarChart(bars: List<Bar>) {
    val maxValue = (bars.maxOfOrNull { it.millis } ?: 0L).coerceAtLeast(1L)
    val past = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val current = MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bars.forEach { bar ->
                val fraction = (bar.millis.toFloat() / maxValue.toFloat()).coerceIn(0.03f, 1f)
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatDurationShort(bar.millis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CHART_HEIGHT * fraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (bar.isCurrent) current else past)
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bars.forEach { bar ->
                Text(
                    bar.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bar.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
