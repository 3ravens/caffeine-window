package com.caffeinewindow.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Every Material role is set on purpose. A partially specified ColorScheme falls back to the
// Material baseline (purple), which leaks into chips, sliders, text-field outlines and dividers.

private val LightPrimary = Color(0xFF9A5B34)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFF3E4D8)
private val LightOnPrimaryContainer = Color(0xFF5A3219)

private val LightSecondary = Color(0xFF6B635B)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFEFE7DE)
private val LightOnSecondaryContainer = Color(0xFF3A2A1C)

private val LightTertiary = Color(0xFF3F6F55)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFDCE9DF)
private val LightOnTertiaryContainer = Color(0xFF16301F)

private val LightBackground = Color(0xFFF7F5F2)
private val LightOnBackground = Color(0xFF1F1B17)
private val LightSurface = Color(0xFFF7F5F2)
private val LightOnSurface = Color(0xFF1F1B17)
private val LightSurfaceVariant = Color(0xFFEFECE8)
private val LightOnSurfaceVariant = Color(0xFF6B635B)

private val LightOutline = Color(0xFFB9AFA4)
private val LightOutlineVariant = Color(0xFFE2DDD6)

private val LightError = Color(0xFFB3261E)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFF9DEDC)
private val LightOnErrorContainer = Color(0xFF410E0B)

internal val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = LightPrimary,
    surfaceDim = Color(0xFFDED9D2),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEFECE8),
    surfaceContainerHigh = Color(0xFFE8E4DF),
    surfaceContainerHighest = Color(0xFFE2DDD6),
    inverseSurface = Color(0xFF33302C),
    inverseOnSurface = Color(0xFFF4F0EC),
    inversePrimary = Color(0xFFE0A57E),
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = Color(0xFF000000),
)

private val DarkPrimary = Color(0xFFE0A57E)
private val DarkOnPrimary = Color(0xFF3A1E0C)
private val DarkPrimaryContainer = Color(0xFF4A2C18)
private val DarkOnPrimaryContainer = Color(0xFFF3E4D8)

private val DarkSecondary = Color(0xFFA9A096)
private val DarkOnSecondary = Color(0xFF151311)
private val DarkSecondaryContainer = Color(0xFF2E2924)
private val DarkOnSecondaryContainer = Color(0xFFEDE8E2)

private val DarkTertiary = Color(0xFF8CC4A3)
private val DarkOnTertiary = Color(0xFF0E2A1B)
private val DarkTertiaryContainer = Color(0xFF22392C)
private val DarkOnTertiaryContainer = Color(0xFFC9E4D4)

private val DarkBackground = Color(0xFF151311)
private val DarkOnBackground = Color(0xFFEDE8E2)
private val DarkSurface = Color(0xFF151311)
private val DarkOnSurface = Color(0xFFEDE8E2)
private val DarkSurfaceVariant = Color(0xFF28241F)
private val DarkOnSurfaceVariant = Color(0xFFA9A096)

private val DarkOutline = Color(0xFF6B635B)
private val DarkOutlineVariant = Color(0xFF3A352F)

internal val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = DarkPrimary,
    surfaceDim = Color(0xFF151311),
    surfaceBright = Color(0xFF3B3630),
    surfaceContainerLowest = Color(0xFF100E0D),
    surfaceContainerLow = Color(0xFF1E1B18),
    surfaceContainer = Color(0xFF28241F),
    surfaceContainerHigh = Color(0xFF332E29),
    surfaceContainerHighest = Color(0xFF3D3731),
    inverseSurface = Color(0xFFEDE8E2),
    inverseOnSurface = Color(0xFF33302C),
    inversePrimary = Color(0xFF9A5B34),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = Color(0xFF000000),
)

/**
 * Tones that Material does not have a role for. Only "attention, but not an error"
 * (missing reliability permissions) lives here today.
 */
@Immutable
data class ExtendedColors(
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

internal val LightExtended = ExtendedColors(
    warning = Color(0xFF8A5A00),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFF6EBD3),
    onWarningContainer = Color(0xFF8A5A00),
)

internal val DarkExtended = ExtendedColors(
    warning = Color(0xFFE8C27A),
    onWarning = Color(0xFF2E2616),
    warningContainer = Color(0xFF2E2616),
    onWarningContainer = Color(0xFFE8C27A),
)

internal val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

val MaterialTheme.extended: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
