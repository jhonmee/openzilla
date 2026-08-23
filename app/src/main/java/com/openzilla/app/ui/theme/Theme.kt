package com.openzilla.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.openzilla.app.data.ThemeMode

/** Material You (wallpaper-based) colors only exist from Android 12 onwards. */
val dynamicColorAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun OpenZillaTheme(
    mode: ThemeMode,
    seedLight: Color,
    seedDark: Color,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK, ThemeMode.OLED -> true
        ThemeMode.LIGHT -> false
    }
    val useOled = mode == ThemeMode.OLED
    val context = LocalContext.current

    // La comprobación de versión va escrita aquí mismo (y no escondida en una variable) para
    // que tanto el compilador como lint vean que estas llamadas sólo ocurren en Android 12+.
    val dynamicScheme: ColorScheme? = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }

    val scheme = when {
        // OLED conserva el acento (elegido a mano o tomado del fondo de pantalla) y sólo
        // reemplaza los grises por negro puro.
        useOled -> oledColorScheme(
            primary = dynamicScheme?.primary ?: seedDark,
            onPrimary = dynamicScheme?.onPrimary ?: Color.White,
            secondary = dynamicScheme?.secondary ?: tone(seedDark, 0.35f, towardWhite = true),
            tertiary = dynamicScheme?.tertiary ?: tone(seedDark, 0.5f, towardWhite = true)
        )
        dynamicScheme != null -> dynamicScheme
        useDark -> darkColorScheme(
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
            surfaceContainerLowest = NeutralDark.containerLowest,
            surfaceContainerLow = NeutralDark.containerLow,
            surfaceContainer = NeutralDark.container,
            surfaceContainerHigh = NeutralDark.containerHigh,
            surfaceContainerHighest = NeutralDark.containerHighest,
            outline = NeutralDark.outline,
            error = Color(0xFFCF6679)
        )
        else -> lightColorScheme(
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
            surfaceContainerLowest = NeutralLight.containerLowest,
            surfaceContainerLow = NeutralLight.containerLow,
            surfaceContainer = NeutralLight.container,
            surfaceContainerHigh = NeutralLight.containerHigh,
            surfaceContainerHighest = NeutralLight.containerHighest,
            outline = NeutralLight.outline,
            error = Color(0xFFB3261E)
        )
    }

    MaterialTheme(colorScheme = scheme, typography = OpenZillaTypography, content = content)
}

private fun oledColorScheme(primary: Color, onPrimary: Color, secondary: Color, tertiary: Color): ColorScheme =
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        secondary = secondary,
        tertiary = tertiary,
        background = NeutralOled.background,
        onBackground = NeutralOled.onSurface,
        surface = NeutralOled.surface,
        onSurface = NeutralOled.onSurface,
        surfaceVariant = NeutralOled.surfaceVariant,
        onSurfaceVariant = NeutralOled.onSurfaceMuted,
        surfaceContainerLowest = NeutralOled.containerLowest,
        surfaceContainerLow = NeutralOled.containerLow,
        surfaceContainer = NeutralOled.container,
        surfaceContainerHigh = NeutralOled.containerHigh,
        surfaceContainerHighest = NeutralOled.containerHighest,
        outline = NeutralOled.outline,
        error = Color(0xFFCF6679)
    )
