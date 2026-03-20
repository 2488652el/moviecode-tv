# MovieCode TV - UI 美化设计方案

## 项目概述
- **项目类型**: Android TV 本地媒体中心
- **当前框架**: Jetpack Compose + Material3 TV
- **目标风格**: Apple TV 极简沉浸式设计

---

## 1. 设计系统增强

### 1.1 色彩系统 (Enhanced Color Palette)

```kotlin
// ===== 核心色彩增强 =====

// 背景层次
val BackgroundDeep = Color(0xFF0A0A0C)           // 最深背景
val BackgroundPrimary = Color(0xFF101214)         // 主背景
val BackgroundElevated = Color(0xFF18181B)        // 提升层级背景
val BackgroundCard = Color(0xFF1C1C1E)            // 卡片背景
val BackgroundCardHover = Color(0xFF2C2C2E)       // 卡片悬停

// 主色调增强 - 渐变色系统
val GradientStart = Color(0xFF0A84FF)             // 蓝色起点
val GradientEnd = Color(0xFF5E5CE6)              // 紫色终点
val GradientAccent = Color(0xFFFF6B9D)            // 粉紫渐变

// 玻璃态效果
val GlassOverlay = Color(0x40FFFFFF)               // 20% 白色
val GlassBorder = Color(0x30FFFFFF)               // 12% 白色边框

// 语义色彩增强
val RatingGold = Color(0xFFFFD60A)               // 评分金色
val SuccessGreen = Color(0xFF32D74B)             // 成功绿
val WarningAmber = Color(0xFFFFD60A)             // 警告琥珀
val ErrorRed = Color(0xFFFF453A)                 // 错误红
val InfoBlue = Color(0xFF0A84FF)                 // 信息蓝

// 文字层次
val TextOnDark = Color(0xFFFFFFFF)                // 深色背景上的文字
val TextPrimary = Color(0xFFF5F5F7)              // 主要文字
val TextSecondary = Color(0xFF8E8E93)            // 次要文字
val TextTertiary = Color(0xFF636366)             // 三级文字
val TextDisabled = Color(0xFF48484A)              // 禁用文字
```

### 1.2 字体系统 (Typography System)

```kotlin
// Apple SF Pro 风格字体系统
object MovieCodeTypography {
    // 大标题 - Hero区域
    val HeroTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.5).sp
    )

    // 区块标题
    val SectionTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    )

    // 卡片标题
    val CardTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )

    // 正文描述
    val Body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    )

    // 标签文字
    val Label = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
}
```

### 1.3 间距系统 (Spacing System)

```kotlin
// 8pt 网格系统
object Spacing {
    val xxs = 4.dp    // 极小间距
    val xs = 8.dp     // 小间距
    val sm = 12.dp    // 较小间距
    val md = 16.dp    // 中等间距
    val lg = 24.dp    // 大间距
    val xl = 32.dp    // 较大间距
    val xxl = 48.dp   // 极大间距
    val xxxl = 64.dp  // 超大间距
}

// 圆角系统
object Radius {
    val xs = 4.dp      // 微圆角
    val sm = 8.dp     // 小圆角
    val md = 12.dp    // 中圆角
    val lg = 16.dp    // 大圆角
    val xl = 24.dp    // 超大圆角
    val full = 999.dp // 全圆角
}
```

---

## 2. 组件系统增强

### 2.1 增强版海报卡片 (Enhanced PosterCard)

```kotlin
@Composable
fun EnhancedMediaPosterCard(
    item: MediaItem,
    isSelected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        ),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 8.dp,
        animationSpec = tween(200),
        label = "elevation"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .width(180.dp)
            .focusable()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
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

            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                BackgroundDeep.copy(alpha = 0.8f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // 选中指示器
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

            // 标题
            Text(
                text = item.title,
                color = if (isSelected) Color.White else TextPrimary,
                fontSize = 14.sp,
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
```

### 2.2 玻璃态导航栏 (Glass Navigation Rail)

