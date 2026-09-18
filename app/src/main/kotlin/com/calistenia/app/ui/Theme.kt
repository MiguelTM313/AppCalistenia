package com.calistenia.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCBF59A), onPrimary = Color(0xFF17270E),
    primaryContainer = Color(0xFF293E20), onPrimaryContainer = Color(0xFFE0FFC4),
    secondary = Color(0xFF8EDCCB), onSecondary = Color(0xFF07372E),
    secondaryContainer = Color(0xFF193D35), onSecondaryContainer = Color(0xFFB8F3E4),
    background = Color(0xFF0D1512), onBackground = Color(0xFFEAF0E9),
    surface = Color(0xFF141F1A), onSurface = Color(0xFFEAF0E9),
    surfaceVariant = Color(0xFF24322A), onSurfaceVariant = Color(0xFFB5C6B9),
    outline = Color(0xFF6F8375), outlineVariant = Color(0xFF33473B),
    error = Color(0xFFFFB4AB), errorContainer = Color(0xFF512821), onErrorContainer = Color(0xFFFFDAD5)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp)
)

@Composable fun CalisthenicsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, typography = AppTypography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)), content = content)
}
