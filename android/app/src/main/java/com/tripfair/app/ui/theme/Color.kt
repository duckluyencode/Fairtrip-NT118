package com.tripfair.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light values come from the Figma design; dark values are derived from them
// (the design has no dark mode) and checked for WCAG AA contrast.

// Brand teal
private val Teal = Color(0xFF176B62)
private val TealLight = Color(0xFF7DD3C4)
private val TealContainerLight = Color(0xFFDDF1EC)
private val TealContainerDark = Color(0xFF17423C)
private val OnTealContainerDark = Color(0xFFB5ECE2)
private val OnTealDark = Color(0xFF00382F)

// Coral accent
private val Coral = Color(0xFFF06B4F)
private val CoralLight = Color(0xFFFF9E87)
private val CoralContainerLight = Color(0xFFFFF0EB)
private val CoralContainerDark = Color(0xFF4D2A21)
private val OnCoralDark = Color(0xFF5A1A0B)
private val OnCoralContainerDark = Color(0xFFFFD9CF)

// Danger red (SOS, warnings)
private val Red = Color(0xFFD92D20)
private val RedLight = Color(0xFFFF8F84)
private val RedContainerLight = Color(0xFFFEE2E2)
private val RedContainerDark = Color(0xFF5A1D18)
private val OnRedDark = Color(0xFF4A0702)
private val OnRedContainerDark = Color(0xFFFFDAD5)

// Neutrals
private val Ink = Color(0xFF17211F)
private val InkMuted = Color(0xFF66726F)
private val Paper = Color(0xFFF7F8F4)
private val Line = Color(0xFFDDE5E1)
private val NightSurface = Color(0xFF0F1614)
private val NightOnSurface = Color(0xFFE3EAE7)
private val NightOnSurfaceVariant = Color(0xFFA3B1AD)
private val NightLine = Color(0xFF34413E)

internal val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = Teal,
    inversePrimary = TealLight,
    // The design has no second accent, so secondary reuses the brand teal.
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = Teal,
    tertiary = Coral,
    onTertiary = Color.White,
    tertiaryContainer = CoralContainerLight,
    onTertiaryContainer = Coral,
    error = Red,
    onError = Color.White,
    errorContainer = RedContainerLight,
    onErrorContainer = Red,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEEF2EF),
    onSurfaceVariant = InkMuted,
    surfaceTint = Teal,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    outline = Line,
    outlineVariant = Line,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0F3EF),
    surfaceContainerHighest = Color(0xFFE9EEEB),
)

internal val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = OnTealDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = OnTealContainerDark,
    inversePrimary = Teal,
    secondary = TealLight,
    onSecondary = OnTealDark,
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = OnTealContainerDark,
    tertiary = CoralLight,
    onTertiary = OnCoralDark,
    tertiaryContainer = CoralContainerDark,
    onTertiaryContainer = OnCoralContainerDark,
    error = RedLight,
    onError = OnRedDark,
    errorContainer = RedContainerDark,
    onErrorContainer = OnRedContainerDark,
    background = NightSurface,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = Color(0xFF26302E),
    onSurfaceVariant = NightOnSurfaceVariant,
    surfaceTint = TealLight,
    inverseSurface = NightOnSurface,
    inverseOnSurface = Ink,
    outline = NightLine,
    outlineVariant = Color(0xFF2A3532),
    surfaceContainerLowest = Color(0xFF0B100F),
    surfaceContainerLow = Color(0xFF1A2422),
    surfaceContainer = Color(0xFF1A2422),
    surfaceContainerHigh = Color(0xFF222C2A),
    surfaceContainerHighest = Color(0xFF2C3734),
)

/** Colors used by the design that have no matching Material 3 role. */
@Immutable
data class ExtendedColors(
    /** Positive status text, e.g. "Không ai vượt ngân sách cá nhân". */
    val success: Color,
    /** Background and border of danger cards (SOS). */
    val errorSurface: Color,
    val errorBorder: Color,
    /** Background, border and icon circle of light teal cards (camera). */
    val primarySurface: Color,
    val primaryBorder: Color,
    val primaryIconContainer: Color,
    /** Dark overlay that keeps white text readable on photos. */
    val imageScrim: Color,
)

internal val LightExtendedColors = ExtendedColors(
    success = Color(0xFF2B7D5B),
    errorSurface = Color(0xFFFFF7F7),
    errorBorder = Color(0xFFF4C7C7),
    primarySurface = Color(0xFFF7FFF8),
    primaryBorder = Color(0xFFD7EFE6),
    primaryIconContainer = Color(0xFFE8F7F2),
    imageScrim = Color(0x800A3930),
)

internal val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF8FE0BC),
    errorSurface = Color(0xFF2A1614),
    errorBorder = Color(0xFF5A2A26),
    primarySurface = Color(0xFF13201D),
    primaryBorder = Color(0xFF24413B),
    primaryIconContainer = Color(0xFF1B3530),
    imageScrim = Color(0x800A3930),
)

internal val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Extra theme colors, read like `MaterialTheme.extendedColors.success`. */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
