package com.openzilla.app.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.openzilla.app.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openzilla.app.data.HabitEntity
import com.openzilla.app.util.TROPHIES

@Composable
fun TrophiesTab(habit: HabitEntity) {
    val elapsed = (System.currentTimeMillis() - habit.startedAt).coerceAtLeast(0)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(TROPHIES) { trophy ->
            val achieved = elapsed >= trophy.durationMillis
            val progress = (elapsed.toFloat() / trophy.durationMillis.toFloat()).coerceIn(0f, 1f)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (achieved) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                )
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (achieved) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = if (achieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(trophy.labelRes), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(if (achieved) R.string.trophy_achieved else R.string.trophy_in_progress),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
