package com.moviecode.tv.ui.theme

import androidx.compose.ui.graphics.Color

// ===== 核心背景层次 =====
val BackgroundDeep = Color(0xFF0A0A0C)           // 最深背景
val BackgroundPrimary = Color(0xFF101214)         // 主背景
val BackgroundElevated = Color(0xFF18181B)        // 提升层级背景
val BackgroundCard = Color(0xFF1C1C1E)            // 卡片背景
val BackgroundCardHover = Color(0xFF2C2C2E)       // 卡片悬停

// ===== 渐变色系统 =====
val GradientStart = Color(0xFF0A84FF)             // 蓝色起点
val GradientEnd = Color(0xFF5E5CE6)              // 紫色终点
val GradientAccent = Color(0xFFFF6B9D)            // 粉紫渐变

// ===== 玻璃态效果 =====
val GlassOverlay = Color(0x40FFFFFF)               // 20% 白色
val GlassBorder = Color(0x30FFFFFF)               // 12% 白色边框

// ===== 语义色彩 =====
val RatingGold = Color(0xFFFFD60A)               // 评分金色
val SuccessGreen = Color(0xFF32D74B)             // 成功绿
val WarningAmber = Color(0xFFFFD60A)             // 警告琥珀
val ErrorRed = Color(0xFFFF453A)                 // 错误红
val InfoBlue = Color(0xFF0A84FF)                 // 信息蓝

// ===== 文字层次 =====
val TextPrimary = Color(0xFFF5F5F7)              // 主要文字
val TextSecondary = Color(0xFF8E8E93)            // 次要文字
val TextTertiary = Color(0xFF636366)             // 三级文字
val TextDisabled = Color(0xFF48484A)             // 禁用文字

// ===== 兼容旧版本 =====
@Deprecated("Use BackgroundDeep instead", ReplaceWith("BackgroundDeep"))
val TvBackground = BackgroundPrimary
val CardBackground = BackgroundCard
val CardBackgroundSelected = BackgroundCardHover
val Primary = GradientStart
val PrimaryDark = Color(0xFF0066CC)
val Accent = GradientEnd
val TextOnDark = Color(0xFFFFFFFF)
val Divider = Color(0xFF38383A)
val Overlay = Color(0x80101214)
val Yellow = RatingGold
val Green = SuccessGreen
val Red = ErrorRed
