package com.drautomation.intelpark.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Colours of the Intel Park mock-up. */
object IP {
    val Ink = Color(0xFF12161B)
    val Ground = Color(0xFFF6F5F1)
    val Card = Color(0xFFFFFFFF)
    val Line = Color(0xFFE3E1DA)
    val Field = Color(0xFFD8D5CC)
    val Muted = Color(0xFF5A6069)
    val Body = Color(0xFF3B4048)
    val Yellow = Color(0xFFFFD23F)
    val Blue = Color(0xFF2547D0)
    val BlueDark = Color(0xFF1A3399)
    val BlueSoft = Color(0xFFE4E9FB)
    val Amber = Color(0xFF8A3A06)
    val AmberSoft = Color(0xFFFCEBD9)
    val Segment = Color(0xFFE6E4DC)
    val Dark2 = Color(0xFF262C34)
    val OnDarkMuted = Color(0xFFB9BEC6)
}

private val Colors = lightColorScheme(
    primary = IP.Ink,
    onPrimary = Color.White,
    secondary = IP.Yellow,
    onSecondary = IP.Ink,
    tertiary = IP.Blue,
    background = IP.Ground,
    onBackground = IP.Ink,
    surface = IP.Card,
    onSurface = IP.Ink,
    surfaceVariant = IP.Segment,
    onSurfaceVariant = IP.Muted,
    outline = IP.Field,
    error = IP.Amber,
)

private val Type = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.ExtraBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp),
)

@Composable
fun IntelParkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Type, content = content)
}