```kotlin
@Composable
fun GlassNavigationRail(
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(220.dp),
        color = BackgroundDeep.copy(alpha = 0.7f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BackgroundElevated.copy(alpha = 0.3f),
                            BackgroundDeep.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassBorder,
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(0.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo 区域
                Box(
                    modifier = Modifier
                        .padding(bottom = 48.dp)
                ) {
                    Text(
                        text = "MovieCode",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // 导航项
                NavigationItem.entries.forEach { item ->
                    EnhancedNavItem(
                        item = item,
                        isSelected = item == selectedItem,
                        onSelect = { onItemSelected(item) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                // 版本信息
                Text(
                    text = "v1.0.0",
                    color = TextDisabled,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun EnhancedNavItem(
    item: NavigationItem,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) GradientStart.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(200),
        label = "bg"
    )

    Surface(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .focusable(),
        color = backgroundColor,
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) {
            BorderStroke(1.dp, GradientStart.copy(alpha = 0.5f))
        } else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected) GradientStart else TextSecondary,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = item.title,
                color = if (isSelected) Color.White else TextSecondary,
                fontSize = 17.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
```

### 2.3 增强版 Hero Banner

```kotlin
@Composable
fun EnhancedHeroBanner(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(480.dp)
            .focusable()
    ) {
        // 背景图片 - 模糊处理
        AsyncImage(
            model = item.backdropPath ?: item.posterPath,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 添加轻微模糊效果
                    // 动态模糊实现见下文
                },
            contentScale = ContentScale.Crop
        )

        // 多层渐变叠加
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BackgroundDeep.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Transparent,
                            BackgroundPrimary.copy(alpha = 0.9f),
                            BackgroundPrimary
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // 左侧装饰光晕
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            GradientStart.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 内容区域
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(600)) +
                    slideInHorizontally(animationSpec = tween(600)) { -50 }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 64.dp, bottom = 60.dp)
            ) {
                // 媒体类型
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (item.type) {
                        MediaType.MOVIE -> GradientStart
                        MediaType.TV_SHOW -> GradientEnd
                        MediaType.ANIME -> GradientAccent
                    }
                ) {
                    Text(
                        text = when (item.type) {
                            MediaType.MOVIE -> "电影"
                            MediaType.TV_SHOW -> "电视剧"
                            MediaType.ANIME -> "动漫"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 标题
                Text(
                    text = item.title,
                    style = MovieCodeTypography.HeroTitle,
                    color = Color.White,
                    modifier = Modifier.widthIn(max = 700.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 元信息行
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 年份
                    item.year?.let {
                        Text(
                            text = "$it",
                            color = TextSecondary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = " • ",
                            color = TextTertiary,
                            fontSize = 16.sp
                        )
                    }

                    // 评分
                    if (item.rating > 0) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = RatingGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = " ${String.format("%.1f", item.rating)} ",
                            color = RatingGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = " • ",
                            color = TextTertiary,
                            fontSize = 16.sp
                        )
                    }

                    // 类型标签
                    item.genres.take(3).forEachIndexed { index, genre ->
                        if (index > 0) {
                            Text(
                                text = " • ",
                                color = TextTertiary,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = genre,
                            color = TextSecondary,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 简介
                item.overview?.let { overview ->
                    Text(
                        text = overview.take(180) + if (overview.length > 180) "..." else "",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 操作按钮
                Row {
                    // 播放按钮
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = BackgroundDeep,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "立即播放",
                            color = BackgroundDeep,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 详情按钮
                    OutlinedButton(
                        onClick = { },
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "查看详情",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
```

---

## 3. 布局系统增强

### 3.1 横向滑动列表组件

```kotlin
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
                style = MovieCodeTypography.SectionTitle,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // 查看更多按钮
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
```

---

## 4. 动效系统

### 4.1 全局动画配置

```kotlin
// 动画常量
object AnimationSpec {
    // 快速交互
    val Quick = AnimationSpec.tween<Float>(
        durationMillis = 150,
        easing = FastOutSlowInEasing
    )

    // 正常过渡
    val Normal = AnimationSpec.tween<Float>(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )

    // 缓慢展开
    val Slow = AnimationSpec.tween<Float>(
        durationMillis = 500,
        easing = FastOutSlowInEasing
    )

    // 弹性效果
    val Spring = AnimationSpec.spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 300f
    )
}

// 焦点缩放动画
val focusScale = @Composable Animatable(1f)

// 列表项入场动画
fun Modifier.animateItemEnter(): Modifier = this.then(
    animateItemPlacement(
        animationSpec = tween(300)
    )
)
```

### 4.2 页面过渡效果

