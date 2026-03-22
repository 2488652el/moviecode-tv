package com.moviecode.tv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.domain.model.MediaType
import com.moviecode.tv.ui.theme.*

/**
 * 带进度标签的海报卡片组件
 */
@Composable
fun ProgressPosterCard(
    item: MediaItem,
    isSelected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    progress: Int = 0,
    modifier: Modifier = Modifier
) {
    // 焦点缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        ),
        label = "scale"
    )

    // 阴影高度动画
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 8.dp,
        animationSpec = tween(200),
        label = "elevation"
    )

    // 获取进度颜色
    val progressColor = when {
        progress >= 95 -> Color(0xFF22C55E) // 绿色 - 已看完
        progress >= 50 -> GradientStart // 蓝色
        progress >= 25 -> Color(0xFFEAB308) // 黄色
        progress > 0 -> Color(0xFF6B7280) // 灰色
        else -> Color.Transparent
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(180.dp)
            .focusable()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                GradientStart.copy(alpha = 0.8f),
                                GradientEnd.copy(alpha = 0.8f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.colors(
            containerColor = if (isSelected) BackgroundCardHover else BackgroundCard
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box {
            // 海报图片
            AsyncImage(
                model = item.posterPath,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            // 底部渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                BackgroundDeep.copy(alpha = 0.9f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // 焦点指示器（底部渐变线）
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    GradientStart,
                                    GradientEnd,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // 进度条 - 底部
            if (progress > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress / 100f)
                            .background(progressColor)
                    )
                }
            }

            // 评分徽章
            if (item.rating > 0) {
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd),
                    shape = RoundedCornerShape(8.dp),
                    color = BackgroundDeep.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = RatingGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", item.rating),
                            color = RatingGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 媒体类型标签
            Surface(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(6.dp),
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
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // 已看完标签
            if (progress >= 95) {
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                        .offset(x = 52.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF22C55E).copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "已看完",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 观看进度标签
            if (progress in 1..94) {
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                        .offset(x = if (progress >= 95) 52.dp else 0.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = BackgroundDeep.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = "看到 ${progress}%",
                        color = progressColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 标题
            Text(
                text = item.title,
                color = if (isSelected) Color.White else TextPrimary,
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

/**
 * 带进度的横向媒体列表组件
 */
@Composable
fun ProgressMediaRow(
    title: String,
    items: List<MediaItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onItemClicked: (MediaItem) -> Unit,
    getProgress: @Composable (MediaItem) -> Int = { 0 },
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 20.dp)) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = { },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = TextSecondary
                )
            ) {
                Text("查看全部")
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 横向滚动列表
        androidx.tv.foundation.lazy.list.TvLazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(items) { index, item ->
                ProgressPosterCard(
                    item = item,
                    isSelected = index == selectedIndex,
                    onFocus = { onItemSelected(index) },
                    onClick = { onItemClicked(item) },
                    progress = getProgress(item)
                )
            }
        }
    }
}
