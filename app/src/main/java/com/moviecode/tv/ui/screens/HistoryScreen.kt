package com.moviecode.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviecode.tv.data.repository.HistoryRepository
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.domain.model.MediaType
import com.moviecode.tv.ui.components.ProgressPosterCard
import com.moviecode.tv.ui.theme.*

/**
 * 历史记录屏幕
 */
@Composable
fun HistoryScreen(
    historyRepository: HistoryRepository,
    onItemClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var historyList by remember { mutableStateOf<List<com.moviecode.tv.data.repository.PlayHistoryEntry>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("all") }
    var selectedSort by remember { mutableStateOf("recent") }
    var selectedIndex by remember { mutableIntStateOf(0) }
    
    // 加载历史记录
    LaunchedEffect(Unit) {
        historyRepository.getHistoryFlow().collect { history ->
            historyList = when {
                selectedSort == "progress" -> history.sortedByDescending { it.progress }
                else -> history
            }.let { sorted ->
                when (selectedFilter) {
                    "all" -> sorted
                    else -> sorted.filter { it.type == selectedFilter }
                }
            }
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        // 左侧导航
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .padding(16.dp)
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
                text = "播放历史",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "共 ${historyList.size} 部",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // 筛选按钮
            Text(
                text = "筛选",
                color = TextTertiary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            listOf(
                "all" to "全部",
                "movie" to "电影",
                "tv" to "剧集",
                "anime" to "动漫"
            ).forEach { (value, label) ->
                FilterChipItem(
                    label = label,
                    isSelected = selectedFilter == value,
                    onClick = { selectedFilter = value }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 排序按钮
            Text(
                text = "排序",
                color = TextTertiary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            SortChipItem(
                label = "最近观看",
                isSelected = selectedSort == "recent",
                onClick = { selectedSort = "recent" }
            )
            
            SortChipItem(
                label = "播放进度",
                isSelected = selectedSort == "progress",
                onClick = { selectedSort = "progress" }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 清除历史按钮
            if (historyList.isNotEmpty()) {
                Button(
                    onClick = { 
                        kotlinx.coroutines.MainScope().launch {
                            historyRepository.clearHistory()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed.copy(alpha = 0.2f),
                        contentColor = ErrorRed
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("清除全部")
                }
            }
        }
        
        // 右侧内容
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (historyList.isEmpty()) {
                // 空状态
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无播放历史",
                        color = TextSecondary,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "开始观看一些影片吧",
                        color = TextTertiary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                // 历史网格
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    contentPadding = PaddingValues(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(historyList) { entry ->
                        val isSelected = historyList.indexOf(entry) == selectedIndex
                        HistoryCard(
                            entry = entry,
                            isSelected = isSelected,
                            onFocus = { selectedIndex = historyList.indexOf(entry) },
                            onClick = { onItemClick(entry.mediaId) },
                            onDelete = {
                                kotlinx.coroutines.MainScope().launch {
                                    historyRepository.deleteEntry(entry.mediaId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .focusable(),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) GradientStart.copy(alpha = 0.2f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) GradientStart else TextTertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SortChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .focusable(),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) BackgroundCard else BackgroundCard.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (label) {
                    "最近观看" -> Icons.Default.Schedule
                    else -> Icons.Default.TrendingUp
                },
                contentDescription = null,
                tint = if (isSelected) GradientStart else TextTertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun HistoryCard(
    entry: com.moviecode.tv.data.repository.PlayHistoryEntry,
    isSelected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val progressColor = when {
        entry.progress >= 95 -> Color(0xFF22C55E)
        entry.progress >= 50 -> GradientStart
        entry.progress >= 25 -> Color(0xFFEAB308)
        entry.progress > 0 -> Color(0xFF6B7280)
        else -> Color.Transparent
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .focusable()
            .then(
                if (isSelected) {
                    Modifier
                } else Modifier
            ),
        colors = CardDefaults.colors(
            containerColor = if (isSelected) BackgroundCardHover else BackgroundCard
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 16.dp else 8.dp
        )
    ) {
        Box {
            // 海报图片
            coil.compose.AsyncImage(
                model = entry.posterPath,
                contentDescription = entry.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            
            // 删除按钮
            if (isSelected) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        tint = ErrorRed,
                        modifier = Modifier
                            .background(
                                BackgroundDeep.copy(alpha = 0.8f),
                                RoundedCornerShape(50)
                            )
                            .padding(4.dp)
                            .size(20.dp)
                    )
                }
            }
            
            // 进度条
            if (entry.progress > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(entry.progress / 100f)
                            .background(progressColor)
                    )
                }
            }
            
            // 类型标签
            Surface(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(6.dp),
                color = when (entry.type) {
                    "movie" -> GradientStart
                    "tv" -> GradientEnd
                    else -> GradientAccent
                }
            ) {
                Text(
                    text = when (entry.type) {
                        "movie" -> "电影"
                        "tv" -> "剧集"
                        else -> "动漫"
                    },
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            // 已看完标签
            if (entry.progress >= 95) {
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                        .offset(x = 48.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF22C55E)
                ) {
                    Text(
                        text = "已看完",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            // 进度标签
            if (entry.progress in 1..94) {
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(6.dp),
                    color = BackgroundDeep.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = "看到 ${entry.progress}%",
                        color = progressColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            // 标题
            Text(
                text = entry.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}

private fun kotlinx.coroutines.MainScope() = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
