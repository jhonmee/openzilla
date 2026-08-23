package com.openzilla.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * Shared clock for everything that displays elapsed time.
 *
 * Two details it exists to get right:
 *
 * - **It wakes up with the app.** The loop is tied to the lifecycle, so it stops when the app
 *   goes to the background and starts again — publishing the current time immediately — the
 *   moment it comes back. Without this, returning to the app showed whatever the counter said
 *   before leaving until the pending delay happened to expire.
 * - **It ticks on the boundary.** Each delay is only as long as what is left of the current
 *   second (or minute), instead of a fixed interval that drifts. The number on screen changes
 *   when the real clock changes, not up to an interval later.
 *
 * One ticker per screen is enough: reading it recomposes the texts that show it and nothing
 * else, which is far cheaper than one coroutine per row.
 */
@Composable
fun rememberNowTicker(intervalMillis: Long = 1_000L): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, intervalMillis) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val current = System.currentTimeMillis()
                now.longValue = current
                delay(intervalMillis - (current % intervalMillis))
            }
        }
    }
    return now
}
