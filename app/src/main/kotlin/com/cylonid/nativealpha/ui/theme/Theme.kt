package com.cylonid.nativealpha.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat

private fun darkScheme() = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = BgDeep,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanLight,
    secondary = VioletSecondary,
    onSecondary = BgDeep,
    secondaryContainer = VioletContainer,
    onSecondaryContainer = VioletLight,
    tertiary = TealTertiary,
    onTertiary = BgDeep,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = TealLight,
    error = ErrorRed,
    onError = BgDeep,
    errorContainer = ErrorRedContainer,
    onErrorContainer = ErrorRedLight,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = BgMedium,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = TextMuted
)

private fun lightScheme() = lightColorScheme(
    primary = CyanPrimary,
    onPrimary = BgMedium,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanDark,
    secondary = VioletSecondary,
    onSecondary = BgMedium,
    secondaryContainer = VioletContainer,
    onSecondaryContainer = VioletDark,
    tertiary = TealTertiary,
    onTertiary = BgMedium,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = TealDark,
    error = ErrorRed,
    onError = BgMedium,
    errorContainer = ErrorRedContainer,
    onErrorContainer = ErrorRedDark,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = BgMedium,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = TextMuted
)

private fun matrixScheme() = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = BgDeep,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanLight,
    secondary = VioletSecondary,
    onSecondary = BgDeep,
    secondaryContainer = VioletContainer,
    onSecondaryContainer = VioletLight,
    tertiary = TealTertiary,
    onTertiary = BgDeep,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = TealLight,
    error = ErrorRed,
    onError = BgDeep,
    errorContainer = ErrorRedContainer,
    onErrorContainer = ErrorRedLight,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = BgMedium,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = TextMuted
)

/**
 * Returns a Typography that swaps every default style to a monospace family
 * when the active theme is "Matrix", giving the whole app a terminal feel.
 * Other themes keep the existing Inter-style typography unchanged.
 */
private fun typographyFor(effective: String): Typography {
    if (effective != "Matrix") return Typography
    fun ms(s: androidx.compose.ui.text.TextStyle) = s.copy(fontFamily = FontFamily.Monospace)
    return Typography(
        displayLarge = ms(Typography.displayLarge),
        displayMedium = ms(Typography.displayMedium),
        displaySmall = ms(Typography.displaySmall),
        headlineLarge = ms(Typography.headlineLarge),
        headlineMedium = ms(Typography.headlineMedium),
        headlineSmall = ms(Typography.headlineSmall),
        titleLarge = ms(Typography.titleLarge),
        titleMedium = ms(Typography.titleMedium),
        titleSmall = ms(Typography.titleSmall),
        bodyLarge = ms(Typography.bodyLarge),
        bodyMedium = ms(Typography.bodyMedium),
        bodySmall = ms(Typography.bodySmall),
        labelLarge = ms(Typography.labelLarge),
        labelMedium = ms(Typography.labelMedium),
        labelSmall = ms(Typography.labelSmall),
    )
}

/**
 * Root theme wrapper. The default value pulls the active theme from
 * [ThemeState], so updating Settings → "App Theme" instantly re-themes every
 * activity that wraps its content in WAOSTheme without an activity restart.
 *
 * Picks one of three palettes (Dark / Light / Matrix), with "System" mapping
 * to Dark or Light based on the OS dark-mode flag. Status bar color and icon
 * brightness are kept in sync so the whole window — not just the content area
 * — switches with the theme.
 */
@Composable
fun WAOSTheme(
    themeMode: String = ThemeState.mode,
    content: @Composable () -> Unit
) {
    val isSysDark = isSystemInDarkTheme()
    SideEffect { ThemeState.isSystemDark = isSysDark }

    val effective = when (themeMode) {
        "System" -> if (isSysDark) "Dark" else "Light"
        "Light", "Matrix", "Dark" -> themeMode
        else -> "Dark"
    }
    val colorScheme = when (effective) {
        "Light" -> lightScheme()
        "Matrix" -> matrixScheme()
        else -> darkScheme()
    }
    val isLight = effective == "Light"

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = isLight
            controller.isAppearanceLightNavigationBars = isLight
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyFor(effective),
        content = content
    )
}
