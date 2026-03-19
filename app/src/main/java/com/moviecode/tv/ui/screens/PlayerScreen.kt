package com.moviecode.tv.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.moviecode.tv.ui.theme.*

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoPath: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoPath)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }
    
    // Update position periodically
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(1L)
            kotlinx.coroutines.delay(1000)
        }
    }
    
    // Lifecycle handling
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
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
        exoPlayer.stop()
        onBack()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
    ) {
        // Video player
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
        
        // Controls overlay
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
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 24.sp
                    )
                }
                
                // Center controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { exoPlayer.seekTo(exoPlayer.currentPosition - 10000) }
                    ) {
                        Icon(
                            Icons.Default.FastRewind,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
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
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10000) }
                    ) {
                        Icon(
                            Icons.Default.FastForward,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                // Bottom progress bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
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
                }
            }
        }
        
        // Toggle controls on click
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusable()
        )
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
