package com.openzilla.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Default accent, close in spirit to the reference app's coral-red — but this is just the default; the user can change it in Settings. */
val DefaultSeed = Color(0xFFE0526B)

/**
 * Builds a small, flat set of tones around one seed color by blending toward white/black.
 * Deliberately simple (no HCT/Material-You color science, no extra dependency) — it produces
 * a plain, predictable palette rather than anything trying to look clever.
 */
fun tone(seed: Color, amount: Float, towardWhite: Boolean): Color =
    lerp(seed, if (towardWhite) Color.White else Color.Black, amount.coerceIn(0f, 1f))

object NeutralDark {
    val background = Color(0xFF121212)
    val surface = Color(0xFF1B1B1B)
    val surfaceVariant = Color(0xFF262626)
    val onSurface = Color(0xFFECECEC)
    val onSurfaceMuted = Color(0xFFA3A3A3)
    val outline = Color(0xFF3A3A3A)
}

/** Small, fixed set of preset accent colors offered in Settings — keeps color choice simple (swatches, not a full picker). */
val PRESET_COLORS = listOf(
    Color(0xFFE0526B), // coral red (default)
    Color(0xFFE07B39), // orange
    Color(0xFFD9B44A), // amber
    Color(0xFF4CAF7D), // green
    Color(0xFF3E9CB8), // teal
    Color(0xFF4A73D9), // blue
    Color(0xFF8B5FBF), // purple
    Color(0xFF8D8D8D)  // neutral gray
)

object NeutralLight {
    val background = Color(0xFFF7F7F8)
    val surface = Color(0xFFFFFFFF)
    val surfaceVariant = Color(0xFFEDEDED)
    val onSurface = Color(0xFF1B1B1B)
    val onSurfaceMuted = Color(0xFF6B6B6B)
    val outline = Color(0xFFDDDDDD)
}
