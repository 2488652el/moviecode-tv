package com.moviecode.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moviecode.tv.BuildConfig
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.domain.model.MediaType
import com.moviecode.tv.ui.theme.*

/**
 * 判断是否为TV设备
 */
val isTvDevice: Boolean
    get() = BuildConfig.IS_TV_DEVICE

/**
 * 手机版底部导航栏
 */
@Composable
fun PhoneBottomNavigation(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf("首页", "搜索", "设置")
    val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.Settings)

    NavigationBar(
        modifier = modifier,
        containerColor = BackgroundDeep
    ) {
        items.forEachIndexed { index, title ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = title) },
                label = { Text(title) },
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GradientStart,
                    selectedTextColor = GradientStart,
                    indicatorColor = GradientStart.copy(alpha = 0.2f)
                )
            )
        }
    }
}

/**
 * 手机版海报网格组件
 */
@Composable
fun PhonePosterGrid(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        items(items) { item ->
            PhonePosterCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

/**
 * 手机版海报卡片
 */
@Composable
fun PhonePosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.67f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard)
    ) {
        Box {
            // 海报图片
            AsyncImage(
                model = item.posterPath,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 类型标签
            Surface(
                modifier = Modifier
                    .padding(6.dp)
                    .align(Alignment.TopEnd),
                shape = RoundedCornerShape(4.dp),
                color = when (item.type) {
                    MediaType.MOVIE -> GradientStart.copy(alpha = 0.9f)
                    MediaType.TV_SHOW -> GradientEnd.copy(alpha = 0.9f)
                    MediaType.ANIME -> GradientAccent.copy(alpha = 0.9f)
                }
            ) {
                Text(
                    text = when (item.type) {
                        MediaType.MOVIE -> "电影"
                        MediaType.TV_SHOW -> "剧集"
                        MediaType.ANIME -> "动漫"
                    },
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // 底部渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, BackgroundDeep.copy(alpha = 0.9f))
                        )
                    )
            )

            // 标题
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

/**
 * 手机版搜索栏
 */
@Composable
fun PhoneSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索电影、电视剧..."
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text(placeholder, color = TextSecondary) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "搜索", tint = TextSecondary)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange(""); onSearch() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "清除", tint = TextSecondary)
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GradientStart,
            unfocusedBorderColor = BackgroundCard,
            focusedContainerColor = BackgroundCard,
            unfocusedContainerColor = BackgroundCard
        ),
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * 手机版详情页返回按钮
 */
@Composable
fun PhoneTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BackgroundDeep,
            titleContentColor = Color.White
        ),
        modifier = modifier
    )
}

/**
 * 手机版视频播放器控件
 */
@Composable
fun PhonePlayerControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        // 播放/暂停按钮
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = if (isPlaying) {
                    androidx.compose.material.icons.Icons.Default.Pause
                } else {
                    androidx.compose.material.icons.Icons.Default.PlayArrow
                },
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 进度条
        Slider(
            value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
            onValueChange = { onSeek((it * duration).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = GradientStart,
                activeTrackColor = GradientStart
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
                fontSize = 12.sp
            )
            Text(
                text = formatDuration(duration),
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * 格式化时长
 */
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

/**
 * 懒加载海报网格（适用于手机）
 */
@Composable
fun LazyPosterGrid(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    onLoadMore: (() -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        items(items) { item ->
            PhonePosterCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}
