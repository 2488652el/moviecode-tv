package com.moviecode.tv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.graphics.Brush
import androidx.compose.foundation.graphics.Color
import androidx.compose.foundation.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.domain.model.MediaType
import com.moviecode.tv.ui.theme.*

/**
 * 增强版海报卡片组件
 * 特性:
 * - 焦点缩放动画
 * - 渐变边框指示器
 * - 媒体类型标签
 * - 优化评分显示
 */
@Composable
fun EnhancedMediaPosterCard(
    item: MediaItem,
    isSelected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
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

    // 焦点边框透明度
    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "border"
    )

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
                        brush = ComposeBrush.horizontalGradient(
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
                        ComposeBrush.verticalGradient(
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
                            ComposeBrush.horizontalGradient(
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
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
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // 标题
            Text(
                text = item.title,
                color = if (isSelected) Color.White else TextPrimary,
                fontSize = 13.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
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
 * 横向媒体列表组件
 * 特性:
 * - 优化的标题样式
 * - "查看全部"按钮
 * - 平滑的滚动体验
 */
@Composable
fun EnhancedMediaRow(
    title: String,
    items: List<MediaItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onItemClicked: (MediaItem) -> Unit,
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
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            // 查看全部按钮
            TextButton(
                onClick = { },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = TextSecondary
                )
            ) {
                Text("查看全部")
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 横向滚动列表
        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(items) { index, item ->
                EnhancedMediaPosterCard(
                    item = item,
                    isSelected = index == selectedIndex,
                    onFocus = { onItemSelected(index) },
                    onClick = { onItemClicked(item) }
                )
            }
        }
    }
}

// 向后兼容旧版组件
@Deprecated("Use EnhancedMediaPosterCard instead", ReplaceWith("EnhancedMediaPosterCard"))
@Composable
fun MediaPosterCard(
    item: MediaItem,
    isSelected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EnhancedMediaPosterCard(
        item = item,
        isSelected = isSelected,
        onFocus = onFocus,
        onClick = onClick,
        modifier = modifier
    )
}

@Deprecated("Use EnhancedMediaRow instead", ReplaceWith("EnhancedMediaRow"))
@Composable
fun MediaRow(
    title: String,
    items: List<MediaItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onItemClicked: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    EnhancedMediaRow(
        title = title,
        items = items,
        selectedIndex = selectedIndex,
        onItemSelected = onItemSelected,
        onItemClicked = onItemClicked,
        modifier = modifier
    )
}
