package com.moviecode.tv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviecode.tv.ui.theme.*

/**
 * 导航项枚举
 */
enum class NavigationItem(
    val title: String,
    val icon: ImageVector
) {
    HOME("首页", Icons.Filled.Home),
    MOVIES("电影", Icons.Filled.Movie),
    TV_SHOWS("电视剧", Icons.Filled.Tv),
    ANIME("动漫", Icons.Filled.VideoLibrary),
    SETTINGS("设置", Icons.Filled.Settings)
}

/**
 * 增强版玻璃态导航栏
 * 特性:
 * - 毛玻璃背景效果
 * - 渐变 Logo
 * - 焦点动画
 * - 侧边装饰光晕
 */
@Composable
fun GlassNavigationRail(
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(220.dp),
        color = BackgroundDeep.copy(alpha = 0.7f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BackgroundElevated.copy(alpha = 0.3f),
                            BackgroundDeep.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassBorder,
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(0.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo 区域
                Box(
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    Text(
                        text = "MovieCode",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // 导航项
                NavigationItem.entries.forEach { item ->
                    EnhancedNavItem(
                        item = item,
                        isSelected = item == selectedItem,
                        onSelect = { onItemSelected(item) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                // 版本信息
                Text(
                    text = "v1.0.0",
                    color = TextDisabled,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * 增强版导航项
 */
@Composable
private fun EnhancedNavItem(
    item: NavigationItem,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) GradientStart.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(200),
        label = "bg"
    )

    Surface(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .focusable(),
        color = backgroundColor,
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) {
            BorderStroke(1.dp, GradientStart.copy(alpha = 0.5f))
        } else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected) GradientStart else TextSecondary,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = item.title,
                color = if (isSelected) Color.White else TextSecondary,
                fontSize = 17.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// 向后兼容旧版
@Deprecated("Use GlassNavigationRail instead", ReplaceWith("GlassNavigationRail"))
@Composable
fun TvNavigationRail(
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassNavigationRail(
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        modifier = modifier
    )
}
