package com.selfspotify.oneum.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Lime = Color(0xFFC3F36B)
val Ink = Color(0xFF101210)

private val DarkColors = darkColorScheme(
    primary = Lime, onPrimary = Ink, primaryContainer = Color(0xFF273819), onPrimaryContainer = Lime,
    secondary = Lime, onSecondary = Ink, secondaryContainer = Color(0xFF273819), onSecondaryContainer = Lime,
    tertiary = Color(0xFFD9E8BE), onTertiary = Ink, inversePrimary = Color(0xFF426519),
    background = Ink, onBackground = Color(0xFFF1F3EC), surface = Color(0xFF191D19),
    onSurface = Color(0xFFF1F3EC), surfaceVariant = Color(0xFF232823), onSurfaceVariant = Color(0xFF98A293),
    outline = Color(0xFF353D33), outlineVariant = Color(0xFF292F27),
    error = Color(0xFFFFB4AB), errorContainer = Color(0xFF442622),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF426519), onPrimary = Color.White,
    primaryContainer = Color(0xFFE2F1CC), onPrimaryContainer = Color(0xFF304B12),
    secondary = Color(0xFF426519), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2F1CC), onSecondaryContainer = Color(0xFF304B12),
    tertiary = Color(0xFF527A37), onTertiary = Color.White, inversePrimary = Lime,
    background = Color(0xFFFAFAF7), onBackground = Color(0xFF1B241A),
    surface = Color.White, onSurface = Color(0xFF1B241A),
    surfaceVariant = Color(0xFFF0F2E9), onSurfaceVariant = Color(0xFF687160),
    outline = Color(0xFFCAD0C2), outlineVariant = Color(0xFFE4E8DD),
    error = Color(0xFFB3261E), errorContainer = Color(0xFFFCE9E6),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 43.sp, letterSpacing = (-1.3).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 36.sp, letterSpacing = (-0.8).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 29.sp, letterSpacing = (-0.5).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 11.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp),
)

@Composable
fun OneumTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography, content = content)
}
