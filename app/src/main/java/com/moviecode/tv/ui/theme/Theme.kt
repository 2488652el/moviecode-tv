package com.moviecode.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 增强版深色配色方案
private val EnhancedDarkColorScheme = darkColorScheme(
    // 主色调
    primary = GradientStart,
    onPrimary = Color.White,
    primaryContainer = GradientEnd,
    onPrimaryContainer = Color.White,

    // 次要色调
    secondary = GradientEnd,
    onSecondary = Color.White,
    secondaryContainer = GradientAccent,
    onSecondaryContainer = Color.White,

    // 背景色
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = BackgroundCard,
    onSurface = TextPrimary,

    // 表面变体
    surfaceVariant = BackgroundCardHover,
    onSurfaceVariant = TextSecondary,

    // 错误色
    error = ErrorRed,
    onError = Color.White,

    // 轮廓
    outline = Divider,
    outlineVariant = GlassBorder
)

@Composable
fun MovieCodeTVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EnhancedDarkColorScheme,
        content = content
    )
}
