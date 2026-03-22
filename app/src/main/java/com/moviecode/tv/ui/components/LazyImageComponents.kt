package com.moviecode.tv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.layout.PositionAlignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.moviecode.tv.data.repository.ImageCacheRepository
import com.moviecode.tv.ui.theme.BackgroundCard
import kotlinx.coroutines.launch

/**
 * 懒加载图片组件
 * 只在进入可见区域时才加载图片
 */
@Composable
fun LazyAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderColor: Color = BackgroundCard
) {
    var isVisible by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 骨架屏动画
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(placeholderColor.copy(alpha = shimmerAlpha))
            .onGloballyPositioned { coordinates ->
                // 检查是否在可见区域
                val size = coordinates.size
                if (size.width > 0 && size.height > 0) {
                    isVisible = true
                }
            }
    ) {
        if (isVisible && imageUrl != null && isLoaded) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onSuccess = { isLoaded = true }
            )
        }
        
        // 加载完成后隐藏骨架
        if (isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }
    }
}

/**
 * 带错误处理的懒加载图片
 */
@Composable
fun LazyMediaPosterImage(
    posterPath: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(modifier = modifier) {
        // 加载占位符
        if (isLoading || hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BackgroundCard.copy(alpha = alpha))
            )
        }
        
        // 图片
        if (posterPath != null && !hasError) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(posterPath)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { isLoading = true },
                onSuccess = { isLoading = false },
                onError = { hasError = true; isLoading = false }
            )
        }
    }
}

/**
 * 批量图片预加载
 */
class ImagePreloader(
    private val imageCacheRepository: ImageCacheRepository
) {
    private val preloadedUrls = mutableSetOf<String>()
    
    suspend fun preload(urls: List<String>) {
        val newUrls = urls.filter { it !in preloadedUrls }.take(30)
        if (newUrls.isNotEmpty()) {
            imageCacheRepository.preloadImages(newUrls)
            preloadedUrls.addAll(newUrls)
        }
    }
    
    fun isPreloaded(url: String): Boolean = url in preloadedUrls
}
