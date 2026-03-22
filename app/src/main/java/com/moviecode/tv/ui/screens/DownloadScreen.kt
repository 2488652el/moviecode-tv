package com.moviecode.tv.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moviecode.tv.data.repository.DownloadRepository
import com.moviecode.tv.data.repository.DownloadStatus
import com.moviecode.tv.data.repository.DownloadTask
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.ui.components.GlassNavigationRail
import com.moviecode.tv.ui.components.NavigationItem
import com.moviecode.tv.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 下载管理屏幕
 */
@Composable
fun DownloadScreen(
    repository: DownloadRepository,
    selectedNavItem: NavigationItem,
    onNavItemSelected: (NavigationItem) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val tasks by repository.getTasksFlow().collectAsState(initial = emptyList())
    
    val activeTasks = tasks.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
    val pausedTasks = tasks.filter { it.status == DownloadStatus.PAUSED }
    val completedTasks = tasks.filter { it.status == DownloadStatus.COMPLETED }
    val failedTasks = tasks.filter { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }
    
    Row(modifier = modifier.fillMaxSize().background(BackgroundPrimary)) {
        // 导航栏
        GlassNavigationRail(
            selectedItem = selectedNavItem,
            onItemSelected = onNavItemSelected
        )
        
        // 主内容
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "离线下载",
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { scope.launch { repository.clearCompleted() } },
                        enabled = completedTasks.isNotEmpty()
                    ) {
                        Text("清空已完成")
                    }
                    OutlinedButton(
                        onClick = { scope.launch { repository.clearAll() } },
                        enabled = tasks.isNotEmpty(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text("清空全部")
                    }
                }
            }
            
            // 统计栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StatItem(label = "下载中", value = activeTasks.size.toString(), color = GradientStart)
                StatItem(label = "已暂停", value = pausedTasks.size.toString(), color = Color(0xFFF59E0B))
                StatItem(label = "已完成", value = completedTasks.size.toString(), color = Color(0xFF22C55E))
                StatItem(label = "失败", value = failedTasks.size.toString(), color = Color(0xFFEF4444))
            }
            
            // 下载列表
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (tasks.isEmpty()) {
                    item {
                        EmptyDownloadsState()
                    }
                } else {
                    // 活跃任务
                    if (activeTasks.isNotEmpty()) {
                        item {
                            SectionHeader(title = "正在下载")
                        }
                        items(activeTasks, key = { it.id }) { task ->
                            DownloadItemCard(
                                task = task,
                                repository = repository,
                                onClick = { onMediaClick(task.mediaItem) }
                            )
                        }
                    }
                    
                    // 暂停的任务
                    if (pausedTasks.isNotEmpty()) {
                        item {
                            SectionHeader(title = "已暂停")
                        }
                        items(pausedTasks, key = { it.id }) { task ->
                            DownloadItemCard(
                                task = task,
                                repository = repository,
                                onClick = { onMediaClick(task.mediaItem) }
                            )
                        }
                    }
                    
                    // 已完成的任务
                    if (completedTasks.isNotEmpty()) {
                        item {
                            SectionHeader(title = "已完成")
                        }
                        items(completedTasks, key = { it.id }) { task ->
                            DownloadItemCard(
                                task = task,
                                repository = repository,
                                onClick = { onMediaClick(task.mediaItem) }
                            )
                        }
                    }
                    
                    // 失败的任务
                    if (failedTasks.isNotEmpty()) {
                        item {
                            SectionHeader(title = "失败/已取消")
                        }
                        items(failedTasks, key = { it.id }) { task ->
                            DownloadItemCard(
                                task = task,
                                repository = repository,
                                onClick = { onMediaClick(task.mediaItem) }
                            )
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun EmptyDownloadsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无下载任务",
                color = TextSecondary,
                fontSize = 18.sp
            )
            Text(
                text = "在详情页点击下载按钮开始离线下载",
                color = TextTertiary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun DownloadItemCard(
    task: DownloadTask,
    repository: DownloadRepository,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    val statusConfig = when (task.status) {
        DownloadStatus.PENDING -> Triple("等待中", Color(0xFF888888), Icons.Default.Schedule)
        DownloadStatus.DOWNLOADING -> Triple("下载中", GradientStart, Icons.Default.Download)
        DownloadStatus.PAUSED -> Triple("已暂停", Color(0xFFF59E0B), Icons.Default.Pause)
        DownloadStatus.COMPLETED -> Triple("已完成", Color(0xFF22C55E), Icons.Default.CheckCircle)
        DownloadStatus.FAILED -> Triple("失败", Color(0xFFEF4444), Icons.Default.Error)
        DownloadStatus.CANCELLED -> Triple("已取消", Color(0xFF888888), Icons.Default.Cancel)
    }
    
    val (statusLabel, statusColor, statusIcon) = statusConfig
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 海报
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .focusable()
            ) {
                if (task.mediaItem.posterPath != null) {
                    AsyncImage(
                        model = task.mediaItem.posterPath,
                        contentDescription = task.mediaItem.title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(GradientStart, GradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = task.mediaItem.title.take(1),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                // 标题和状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = task.mediaItem.title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // 集数信息
                if (task.episodeNumber != null && task.seasonNumber != null) {
                    Text(
                        text = "第${task.seasonNumber}季 第${task.episodeNumber}集",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // 进度条
                if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.PAUSED) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        LinearProgressIndicator(
                            progress = { (task.progress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = GradientStart,
                            trackColor = BackgroundCardHover,
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${repository.formatFileSize(task.downloadedBytes)} / ${repository.formatFileSize(task.totalBytes)}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${String.format("%.1f", task.progress)}%",
                                color = GradientStart,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // 文件路径
                if (task.status == DownloadStatus.COMPLETED && task.filePath.isNotEmpty()) {
                    Text(
                        text = task.filePath,
                        color = TextTertiary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                // 错误信息
                if (task.error != null) {
                    Text(
                        text = task.error,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 操作按钮
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (task.status) {
                    DownloadStatus.DOWNLOADING -> {
                        ActionIconButton(
                            icon = Icons.Default.Pause,
                            contentDescription = "暂停",
                            onClick = { scope.launch { repository.pauseTask(task.id) } }
                        )
                    }
                    DownloadStatus.PAUSED -> {
                        ActionIconButton(
                            icon = Icons.Default.PlayArrow,
                            contentDescription = "继续",
                            onClick = { scope.launch { repository.resumeTask(task.id) } }
                        )
                    }
                    DownloadStatus.PENDING -> {
                        ActionIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "取消",
                            onClick = { scope.launch { repository.cancelTask(task.id) } }
                        )
}
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        ActionIconButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = "重试",
                            onClick = { scope.launch { repository.retryTask(task.id) } }
                        )
                        ActionIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "删除",
                            onClick = { scope.launch { repository.removeTask(task.id) } }
                        )
                    }
                    DownloadStatus.COMPLETED -> {
                        ActionIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "删除",
                            onClick = { scope.launch { repository.removeTask(task.id) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = BackgroundCardHover
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
