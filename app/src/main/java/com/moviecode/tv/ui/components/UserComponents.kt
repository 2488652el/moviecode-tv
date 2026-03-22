package com.moviecode.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.domain.model.MediaType
import com.moviecode.tv.ui.theme.*

/**
 * 用户头像组件
 */
@Composable
fun UserAvatar(
    name: String,
    avatarUrl: String? = null,
    size: Int = 48,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "U",
                color = Color.White,
                fontSize = (size / 2).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 用户选择器
 */
@Composable
fun UserSelector(
    users: List<String>, // 简化：只用名字
    currentUser: String,
    onUserSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        users.forEach { user ->
            val isSelected = user == currentUser
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .focusable()
                    .then(
                        if (isSelected) {
                            Modifier
                                .background(
                                    GradientStart.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(8.dp)
                        } else Modifier
                    )
            ) {
                UserAvatar(
                    name = user,
                    size = if (isSelected) 64 else 48
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user,
                    color = if (isSelected) GradientStart else TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 收藏按钮
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "取消收藏" else "添加收藏",
            tint = if (isFavorite) Color.Red else TextSecondary
        )
    }
}

/**
 * 收藏列表组件
 */
@Composable
fun FavoritesSection(
    favorites: List<Pair<MediaItem, String?>>, // MediaItem + note
    onItemClick: (MediaItem) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.Red
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "我的收藏",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${favorites.size} 部",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无收藏",
                    color = TextSecondary,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(favorites.size) { index ->
                    val (item, note) = favorites[index]
                    FavoriteCard(
                        item = item,
                        note = note,
                        onClick = { onItemClick(item) },
                        onRemove = { onRemoveFavorite(item.id.toString()) }
                    )
                }
            }
        }
    }
}

/**
 * 收藏卡片
 */
@Composable
fun FavoriteCard(
    item: MediaItem,
    note: String?,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .focusable(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard)
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = item.posterPath,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    
                    // 移除按钮
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "移除",
                            tint = Color.White
                        )
                    }
                }
                
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (note != null) {
                        Text(
                            text = note,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * 播放列表组件
 */
@Composable
fun PlaylistsSection(
    playlists: List<Pair<String, Int>>, // name, itemCount
    onPlaylistClick: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlaylistPlay,
                contentDescription = null,
                tint = GradientStart
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "播放列表",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(onClick = onCreatePlaylist) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "创建",
                    tint = GradientStart
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("新建", color = GradientStart)
            }
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无播放列表",
                    color = TextSecondary,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlists.size) { index ->
                    val (name, count) = playlists[index]
                    PlaylistCard(
                        name = name,
                        itemCount = count,
                        onClick = { onPlaylistClick(name) }
                    )
                }
            }
        }
    }
}

/**
 * 播放列表卡片
 */
@Composable
fun PlaylistCard(
    name: String,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(180.dp)
            .focusable(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(GradientStart.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistPlay,
                    contentDescription = null,
                    tint = GradientStart
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$itemCount 部",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 观看统计组件
 */
@Composable
fun WatchStatsCard(
    totalWatchTime: Long, // 秒
    totalItems: Int,
    completedItems: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(280.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard.copy(alpha = 0.8f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = GradientStart
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "观看统计",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 总观看时长
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "总观看时长",
                    value = formatWatchTime(totalWatchTime)
                )
                StatItem(
                    label = "观看项目",
                    value = "$totalItems"
                )
                StatItem(
                    label = "看完",
                    value = "$completedItems"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = GradientStart,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

/**
 * 格式化观看时长
 */
private fun formatWatchTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "0m"
    }
}
