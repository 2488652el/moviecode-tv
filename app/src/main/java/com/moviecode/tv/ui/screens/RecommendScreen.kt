package com.moviecode.tv.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moviecode.tv.data.repository.HistoryRepository
import com.moviecode.tv.data.repository.RecommendationItem
import com.moviecode.tv.data.repository.RecommendationRepository
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.domain.model.MediaType
import com.moviecode.tv.ui.components.GlassNavigationRail
import com.moviecode.tv.ui.components.NavigationItem
import com.moviecode.tv.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 推荐类型枚举
 */
enum class RecommendTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FOR_YOU("为你推荐", Icons.Default.Favorite),
    SIMILAR("相似内容", Icons.Default.SmartScreen),
    NEW_RELEASES("最新上线", Icons.Default.NewReleases)
}

/**
 * 推荐屏幕
 */
@Composable
fun RecommendScreen(
    mediaItems: List<MediaItem>,
    historyRepository: HistoryRepository,
    recommendationRepository: RecommendationRepository,
    selectedNavItem: NavigationItem,
    onNavItemSelected: (NavigationItem) -> Unit,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(RecommendTab.FOR_YOU) }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    var recommendations by remember { mutableStateOf<List<RecommendationItem>>(emptyList()) }
    var recentWatched by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    // 加载数据
    LaunchedEffect(mediaItems) {
        scope.launch {
            recentWatched = withContext(Dispatchers.IO) {
                historyRepository.getRecentHistory(5).mapNotNull { entry ->
                    mediaItems.find { it.id == entry.mediaId }
                }
            }
            
            recommendations = loadRecommendations(selectedTab, mediaItems, selectedItem, recommendationRepository)
            
            if (selectedTab == RecommendTab.SIMILAR && selectedItem == null && recentWatched.isNotEmpty()) {
                selectedItem = recentWatched.first()
            }
        }
    }
    
    // Tab 切换时重新加载
    LaunchedEffect(selectedTab, selectedItem) {
        recommendations = loadRecommendations(selectedTab, mediaItems, selectedItem, recommendationRepository)
    }

    Row(modifier = modifier.fillMaxSize().background(BackgroundPrimary)) {
        // 导航栏
        GlassNavigationRail(
            selectedItem = selectedNavItem,
            onItemSelected = onNavItemSelected
        )
        
        // 主内容
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // 标题栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "发现",
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Tab 栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RecommendTab.entries.forEach { tab ->
                    TabButton(
                        tab = tab,
                        isSelected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }
            
            // 内容
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // 相似推荐：选择影片
                if (selectedTab == RecommendTab.SIMILAR) {
                    item {
                        SimilarSection(
                            recentWatched = recentWatched,
                            selectedItem = selectedItem,
                            onItemSelected = { selectedItem = it }
                        )
                    }
                }
                
                // 为你推荐：提示
                if (selectedTab == RecommendTab.FOR_YOU && recentWatched.isNotEmpty()) {
                    item {
                        RecommendationHint()
                    }
                }
                
                // 推荐列表
                item {
                    if (recommendations.isEmpty()) {
                        EmptyState(
                            message = when (selectedTab) {
                                RecommendTab.SIMILAR -> "请选择一部影片"
                                else -> "暂无推荐"
                            }
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(recommendations) { rec ->
                                RecommendationCard(
                                    recommendation = rec,
                                    onClick = { onItemClick(rec.item) }
                                )
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun TabButton(
    tab: RecommendTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.focusable(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) GradientStart else BackgroundCard
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = tab.title,
                color = if (isSelected) Color.White else TextSecondary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SimilarSection(
    recentWatched: List<MediaItem>,
    selectedItem: MediaItem?,
    onItemSelected: (MediaItem) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp)
    ) {
        Text(
            text = "选择一部影片，获取相似推荐",
            color = TextSecondary,
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (recentWatched.isEmpty()) {
            Text(
                text = "暂无观看记录",
                color = TextTertiary,
                fontSize = 14.sp
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recentWatched) { item ->
                    val isSelected = selectedItem?.id == item.id
                    
                    Surface(
                        onClick = { onItemSelected(item) },
                        modifier = Modifier
                            .focusable()
                            .then(
                                if (isSelected) Modifier.padding(4.dp) else Modifier
                            ),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(2.dp, GradientStart)
                        } else null,
                        color = if (isSelected) BackgroundCardHover else BackgroundCard
                    ) {
                        AsyncImage(
                            model = item.posterPath,
                            contentDescription = item.title,
                            modifier = Modifier
                                .size(width = 100.dp, height = 150.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationHint() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "个性化推荐",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "根据您的观看记录生成",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: RecommendationItem,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "scale"
    )
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .focusable()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) BackgroundCardHover else BackgroundCard
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 16.dp else 8.dp
        )
    ) {
        Box {
            // 海报
            AsyncImage(
                model = recommendation.item.posterPath,
                contentDescription = recommendation.item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )
            
            // 推荐理由标签
            Surface(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(6.dp),
                color = GradientStart.copy(alpha = 0.9f)
            ) {
                Text(
                    text = recommendation.reason,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            // 类型标签
            Surface(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd),
                shape = RoundedCornerShape(6.dp),
                color = when (recommendation.item.type) {
                    MediaType.MOVIE -> GradientStart
                    MediaType.TV_SHOW -> GradientEnd
                    MediaType.ANIME -> GradientAccent
                }.copy(alpha = 0.9f)
            ) {
                Text(
                    text = when (recommendation.item.type) {
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
            
            // 评分
            if (recommendation.item.rating > 0) {
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomEnd),
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
                            text = String.format("%.1f", recommendation.item.rating),
                            color = RatingGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            // 标题
            Text(
                text = recommendation.item.title,
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

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Explore,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 16.sp
        )
    }
}


