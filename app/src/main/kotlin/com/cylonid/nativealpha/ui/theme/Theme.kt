package com.cylonid.nativealpha.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WAOSDarkColorScheme = darkColorScheme(
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

@Composable
fun WAOSTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = WAOSDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BgDeep.toArgb()
            window.navigationBarColor = BgDeep.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
