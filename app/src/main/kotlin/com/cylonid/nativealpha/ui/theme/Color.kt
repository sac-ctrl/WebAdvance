package com.cylonid.nativealpha.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * App-wide theme switch. Reading [mode] inside any composable subscribes
 * that composable to recompose whenever the user toggles the theme in
 * Settings, so the entire UI re-paints live without restarting the activity.
 *
 * Valid values: "System", "Dark", "Light", "Matrix".
 *
 * [isSystemDark] is kept in sync from [WAOSTheme] (the only place we have a
 * Composable scope that can call isSystemInDarkTheme()) so the [effective]
 * resolver below can answer correctly when the user picks "System".
 */
object ThemeState {
    var mode by mutableStateOf("Dark")
        private set
    var isSystemDark by mutableStateOf(true)

    fun applyMode(value: String) { mode = value }

    val effective: String
        get() = when (mode) {
            "System" -> if (isSystemDark) "Dark" else "Light"
            "Light", "Matrix", "Dark" -> mode
            else -> "Dark"
        }
}

internal data class WaosPalette(
    val cyanPrimary: Color, val cyanLight: Color, val cyanDark: Color, val cyanContainer: Color,
    val violetSecondary: Color, val violetLight: Color, val violetDark: Color, val violetContainer: Color,
    val tealTertiary: Color, val tealLight: Color, val tealDark: Color, val tealContainer: Color,
    val errorRed: Color, val errorRedLight: Color, val errorRedDark: Color, val errorRedContainer: Color,
    val bgDeep: Color, val bgDark: Color, val bgMedium: Color,
    val cardSurface: Color, val cardBorder: Color, val cardGlow: Color,
    val textPrimary: Color, val textSecondary: Color, val textMuted: Color,
    val statusActive: Color, val statusBg: Color, val statusError: Color, val statusInactive: Color,
    val gradCyanStart: Color, val gradCyanEnd: Color,
    val gradVioletStart: Color, val gradVioletEnd: Color,
    val gradOrangeStart: Color, val gradOrangeEnd: Color,
    val gradPinkStart: Color, val gradPinkEnd: Color,
    val gradGreenStart: Color, val gradGreenEnd: Color,
    val gradBlueStart: Color, val gradBlueEnd: Color,
    val gradRedStart: Color, val gradRedEnd: Color,
    val gradYellowStart: Color, val gradYellowEnd: Color,
)

// Dark = the original NexWeb cyber palette (unchanged from before).
private val DarkPalette = WaosPalette(
    cyanPrimary = Color(0xFF00E5FF), cyanLight = Color(0xFF80F0FF), cyanDark = Color(0xFF00B2CC), cyanContainer = Color(0xFF003A47),
    violetSecondary = Color(0xFF9C7DFF), violetLight = Color(0xFFD0B8FF), violetDark = Color(0xFF6B4FCC), violetContainer = Color(0xFF2A1A5E),
    tealTertiary = Color(0xFF00FFAB), tealLight = Color(0xFF8DFFD6), tealDark = Color(0xFF00CC88), tealContainer = Color(0xFF003B27),
    errorRed = Color(0xFFFF5370), errorRedLight = Color(0xFFFFB3BC), errorRedDark = Color(0xFFCC3055), errorRedContainer = Color(0xFF4D0017),
    bgDeep = Color(0xFF060912), bgDark = Color(0xFF0A0E1A), bgMedium = Color(0xFF0F1523),
    cardSurface = Color(0xFF141C2E), cardBorder = Color(0xFF1E2D45), cardGlow = Color(0xFF1A2540),
    textPrimary = Color(0xFFE8F4FF), textSecondary = Color(0xFF8BA3C4), textMuted = Color(0xFF4A6080),
    statusActive = Color(0xFF00FF88), statusBg = Color(0xFFFFB800), statusError = Color(0xFFFF4466), statusInactive = Color(0xFF4A6080),
    gradCyanStart = Color(0xFF00BFA5), gradCyanEnd = Color(0xFF00E5FF),
    gradVioletStart = Color(0xFF7C4DFF), gradVioletEnd = Color(0xFFB388FF),
    gradOrangeStart = Color(0xFFFF6D00), gradOrangeEnd = Color(0xFFFFAB40),
    gradPinkStart = Color(0xFFE91E63), gradPinkEnd = Color(0xFFFF80AB),
    gradGreenStart = Color(0xFF00C853), gradGreenEnd = Color(0xFF69FF47),
    gradBlueStart = Color(0xFF1565C0), gradBlueEnd = Color(0xFF448AFF),
    gradRedStart = Color(0xFFD50000), gradRedEnd = Color(0xFFFF5370),
    gradYellowStart = Color(0xFFF57F17), gradYellowEnd = Color(0xFFFFD740),
)

