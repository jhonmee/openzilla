package com.openzilla.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.openzilla.app.data.ThemeMode

@Composable
fun OpenZillaTheme(
    mode: ThemeMode,
    seedLight: Color,
    seedDark: Color,
    content: @Composable () -> Unit
) {
    val useDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val scheme = if (useDark) {
        darkColorScheme(
            primary = seedDark,
            onPrimary = Color.White,
            secondary = tone(seedDark, 0.35f, towardWhite = true),
            tertiary = tone(seedDark, 0.5f, towardWhite = true),
            background = NeutralDark.background,
            onBackground = NeutralDark.onSurface,
            surface = NeutralDark.surface,
            onSurface = NeutralDark.onSurface,
            surfaceVariant = NeutralDark.surfaceVariant,
            onSurfaceVariant = NeutralDark.onSurfaceMuted,
            outline = NeutralDark.outline,
            error = Color(0xFFCF6679)
        )
    } else {
        lightColorScheme(
            primary = seedLight,
            onPrimary = Color.White,
            secondary = tone(seedLight, 0.25f, towardWhite = false),
            tertiary = tone(seedLight, 0.35f, towardWhite = false),
            background = NeutralLight.background,
            onBackground = NeutralLight.onSurface,
            surface = NeutralLight.surface,
            onSurface = NeutralLight.onSurface,
            surfaceVariant = NeutralLight.surfaceVariant,
            onSurfaceVariant = NeutralLight.onSurfaceMuted,
            outline = NeutralLight.outline,
            error = Color(0xFFB3261E)
        )
    }

    MaterialTheme(colorScheme = scheme, typography = OpenZillaTypography, content = content)
}
