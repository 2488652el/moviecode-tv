package com.moviecode.tv.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.moviecode.tv.data.local.PlaybackProgress
import com.moviecode.tv.data.repository.PlaybackRepository
import com.moviecode.tv.ui.theme.*
import com.moviecode.tv.util.SubtitleFile
import com.moviecode.tv.util.SubtitleFinder
import kotlinx.coroutines.delay

// 倍速播放档位
val PLAYBACK_SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoPath: String,
    title: String,
    mediaId: String = videoPath,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val playbackRepository = remember { PlaybackRepository(context) }
    
    // 播放器状态
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedToast by remember { mutableStateOf(false) }
    
    // 字幕状态
    var subtitles by remember { mutableStateOf<List<SubtitleFile>>(emptyList()) }
    var currentSubtitle by remember { mutableStateOf<SubtitleFile?>(null) }
    var showSubtitleMenu by remember { mutableStateOf(false) }
    
    // 进度记忆状态
    var savedProgress by remember { mutableStateOf<PlaybackProgress?>(null) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var hasInitialized by remember { mutableStateOf(false) }
    
    // ExoPlayer 实例
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoPath)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }
    
    // 初始化：搜索字幕和加载进度
    LaunchedEffect(videoPath) {
        if (!hasInitialized) {
            hasInitialized = true
            
            // 搜索字幕文件
            subtitles = SubtitleFinder.searchSubtitles(videoPath)
            
            // 加载播放进度
            savedProgress = playbackRepository.shouldShowResumeDialog(mediaId, videoPath)
            if (savedProgress != null) {
                showResumeDialog = true
            }
        }
    }
    
    // 自动隐藏控制栏
    LaunchedEffect(showControls, isPlaying) {
        if (isPlaying && showControls) {
            delay(3000)
            showControls = false
        }
    }
    
    // 更新播放位置
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            duration = exoPlayer.duration.coerceAtLeast(1L)
            delay(1000)
        }
    }
    
    // 自动保存进度（每30秒）
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(30000)
            if (duration > 0 && currentPosition > 0) {
                playbackRepository.saveProgress(
                    mediaId = mediaId,
                    filePath = videoPath,
                    currentTime = currentPosition.toDouble(),
                    duration = duration.toDouble()
                )
            }
        }
    }
    
    // 速度提示自动消失
    LaunchedEffect(showSpeedToast) {
        if (showSpeedToast) {
            delay(2000)
            showSpeedToast = false
        }
    }
    
    // Player listener
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                    // 保存当前进度
                    if (duration > 0) {
                        playbackRepository.saveProgress(
                            mediaId = mediaId,
                            filePath = videoPath,
                            currentTime = exoPlayer.currentPosition.toDouble(),
                            duration = duration.toDouble()
                        )
                    }
                }
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }
    
    BackHandler {
        // 退出前保存进度
        if (duration > 0) {
            playbackRepository.saveProgress(
                mediaId = mediaId,
                filePath = videoPath,
                currentTime = exoPlayer.currentPosition.toDouble(),
                duration = duration.toDouble()
            )
        }
        exoPlayer.stop()
        exoPlayer.release()
        onBack()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
    ) {
        // 视频播放器
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 续播提示对话框
        if (showResumeDialog && savedProgress != null) {
            AlertDialog(
                onDismissRequest = { showResumeDialog = false },
                title = {
                    Text(
                        "继续播放",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("您上次看到 ${savedProgress!!.progressPercent}%，是否从上次位置继续？")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            exoPlayer.seekTo(savedProgress!!.currentTime.toLong())
                            exoPlayer.play()
                            isPlaying = true
                            showResumeDialog = false
                        }
                    ) {
                        Text("继续播放")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            exoPlayer.seekTo(0)
                            exoPlayer.play()
                            isPlaying = true
                            showResumeDialog = false
                        }
                    ) {
                        Text("从头开始")
                    }
                }
            )
        }
        
        // 字幕菜单
        if (showSubtitleMenu) {
            SubtitleMenu(
                subtitles = subtitles,
                currentSubtitle = currentSubtitle,
                onSelectSubtitle = { subtitle ->
                    currentSubtitle = subtitle
                    showSubtitleMenu = false
                },
                onDismiss = { showSubtitleMenu = false }
            )
        }
        
        // 速度提示
        AnimatedVisibility(
            visible = showSpeedToast,
            enter = fadeIn() + scaleEnter(),
            exit = fadeOut() + scaleExit(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (currentSpeed == 1.0f) "正常速度" else "${currentSpeed}x",
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
        
        // 控制栏
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // 顶部栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (duration > 0) {
                            playbackRepository.saveProgress(
                                mediaId = mediaId,
                                filePath = videoPath,
                                currentTime = exoPlayer.currentPosition.toDouble(),
                                duration = duration.toDouble()
                            )
                        }
                        onBack()
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 24.sp,
                        maxLines = 1
                    )
                }
                
                // 中心控制
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 快退
                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0)) }
                    ) {
                        Icon(
                            Icons.Default.FastRewind,
                            contentDescription = "快退10秒",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    // 播放/暂停
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                                isPlaying = false
                            } else {
                                exoPlayer.play()
                                isPlaying = true
                            }
                        }
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                    
                    // 快进
                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(duration)) }
                    ) {
                        Icon(
                            Icons.Default.FastForward,
                            contentDescription = "快进10秒",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                // 底部控制栏
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // 进度条
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                        onValueChange = { value ->
                            exoPlayer.seekTo((value * duration).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Primary,
                            activeTrackColor = Primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    
                    // 时间显示
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(currentPosition),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        
                        Text(
                            text = formatDuration(duration),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 额外控制按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 倍速按钮
                        Box {
                            AssistChip(
                                onClick = {
                                    // 切换到下一个速度
                                    val currentIndex = PLAYBACK_SPEEDS.indexOf(currentSpeed)
                                    val nextIndex = (currentIndex + 1) % PLAYBACK_SPEEDS.size
                                    val newSpeed = PLAYBACK_SPEEDS[nextIndex]
                                    exoPlayer.setPlaybackSpeed(newSpeed)
                                    currentSpeed = newSpeed
                                    showSpeedToast = true
                                },
                                label = {
                                    Text(
                                        if (currentSpeed == 1.0f) "倍速" else "${currentSpeed}x",
                                        color = Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Speed,
                                        contentDescription = "倍速",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // 字幕按钮
                        if (subtitles.isNotEmpty()) {
                            Box {
                                AssistChip(
                                    onClick = { showSubtitleMenu = true },
                                    label = {
                                        Text(
                                            if (currentSubtitle != null) "字幕" else "字幕 ▼",
                                            color = Color.White
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Subtitles,
                                            contentDescription = "字幕",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (currentSubtitle != null) 
                                            Primary.copy(alpha = 0.5f) 
                                        else 
                                            Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 点击切换控制栏显示
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusable()
        )
    }
}

@Composable
private fun SubtitleMenu(
    subtitles: List<SubtitleFile>,
    currentSubtitle: SubtitleFile?,
    onSelectSubtitle: (SubtitleFile?) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 80.dp, bottom = 150.dp),
        color = Color.Black.copy(alpha = 0.9f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "字幕",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 关闭字幕选项
            TextButton(
                onClick = { onSelectSubtitle(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (currentSubtitle == null) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        "关闭字幕",
                        color = if (currentSubtitle == null) Primary else Color.White
                    )
                }
            }
            
            // 字幕列表
            subtitles.forEach { subtitle ->
                TextButton(
                    onClick = { onSelectSubtitle(subtitle) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        if (currentSubtitle?.path == subtitle.path) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else if (currentSubtitle == null) {
                            Spacer(modifier = Modifier.width(28.dp))
                        }
                        Text(
                            subtitle.name,
                            color = if (currentSubtitle?.path == subtitle.path) Primary else Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