```kotlin
// 页面切换动画
@Composable
fun SharedElementTransition(
    content: @Composable () -> Unit
) {
    AnimatedContent(
        transitionSpec = {
            fadeIn(animationSpec = tween(400)) togetherWith
            fadeOut(animationSpec = tween(400)) +
            scaleOut(targetScale = 0.95f, animationSpec = tween(400))
        },
        modifier = Modifier
    ) {
        content()
    }
}

// 详情页展开动画
@Composable
fun DetailExpandTransition(
    mediaItem: MediaItem?,
    content: @Composable (MediaItem) -> Unit
) {
    AnimatedVisibility(
        visible = mediaItem != null,
        enter = fadeIn(tween(300)) +
                expandVertically(tween(400)),
        exit = fadeOut(tween(200)) +
               shrinkVertically(tween(300))
    ) {
        mediaItem?.let { content(it) }
    }
}
```

---

## 5. 状态设计

### 5.1 加载状态

```kotlin
@Composable
fun ShimmerPosterCard() {
    ShimmerEffect(
        modifier = Modifier
            .width(180.dp)
            .height(270.dp),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val shimmerColors = listOf(
        BackgroundCard.copy(alpha = 0.6f),
        BackgroundCardHover.copy(alpha = 0.2f),
        BackgroundCard.copy(alpha = 0.6f)
    )

    Box(
        modifier = modifier
            .background(
                color = BackgroundCard,
                shape = shape
            )
    ) {
        // shimmer 动画效果
    }
}
```

### 5.2 空状态

```kotlin
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MovieCodeTypography.SectionTitle,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 16.sp
        )

        action?.let {
            Spacer(modifier = Modifier.height(32.dp))
            it()
        }
    }
}
```

---

## 6. 可访问性增强

### 6.1 TV 遥控器焦点管理

```kotlin
// 焦点指示器样式
val CustomFocusIndicator: @Composable Modifier.() -> Modifier = {
    this.then(
        Modifier.drawBehind {
            drawRoundRect(
                color = GradientStart,
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    )
}

// 焦点区域扩大
val FocusablePadding = 8.dp

// 确保可点击区域足够大
val MinTouchTarget = 48.dp
```

### 6.2 内容描述

```kotlin
// 图片内容描述
ContentDescription(
    poster = "《${item.title}》的海报图片，评分为${item.rating}分"
)

ContentDescription(
    backdrop = "《${item.title}》的背景图片"
)

// 操作按钮描述
ContentDescription(
    play = "播放《${item.title}》"
)

ContentDescription(
    addToList = "将《${item.title}》添加到观看列表"
)
```

---

## 7. 性能优化

### 7.1 图片加载优化

```kotlin
// Coil 图片配置
val ImageLoaderConfig = ImageLoader.Builder(context)
    .crossfade(true)
    .crossfade(300)
    .bitmapConfig(Bitmap.Config.RGB_565) // 降低内存占用
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(0.25)
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(cacheDir.resolve("image_cache"))
            .maxSizeBytes(100 * 1024 * 1024) // 100MB
            .build()
    }
    .build()
```

### 7.2 懒加载优化

```kotlin
// 预加载相邻项
val PrefetchDistance = 3

// 图片预加载
LazyRow(
    contentPadding = PaddingValues(horizontal = 48.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp)
) {
    items(
        items = items,
        key = { it.id }
    ) { item ->
        AsyncImage(
            model = item.posterPath,
            // 预加载配置
        )
    }
}
```

---

## 8. 实施计划

### Phase 1: 基础设计系统 (第1周)
- [ ] 更新颜色系统
- [ ] 定义字体规范
- [ ] 建立间距系统
- [ ] 创建基础组件样式

### Phase 2: 核心组件重构 (第2周)
- [ ] 重构海报卡片组件
- [ ] 重构导航栏组件
- [ ] 重构 Hero Banner
- [ ] 添加动画效果

### Phase 3: 页面级优化 (第3周)
- [ ] 优化首页布局
- [ ] 优化详情页
- [ ] 优化搜索体验
- [ ] 优化设置页面

### Phase 4: 打磨与测试 (第4周)
- [ ] 焦点行为调试
- [ ] 性能优化
- [ ] 可访问性测试
- [ ] 多设备适配

---

## 9. 设计预览

请查看同目录下的 `UI_Preview.html` 文件获取完整的设计预览效果。

---

**设计方案版本**: v1.0
**更新日期**: 2026-03-20
**设计师**: UI Designer Agent
