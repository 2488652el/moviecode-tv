package com.moviecode.tv.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviecode.tv.data.repository.ParentalControlRepository
import com.moviecode.tv.domain.model.ContentRating
import com.moviecode.tv.domain.model.MediaType
import com.moviecode.tv.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 家长控制设置屏幕
 */
@Composable
fun ParentalControlScreen(
    repository: ParentalControlRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val settings by repository.getSettingsFlow().collectAsState(initial = null)
    val isUnlocked by repository.isUnlocked().collectAsState(initial = false)
    
    var showPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isChangingPin by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var oldPinVerified by remember { mutableStateOf(false) }
    
    // 加载设置
    LaunchedEffect(Unit) {
        if (settings == null) {
            repository.getSettings()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        if (settings == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = GradientStart
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部导航栏
                TopBar(
                    title = "家长控制",
                    onBack = onNavigateBack
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 启用开关
                    item {
                        SettingsCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "启用家长控制",
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "开启后将根据设置过滤和限制内容",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                                Switch(
                                    checked = settings!!.isEnabled,
                                    onCheckedChange = { enabled ->
                                        scope.launch { repository.setEnabled(enabled) }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GradientStart
                                    )
                                )
                            }
                        }
                    }
                    
                    // PIN 保护
                    item {
                        SettingsCard(
                            onClick = {
                                if (isUnlocked) {
                                    showChangePinDialog = true
                                } else {
                                    showPinDialog = true
                                    isChangingPin = false
                                    pinInput = ""
                                    pinError = null
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "PIN 保护",
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isUnlocked) "已解锁，点击修改" else "输入 PIN 解锁设置",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isUnlocked) GradientStart else TextSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    
                    // 年龄分级
                    item {
                        SettingsCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "年龄分级限制",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "允许观看的最高内容分级",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ContentRating.entries.forEach { rating ->
                                        RatingOption(
                                            rating = rating,
                                            isSelected = settings!!.contentRating == rating,
                                            onSelect = {
                                                scope.launch { repository.setContentRating(rating) }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // 媒体类型
                    item {
                        SettingsCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "允许的媒体类型",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "选择允许观看的媒体类型",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MediaType.entries.forEach { type ->
                                        val isSelected = settings!!.allowedMediaTypes.contains(type)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                val newTypes = if (isSelected) {
                                                    settings!!.allowedMediaTypes - type
                                                } else {
                                                    settings!!.allowedMediaTypes + type
                                                }
                                                scope.launch { repository.setAllowedMediaTypes(newTypes) }
                                            },
                                            label = {
                                                Text(
                                                    text = when (type) {
                                                        MediaType.MOVIE -> "电影"
                                                        MediaType.TV_SHOW -> "剧集"
                                                        MediaType.ANIME -> "动漫"
                                                    }
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GradientStart,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // 每日时长限制
                    item {
                        SettingsCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "每日观看时长",
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "0表示不限制",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                                OutlinedTextField(
                                    value = settings!!.dailyWatchLimit.toString(),
                                    onValueChange = { value ->
                                        val minutes = value.filter { it.isDigit() }.toIntOrNull() ?: 0
                                        scope.launch { repository.setDailyWatchLimit(minutes) }
                                    },
                                    modifier = Modifier.width(100.dp),
                                    textStyle = LocalTextStyle.current.copy(
                                        textAlign = TextAlign.Center,
                                        color = TextPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    suffix = { Text("分钟", color = TextSecondary) }
                                )
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
        
        // PIN 输入对话框
        if (showPinDialog) {
            PinDialog(
                title = if (isChangingPin) "输入旧 PIN" else if (oldPinVerified) "输入新 PIN" else "输入 PIN 解锁",
                pinInput = pinInput,
                newPin = newPin,
                confirmPin = confirmPin,
                oldPinVerified = oldPinVerified,
                isChangingPin = isChangingPin,
                error = pinError,
                onPinChange = { pinInput = it },
                onNewPinChange = { newPin = it },
                onConfirmPinChange = { confirmPin = it },
                onPinSubmit = { pin ->
                    scope.launch {
                        if (isChangingPin && !oldPinVerified) {
                            if (repository.verifyPin(pin)) {
oldPinVerified = true
                                pinInput = ""
                            } else {
                                pinError = "PIN 错误"
                                pinInput = ""
                            }
                        } else if (oldPinVerified) {
                            // 验证新 PIN 格式
                            if (!pin.matches(Regex("^\\d{4}$"))) {
                                pinError = "请输入4位数字"
                                return@launch
                            }
                            if (pin != confirmPin) {
                                pinError = "两次输入不一致"
                                return@launch
                            }
                            if (repository.setPin(pin)) {
                                showChangePinDialog = false
                                showPinDialog = false
                                pinInput = ""
                                newPin = ""
                                confirmPin = ""
                                oldPinVerified = false
                                isChangingPin = false
                            }
                        } else {
                            if (repository.verifyAndUnlock(pin)) {
                                showPinDialog = false
                                pinInput = ""
                            } else {
                                pinError = "PIN 错误"
                                pinInput = ""
                            }
                        }
                    }
                },
                onDismiss = {
                    showPinDialog = false
                    showChangePinDialog = false
                    pinInput = ""
                    newPin = ""
                    confirmPin = ""
                    oldPinVerified = false
                    isChangingPin = false
                    pinError = null
                }
            )
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = TextPrimary
            )
        }
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (onClick != null) {
            Surface(
                onClick = onClick,
                modifier = Modifier.focusable(),
                color = Color.Transparent
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
private fun RatingOption(
    rating: ContentRating,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .focusable(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) GradientStart.copy(alpha = 0.2f) else BackgroundCard,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, GradientStart)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = rating.label,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = rating.description,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = GradientStart
                )
            }
        }
    }
}

@Composable
private fun PinDialog(
    title: String,
    pinInput: String,
    newPin: String,
    confirmPin: String,
    oldPinVerified: Boolean,
    isChangingPin: Boolean,
    error: String?,
    onPinChange: (String) -> Unit,
    onNewPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onPinSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(360.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // PIN 显示
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (pinInput.length > index) GradientStart
                                    else BackgroundCardHover
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (pinInput.length > index) {
                                Icon(
                                    imageVector = Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                
                if (error != null) {
                    Text(
                        text = error,
                        color = Color(0xFFFF6B6B),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 数字键盘
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "⌫")
                    ).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { key ->
                                KeypadButton(
                                    text = key,
                                    onClick = {
                                        when (key) {
                                            "⌫" -> onPinChange(pinInput.dropLast(1))
                                            "" -> { }
                                            else -> {
                                                if (pinInput.length < 4) {
                                                    val newPinInput = pinInput + key
                                                    onPinChange(newPinInput)
                                                    if (newPinInput.length == 4) {
                                                        onPinSubmit(newPinInput)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss) {
                    Text("取消", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .focusable(),
        shape = RoundedCornerShape(16.dp),
        color = BackgroundCardHover
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (text == "⌫") {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else if (text.isNotEmpty()) {
                Text(
                    text = text,
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