// Light = clean off-white surfaces with the same cyan/violet accent identity,
// but darkened just enough to stay legible on white.
private val LightPalette = WaosPalette(
    cyanPrimary = Color(0xFF00838F), cyanLight = Color(0xFF4FB3BF), cyanDark = Color(0xFF005662), cyanContainer = Color(0xFFB2EBF2),
    violetSecondary = Color(0xFF5E35B1), violetLight = Color(0xFF9162E4), violetDark = Color(0xFF280680), violetContainer = Color(0xFFD1C4E9),
    tealTertiary = Color(0xFF00897B), tealLight = Color(0xFF4DB6AC), tealDark = Color(0xFF005B4F), tealContainer = Color(0xFFB2DFDB),
    errorRed = Color(0xFFD32F2F), errorRedLight = Color(0xFFEF5350), errorRedDark = Color(0xFF9A0007), errorRedContainer = Color(0xFFFFCDD2),
    bgDeep = Color(0xFFF5F7FB), bgDark = Color(0xFFE8ECF4), bgMedium = Color(0xFFFFFFFF),
    cardSurface = Color(0xFFFFFFFF), cardBorder = Color(0xFFE2E8F0), cardGlow = Color(0xFFEDF2FB),
    textPrimary = Color(0xFF0F1523), textSecondary = Color(0xFF5B6B85), textMuted = Color(0xFF9AA8BF),
    statusActive = Color(0xFF1B873F), statusBg = Color(0xFFE6A100), statusError = Color(0xFFD32F2F), statusInactive = Color(0xFF9AA8BF),
    gradCyanStart = Color(0xFF00ACC1), gradCyanEnd = Color(0xFF26C6DA),
    gradVioletStart = Color(0xFF673AB7), gradVioletEnd = Color(0xFF9575CD),
    gradOrangeStart = Color(0xFFEF6C00), gradOrangeEnd = Color(0xFFFF9800),
    gradPinkStart = Color(0xFFC2185B), gradPinkEnd = Color(0xFFEC407A),
    gradGreenStart = Color(0xFF2E7D32), gradGreenEnd = Color(0xFF66BB6A),
    gradBlueStart = Color(0xFF1565C0), gradBlueEnd = Color(0xFF42A5F5),
    gradRedStart = Color(0xFFC62828), gradRedEnd = Color(0xFFEF5350),
    gradYellowStart = Color(0xFFF9A825), gradYellowEnd = Color(0xFFFFD54F),
)

// Matrix = pure terminal aesthetic: black backgrounds, phosphor-green text and
// accents. Every accent collapses to a green family so chrome, cards and
// gradients all read as "in the matrix".
private val MatrixPalette = WaosPalette(
    cyanPrimary = Color(0xFF00FF41), cyanLight = Color(0xFF66FF8C), cyanDark = Color(0xFF00B82E), cyanContainer = Color(0xFF002908),
    violetSecondary = Color(0xFF33FF66), violetLight = Color(0xFF99FFB3), violetDark = Color(0xFF00CC44), violetContainer = Color(0xFF002912),
    tealTertiary = Color(0xFF00CC33), tealLight = Color(0xFF66FF8C), tealDark = Color(0xFF008822), tealContainer = Color(0xFF001F0A),
    errorRed = Color(0xFFFF3333), errorRedLight = Color(0xFFFF8080), errorRedDark = Color(0xFFB30000), errorRedContainer = Color(0xFF330000),
    bgDeep = Color(0xFF000000), bgDark = Color(0xFF000503), bgMedium = Color(0xFF020A04),
    cardSurface = Color(0xFF031608), cardBorder = Color(0xFF0F3D14), cardGlow = Color(0xFF062A0E),
    textPrimary = Color(0xFF00FF41), textSecondary = Color(0xFF00CC33), textMuted = Color(0xFF0A8A2A),
    statusActive = Color(0xFF00FF41), statusBg = Color(0xFFCCFF00), statusError = Color(0xFFFF3333), statusInactive = Color(0xFF0A8A2A),
    gradCyanStart = Color(0xFF003311), gradCyanEnd = Color(0xFF00FF41),
    gradVioletStart = Color(0xFF002908), gradVioletEnd = Color(0xFF33FF66),
    gradOrangeStart = Color(0xFF003311), gradOrangeEnd = Color(0xFF99FF66),
    gradPinkStart = Color(0xFF002908), gradPinkEnd = Color(0xFF66FF8C),
    gradGreenStart = Color(0xFF003311), gradGreenEnd = Color(0xFF00FF41),
    gradBlueStart = Color(0xFF001F0A), gradBlueEnd = Color(0xFF33FF66),
    gradRedStart = Color(0xFF330000), gradRedEnd = Color(0xFFFF3333),
    gradYellowStart = Color(0xFF002908), gradYellowEnd = Color(0xFFCCFF00),
)

