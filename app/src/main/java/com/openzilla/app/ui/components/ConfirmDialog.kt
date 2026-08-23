package com.openzilla.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.openzilla.app.R

/**
 * Every destructive action in the app (delete a habit, reset a streak, wipe all data) goes
 * through this single dialog. Centralizing it means there is exactly one place that decides
 * "yes, actually delete" — no screen can accidentally skip confirmation.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = stringResource(R.string.action_delete),
    dismissLabel: String = stringResource(R.string.action_cancel),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } }
    )
}
