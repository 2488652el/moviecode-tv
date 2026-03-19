package com.moviecode.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TvColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryDark,
    secondary = Accent,
    onSecondary = Color.White,
    background = TvBackground,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = CardBackgroundSelected,
    onSurfaceVariant = TextSecondary,
    error = Red,
    onError = Color.White,
    outline = Divider
)

@Composable
fun MovieCodeTVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvColorScheme,
        content = content
    )
}
