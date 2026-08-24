package com.openzilla.app.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.openzilla.app.R
import com.openzilla.app.util.dayStartOf
import java.util.Calendar

/**
 * Asks *when* a relapse happened before recording it.
 *
 * It exists because guessing was wrong in both directions: the calendar used to file every
 * relapse at midday regardless of when it actually happened, and the button gave no way to
 * record one that happened earlier. Now the moment is always chosen on purpose.
 *
 * @param dayStart null to pick a full date and time; a day (midnight) to only pick the time
 *   within that day, which is what the calendar needs since the day is already chosen.
 */
@Composable
fun RelapseTimeDialog(
    dayStart: Long?,
    onDismiss: () -> Unit,
    onPicked: (Long) -> Unit
) {
    val context = LocalContext.current

    fun timeOfDayNow(): Long {
        val cal = Calendar.getInstance()
        return (cal.get(Calendar.HOUR_OF_DAY) * 3_600_000L) + (cal.get(Calendar.MINUTE) * 60_000L)
    }

    fun pickTimeOn(day: Long) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onPicked(day + hour * 3_600_000L + minute * 60_000L)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun pickDateAndTime() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                pickTimeOn(dayStartOf(cal.timeInMillis))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // No se puede recaer en el futuro.
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.relapse_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.relapse_when),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Option(stringResource(if (dayStart == null) R.string.relapse_now else R.string.relapse_time_now)) {
                    onDismiss()
                    onPicked(if (dayStart == null) System.currentTimeMillis() else dayStart + timeOfDayNow())
                }
                Option(stringResource(if (dayStart == null) R.string.relapse_pick_date else R.string.relapse_pick_time)) {
                    onDismiss()
                    if (dayStart == null) pickDateAndTime() else pickTimeOn(dayStart)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun Option(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    )
}
