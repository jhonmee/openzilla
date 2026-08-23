package com.openzilla.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * Whether touch feedback is on, provided once from the Activity so every screen can read it
 * without passing it down through every composable.
 */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/**
 * Thin wrapper over the platform's standard haptic constants.
 *
 * It goes through [View.performHapticFeedback], which needs no permission at all (the app
 * still declares nothing beyond notifications) and honours the system-wide "touch vibration"
 * switch — so the phone's own setting always wins, and OpenZilla's toggle only ever makes it
 * quieter, never louder.
 */
class OpenZillaHaptics(private val view: View, private val enabled: Boolean) {

    /** Light tick for ordinary taps: buttons, cards, list rows. */
    fun tap() = perform(HapticFeedbackConstants.CONTEXT_CLICK)

    /** Stronger pulse: an element has been picked up and can now be dragged. */
    fun longPress() = perform(HapticFeedbackConstants.LONG_PRESS)

    /** Very short tick used while something moves under the finger. */
    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)

    /** Something was committed — a relapse recorded, a habit deleted. */
    fun confirm() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.LONG_PRESS
    )

    /**
     * Tap that ignores OpenZilla's own switch (the system setting still applies). Used only
     * when turning the option on, so the feedback can be felt at that very moment — the new
     * value has not reached this object yet.
     */
    fun tapAlways() {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    private fun perform(constant: Int) {
        if (enabled) view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(): OpenZillaHaptics {
    val view = LocalView.current
    val enabled = LocalHapticsEnabled.current
    return remember(view, enabled) { OpenZillaHaptics(view, enabled) }
}
