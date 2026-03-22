package com.moviecode.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviecode.tv.data.repository.ThemeMode
import com.moviecode.tv.data.repository.ThemeRepository
import com.moviecode.tv.ui.theme.*

/**
 * 主题设置屏幕
 */
@Composable
fun ThemeSettingsScreen(
    themeRepository: ThemeRepository,
    onBackClick: () -> Unit
) {
    var themeSettings by remember { mutableStateOf<com.moviecode.tv.data.repository.ThemeSettings?>(null) }
    var selectedMode by remember { mutableStateOf(ThemeMode.DARK) }
    var autoDarkStart by remember { mutableStateOf("22:00") }
    var autoDarkEnd by remember { mutableStateOf("06:00") }
    
    // 加载主题设置
    LaunchedEffect(Unit) {
        themeRepository.getThemeSettingsFlow().collect { settings ->
            themeSettings = settings
            selectedMode = settings.mode
            autoDarkStart = settings.autoDarkStart
            autoDarkEnd = settings.autoDarkEnd
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        // 左侧面板
        Column(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            // 返回按钮
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary
                )
            }
            
            Text(
                text = "主题设置",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "自定义应用外观",
                color = TextSecondary,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 主题图标
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        
        // 右侧内容
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            // 主题模式选择
            Text(
                text = "主题模式",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ThemeModeItem(
                    icon = Icons.Default.DarkMode,
                    label = "深色",
                    isSelected = selectedMode == ThemeMode.DARK,
                    onClick = {
                        selectedMode = ThemeMode.DARK
                        kotlinx.coroutines.MainScope().launch {
                            themeRepository.setThemeMode(ThemeMode.DARK)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                
                ThemeModeItem(
                    icon = Icons.Default.LightMode,
                    label = "浅色",
                    isSelected = selectedMode == ThemeMode.LIGHT,
                    onClick = {
                        selectedMode = ThemeMode.LIGHT
                        kotlinx.coroutines.MainScope().launch {
                            themeRepository.setThemeMode(ThemeMode.LIGHT)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                
                ThemeModeItem(
                    icon = Icons.Default.SettingsBrightness,
                    label = "跟随系统",
                    isSelected = selectedMode == ThemeMode.SYSTEM,
                    onClick = {
                        selectedMode = ThemeMode.SYSTEM
                        kotlinx.coroutines.MainScope().launch {
                            themeRepository.setThemeMode(ThemeMode.SYSTEM)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 自动暗黑模式时间设置
            if (selectedMode == ThemeMode.SYSTEM) {
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "自动暗黑时间",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "在指定时间段内自动切换到深色模式",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 开始时间
                    TimePickerCard(
                        label = "开始时间",
                        time = autoDarkStart,
                        onTimeChange = { newTime ->
                            autoDarkStart = newTime
                            kotlinx.coroutines.MainScope().launch {
                                themeRepository.setAutoDarkTime(newTime, autoDarkEnd)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = "—",
                        color = TextTertiary,
                        fontSize = 24.sp
                    )
                    
                    // 结束时间
                    TimePickerCard(
                        label = "结束时间",
                        time = autoDarkEnd,
                        onTimeChange = { newTime ->
                            autoDarkEnd = newTime
                            kotlinx.coroutines.MainScope().launch {
                                themeRepository.setAutoDarkTime(autoDarkStart, newTime)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "示例: 22:00 - 06:00 表示晚上10点到次日早上6点使用深色模式",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 预览
            Text(
                text = "预览",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            val isDark = selectedMode != ThemeMode.LIGHT
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) BackgroundDeep else Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (isDark) GradientStart else Color(0xFF3B82F6),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "主题预览",
                            color = if (isDark) Color.White else Color(0xFF1F2937),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "当前: ${when (selectedMode) {
                                ThemeMode.DARK -> "深色模式"
                                ThemeMode.LIGHT -> "浅色模式"
                                ThemeMode.SYSTEM -> "跟随系统"
                            }}",
                            color = if (isDark) TextSecondary else Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(100.dp)
            .focusable(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GradientStart.copy(alpha = 0.15f) else BackgroundCard
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, GradientStart)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) GradientStart else TextSecondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TimePickerCard(
    label: String,
    time: String,
    onTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = time,
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 时间调节按钮
                Column {
                    IconButton(
                        onClick = {
                            val parts = time.split(":")
                            var hour = parts[0].toIntOrNull() ?: 22
                            hour = (hour + 1) % 24
                            onTimeChange(String.format("%02d:00", hour))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "增加",
                            tint = TextSecondary
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            val parts = time.split(":")
                            var hour = parts[0].toIntOrNull() ?: 22
                            hour = if (hour == 0) 23 else hour - 1
                            onTimeChange(String.format("%02d:00", hour))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "减少",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun kotlinx.coroutines.MainScope() = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