internal val currentPalette: WaosPalette
    get() = when (ThemeState.effective) {
        "Light" -> LightPalette
        "Matrix" -> MatrixPalette
        else -> DarkPalette
    }

// Public color names used throughout the UI. Each one is a property getter so
// that any composable referencing it automatically resubscribes to ThemeState
// and recomposes when the user flips themes.
val CyanPrimary: Color get() = currentPalette.cyanPrimary
val CyanLight: Color get() = currentPalette.cyanLight
val CyanDark: Color get() = currentPalette.cyanDark
val CyanContainer: Color get() = currentPalette.cyanContainer

val VioletSecondary: Color get() = currentPalette.violetSecondary
val VioletLight: Color get() = currentPalette.violetLight
val VioletDark: Color get() = currentPalette.violetDark
val VioletContainer: Color get() = currentPalette.violetContainer

val TealTertiary: Color get() = currentPalette.tealTertiary
val TealLight: Color get() = currentPalette.tealLight
val TealDark: Color get() = currentPalette.tealDark
val TealContainer: Color get() = currentPalette.tealContainer

val ErrorRed: Color get() = currentPalette.errorRed
val ErrorRedLight: Color get() = currentPalette.errorRedLight
val ErrorRedDark: Color get() = currentPalette.errorRedDark
val ErrorRedContainer: Color get() = currentPalette.errorRedContainer

val BgDeep: Color get() = currentPalette.bgDeep
val BgDark: Color get() = currentPalette.bgDark
val BgMedium: Color get() = currentPalette.bgMedium
val CardSurface: Color get() = currentPalette.cardSurface
val CardBorder: Color get() = currentPalette.cardBorder
val CardGlow: Color get() = currentPalette.cardGlow

val TextPrimary: Color get() = currentPalette.textPrimary
val TextSecondary: Color get() = currentPalette.textSecondary
val TextMuted: Color get() = currentPalette.textMuted

val StatusActive: Color get() = currentPalette.statusActive
val StatusBg: Color get() = currentPalette.statusBg
val StatusError: Color get() = currentPalette.statusError
val StatusInactive: Color get() = currentPalette.statusInactive

val GradCyanStart: Color get() = currentPalette.gradCyanStart
val GradCyanEnd: Color get() = currentPalette.gradCyanEnd
val GradVioletStart: Color get() = currentPalette.gradVioletStart
val GradVioletEnd: Color get() = currentPalette.gradVioletEnd
val GradOrangeStart: Color get() = currentPalette.gradOrangeStart
val GradOrangeEnd: Color get() = currentPalette.gradOrangeEnd
val GradPinkStart: Color get() = currentPalette.gradPinkStart
val GradPinkEnd: Color get() = currentPalette.gradPinkEnd
val GradGreenStart: Color get() = currentPalette.gradGreenStart
val GradGreenEnd: Color get() = currentPalette.gradGreenEnd
val GradBlueStart: Color get() = currentPalette.gradBlueStart
val GradBlueEnd: Color get() = currentPalette.gradBlueEnd
val GradRedStart: Color get() = currentPalette.gradRedStart
val GradRedEnd: Color get() = currentPalette.gradRedEnd
val GradYellowStart: Color get() = currentPalette.gradYellowStart
val GradYellowEnd: Color get() = currentPalette.gradYellowEnd
