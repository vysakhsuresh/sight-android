package com.layerbit.ikshana.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.layerbit.ikshana.R

/**
 * Ink with one Iris band (brief, section 4), palette from Stave's Theme.kt; type as LayerLink
 * sets it (Space Grotesk for every word, the window's font too), by the owner's choice, 12 Nov.
 *
 * The family base is the cool blue-black the icon sits on; each app spends one saturated colour
 * on the primary action alone. Ikshana's is Iris, the icon's eye blue: the active mode, the
 * shutter and the camera frame's edge. The frame gate's state is the semantic trio. Nothing else
 * is coloured, so the one bright thing on the screen is the thing that is on.
 *
 * Dark only in P0. The camera is the home screen and its preview is the background; paper (the
 * light theme) arrives with the low-vision mode in P7, where it is measured, not guessed.
 */
object Ink {
    val Background = Color(0xFF070A12)
    val Surface = Color(0xFF121828)
    val SurfaceDim = Color(0xFF0C1120)
    val Border = Color(0xFF1F2940)
    val BorderStrong = Color(0xFF35425F)
    val Text = Color(0xFFF6F7FB)
    val Muted = Color(0xFFA8B3C9)
    /** Iris on ink, `#6FD3FF` (brief, section 4). */
    val Iris = Color(0xFF6FD3FF)
    val OnIris = Color(0xFF00131D)
    val Good = Color(0xFF4CCB8F)
    val Almost = Color(0xFFF0A93D)
    val Closed = Color(0xFFE8705F)
}

/** Targets are 64 dp here, not 48, because the finger is the eye (brief, section 4). */
val Target = 64.dp

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

/**
 * Numbers in the platform monospace with tabular digits, as LayerLink sets them
 * (../layerlink-android/app/src/main/res/layout/activity_main.xml, `monospace` + `tnum`): the
 * owner chose LayerLink's type for Ikshana on 12 Nov 2026, and LayerLink carries no Fira Code.
 */
val Numerals = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum")

// Every paragraph takes its direction from its own first letters (Stave's lesson on RTL phones).
private val ByContent = TextDirection.Content

/**
 * The family's scale with the body at 18 sp, not 15 (brief, section 4). No letter-spacing on
 * body text: on Malayalam or Devanagari it pulls conjuncts apart (Stave, 11 November).
 */
private val IkshanaTypography = Typography(
    headlineMedium = TextStyle(textDirection = ByContent, fontFamily = SpaceGrotesk, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp),
    titleLarge = TextStyle(textDirection = ByContent, fontFamily = SpaceGrotesk, fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(textDirection = ByContent, fontFamily = SpaceGrotesk, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(textDirection = ByContent, fontFamily = SpaceGrotesk, fontSize = 18.sp, fontWeight = FontWeight.Normal, lineHeight = 26.sp),
    bodyMedium = TextStyle(textDirection = ByContent, fontFamily = SpaceGrotesk, fontSize = 16.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(textDirection = ByContent, fontFamily = SpaceGrotesk, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun IkshanaTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Ink.Iris, onPrimary = Ink.OnIris, background = Ink.Background, onBackground = Ink.Text,
        surface = Ink.Surface, onSurface = Ink.Text, surfaceVariant = Ink.SurfaceDim, onSurfaceVariant = Ink.Muted,
        outline = Ink.Border, error = Ink.Closed
    )
    MaterialTheme(colorScheme = scheme, typography = IkshanaTypography) {
        CompositionLocalProvider(LocalTextStyle provides IkshanaTypography.bodyLarge, content = content)
    }
}
