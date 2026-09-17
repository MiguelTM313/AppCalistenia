package com.calistenia.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(primary = Color(0xFFB6F36B), onPrimary = Color(0xFF182600), secondary = Color(0xFF83D9C7), background = Color(0xFF101410), surface = Color(0xFF191E19), error = Color(0xFFFFB4AB))

@Composable fun CalisthenicsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, typography = Typography(), content = content)
}
