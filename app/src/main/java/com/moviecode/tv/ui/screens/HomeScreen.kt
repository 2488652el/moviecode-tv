package com.moviecode.tv.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.moviecode.tv.data.repository.MediaRepository
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.domain.model.MediaType
import com.moviecode.tv.ui.components.EnhancedMediaPosterCard
import com.moviecode.tv.ui.components.EnhancedMediaRow
import com.moviecode.tv.ui.components.GlassNavigationRail
import com.moviecode.tv.ui.components.NavigationItem
import com.moviecode.tv.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 常量定义
object HomeScreenConstants {
    const val SEARCH_DEBOUNCE_MS = 300L
    const val OVERVIEW_MAX_LENGTH = 200
    const val MAX_GENRES_DISPLAYED = 3
    const val HERO_HEIGHT = 480.dp
    const val SEARCH_BAR_MAX_WIDTH = 500.dp
    const val HERO_AUTO_SCROLL_INTERVAL_MS = 6000L
}

/**
 * 媒体分类枚举
 */
enum class MediaCategory(val title: String, val mediaType: MediaType?) {
    ALL("推荐", null),
    MOVIES("电影", MediaType.MOVIE),
    TV_SHOWS("电视剧", MediaType.TV_SHOW),
    ANIME("动漫", MediaType.ANIME)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {
    
    // 全部分类数据
    private val _allPopularMovies = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _allTopRatedMovies = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _allPopularTvShows = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _allTopRatedTvShows = MutableStateFlow<List<MediaItem>>(emptyList())
    
    // 当前选中的分类
    private val _selectedCategory = MutableStateFlow(MediaCategory.ALL)
    val selectedCategory: StateFlow<MediaCategory> = _selectedCategory.asStateFlow()
    
    // Hero 轮播数据（从所有分类中获取）
    private val _heroItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val heroItems: StateFlow<List<MediaItem>> = _heroItems.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var searchJob: Job? = null
    private var autoScrollJob: Job? = null
    
    init {
        loadContent()
    }
    
    fun selectCategory(category: MediaCategory) {
        _selectedCategory.value = category
        updateHeroItems()
    }
    
    private fun updateHeroItems() {
        val category = _selectedCategory.value
        val items = when (category) {
            MediaCategory.ALL -> (_allPopularMovies.value + _allPopularTvShows.value).take(10)
            MediaCategory.MOVIES -> _allPopularMovies.value
            MediaCategory.TV_SHOWS -> _allPopularTvShows.value
            MediaCategory.ANIME -> _allPopularTvShows.value.take(5) // 动漫暂用剧集数据
        }
        _heroItems.value = items.take(10)
    }
    
    fun loadContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val popularMoviesResult = mediaRepository.getPopularMovies()
                val topRatedMoviesResult = mediaRepository.getTopRatedMovies()
                val popularTvResult = mediaRepository.getPopularTvShows()
                val topRatedTvResult = mediaRepository.getTopRatedTvShows()
                
                popularMoviesResult.getOrNull()?.let {
                    _allPopularMovies.value = it
                } ?: run {
                    _error.value = "Failed to load popular movies"
                }
                
                topRatedMoviesResult.getOrNull()?.let {
                    _allTopRatedMovies.value = it
                }
                
                popularTvResult.getOrNull()?.let {
                    _allPopularTvShows.value = it
                }
                
                topRatedTvResult.getOrNull()?.let {
                    _allTopRatedTvShows.value = it
                }
                
                // 初始化 Hero 数据
                updateHeroItems()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun search(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(HomeScreenConstants.SEARCH_DEBOUNCE_MS)
            try {
                val result = mediaRepository.searchMedia(query).getOrNull()
                _searchResults.value = result ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        autoScrollJob?.cancel()
    }
}

@Composable
fun HomeScreen(
    onMediaSelected: (MediaItem) -> Unit,
    selectedNavItem: NavigationItem,
    onNavItemSelected: (NavigationItem) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val heroItems by viewModel.heroItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    
    var searchText by remember { mutableStateOf("") }
    
    Row(modifier = Modifier.fillMaxSize().background(BackgroundPrimary)) {
        GlassNavigationRail(
            selectedItem = selectedNavItem,
            onItemSelected = onNavItemSelected
        )
        
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // 搜索栏
            EnhancedSearchBar(
                query = searchText,
                onQueryChange = { 
                    searchText = it
                    viewModel.search(it)
                },
                isSearching = isSearching,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 16.dp)
            )
            
            // 错误提示
            error?.let { errorMessage ->
                ErrorBanner(
                    message = errorMessage,
                    onRetry = { viewModel.loadContent() },
                    onDismiss = { viewModel.clearError() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 8.dp)
                )
            }
            
            // 主内容区域
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = GradientStart
                        )
                    }
                    
                    searchQuery.isNotBlank() -> {
                        SearchResultsView(
                            query = searchQuery,
                            results = searchResults,
                            isSearching = isSearching,
                            onMediaSelected = onMediaSelected
                        )
                    }
                    
                    else -> {
                        EnhancedHomeContentView(
                            heroItems = heroItems,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { viewModel.selectCategory(it) },
                            onMediaSelected = onMediaSelected,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("搜索电影、电视剧、动漫...", color = TextTertiary) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary
                )
            },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = GradientStart,
                        strokeWidth = 2.dp
                    )
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GradientStart,
                unfocusedBorderColor = Divider,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = GradientStart
            ),
            modifier = Modifier
                .widthIn(max = HomeScreenConstants.SEARCH_BAR_MAX_WIDTH)
                .fillMaxWidth()
                .focusable()
        )
    }
}

@Composable
fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = ErrorRed.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorRed
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = ErrorRed,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("重试")
            }
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    }
}

@Composable
fun SearchResultsView(
    query: String,
    results: List<MediaItem>,
    isSearching: Boolean,
    onMediaSelected: (MediaItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "搜索 \"$query\" 的结果",
                color = TextPrimary,
                fontSize = 24.sp,
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
            )
        }
        
        if (isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GradientStart)
                }
            }
        }
        
        if (!isSearching && results.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "没有找到结果",
                    message = "试试其他关键词",
                    modifier = Modifier.padding(48.dp)
                )
            }
        }
        
        if (results.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(results) { item ->
                        EnhancedMediaPosterCard(
                            item = item,
                            isSelected = false,
                            onFocus = {},
                            onClick = { onMediaSelected(item) }
                        )
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

/**
 * 增强版首页内容（带分类筛选和 Hero 轮播）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedHomeContentView(
    heroItems: List<MediaItem>,
    selectedCategory: MediaCategory,
    onCategorySelected: (MediaCategory) -> Unit,
    onMediaSelected: (MediaItem) -> Unit,
    viewModel: HomeViewModel
) {
    val popularMovies by viewModel.heroItems.collectAsState()
    val allPopular by viewModel.heroItems.collectAsState()
    val topRatedMovies by remember { mutableStateOf(emptyList<MediaItem>()) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // ===== Hero 轮播区域 =====
        item {
            Column {
                // 分类筛选 Tab
                CategoryTabBar(
                    selectedCategory = selectedCategory,
                    onCategorySelected = onCategorySelected
                )
                
                // Hero 轮播
                if (heroItems.isNotEmpty()) {
                    HeroCarousel(
                        items = heroItems,
                        onItemClick = onMediaSelected
                    )
                }
            }
        }
        
        // ===== 根据分类显示不同内容 =====
        when (selectedCategory) {
            MediaCategory.ALL -> {
                // 推荐：混合显示
                item {
                    EnhancedMediaRow(
                        title = "热门电影",
                        items = heroItems.filter { it.type == MediaType.MOVIE }.take(10),
                        selectedIndex = 0,
                        onItemSelected = {},
                        onItemClicked = onMediaSelected
                    )
                }
                item {
                    EnhancedMediaRow(
                        title = "热门电视剧",
                        items = heroItems.filter { it.type == MediaType.TV_SHOW }.take(10),
                        selectedIndex = 0,
                        onItemSelected = {},
                        onItemClicked = onMediaSelected
                    )
                }
            }
            
            MediaCategory.MOVIES -> {
                item {
                    EnhancedMediaRow(
                        title = "热门电影",
                        items = heroItems.take(10),
                        selectedIndex = 0,
                        onItemSelected = {},
                        onItemClicked = onMediaSelected
                    )
                }
            }
            
            MediaCategory.TV_SHOWS -> {
                item {
                    EnhancedMediaRow(
                        title = "热门电视剧",
                        items = heroItems.take(10),
                        selectedIndex = 0,
                        onItemSelected = {},
                        onItemClicked = onMediaSelected
                    )
                }
            }
            
            MediaCategory.ANIME -> {
                item {
                    EnhancedMediaRow(
                        title = "热门动漫",
                        items = heroItems.take(10),
                        selectedIndex = 0,
                        onItemSelected = {},
                        onItemClicked = onMediaSelected
                    )
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

/**
 * 分类 Tab 栏
 */
@Composable
fun CategoryTabBar(
    selectedCategory: MediaCategory,
    onCategorySelected: (MediaCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MediaCategory.entries.forEach { category ->
            CategoryChip(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

/**
 * 分类筛选按钮
 */
@Composable
fun CategoryChip(
    category: MediaCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            when (category) {
                MediaCategory.ALL -> GradientStart
                MediaCategory.MOVIES -> GradientStart
                MediaCategory.TV_SHOWS -> GradientEnd
                MediaCategory.ANIME -> GradientAccent
            }
        } else {
            BackgroundCard
        },
        animationSpec = tween(200),
        label = "bgColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextSecondary,
        animationSpec = tween(200),
        label = "contentColor"
    )
    
    Surface(
        onClick = onClick,
        modifier = Modifier.focusable(),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标
            Icon(
                imageVector = when (category) {
                    MediaCategory.ALL -> Icons.Default.Home
                    MediaCategory.MOVIES -> Icons.Default.Movie
                    MediaCategory.TV_SHOWS -> Icons.Default.Tv
                    MediaCategory.ANIME -> Icons.Default.Star
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = category.title,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

/**
 * Hero 轮播组件
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit
) {
    if (items.isEmpty()) return
    
    // 自动轮播状态
    var currentPage by remember { mutableIntStateOf(0) }
    
    // Pager 状态
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { items.size }
    )
    
    // 监听页面变化
    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage
    }
    
    // 自动轮播
    LaunchedEffect(Unit) {
        while (true) {
            delay(HomeScreenConstants.HERO_AUTO_SCROLL_INTERVAL_MS)
            val nextPage = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(nextPage)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HomeScreenConstants.HERO_HEIGHT)
    ) {
        // 轮播内容
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            CarouselPage(
                item = items[page],
                onClick = { onItemClick(items[page]) }
            )
        }
        
        // 底部渐变
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BackgroundPrimary
                        )
                    )
                )
        )
        
        // 指示器
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEachIndexed { index, _ ->
                PageIndicator(
                    isSelected = index == currentPage,
                    index = index
                )
            }
        }
        
        // 左右导航按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左按钮
            Surface(
                onClick = {
                    val prevPage = if (pagerState.currentPage > 0) 
                        pagerState.currentPage - 1 
                    else 
                        items.size - 1
                    pagerState.animateScrollToPage(prevPage)
                },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = BackgroundCard.copy(alpha = 0.8f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "上一张",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // 右按钮
            Surface(
                onClick = {
                    val nextPage = (pagerState.currentPage + 1) % items.size
                    pagerState.animateScrollToPage(nextPage)
                },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = BackgroundCard.copy(alpha = 0.8f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "下一张",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * 轮播页面
 */
@Composable
fun CarouselPage(
    item: MediaItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
    ) {
        // 背景图片
        AsyncImage(
            model = item.backdropPath ?: item.posterPath,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BackgroundDeep.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Transparent,
                            BackgroundPrimary.copy(alpha = 0.8f)
                        )
                    )
                )
        )
        
        // 左侧光晕
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(350.dp)
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            when (item.type) {
                                MediaType.MOVIE -> GradientStart.copy(alpha = 0.12f)
                                MediaType.TV_SHOW -> GradientEnd.copy(alpha = 0.12f)
                                MediaType.ANIME -> GradientAccent.copy(alpha = 0.12f)
                            },
                            Color.Transparent
                        )
                    )
                )
        )
        
        // 内容
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 64.dp)
        ) {
            // 类型标签
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
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 54.sp,
                letterSpacing = (-0.5).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 650.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 元信息
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.year?.let {
                    Text(text = "$it", color = TextSecondary, fontSize = 15.sp)
                    Text(text = " • ", color = TextTertiary, fontSize = 15.sp)
                }
                
                if (item.rating > 0) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = RatingGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = " ${String.format("%.1f", item.rating)} ",
                        color = RatingGold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = " • ", color = TextTertiary, fontSize = 15.sp)
                }
                
                item.genres.take(2).forEachIndexed { index, genre ->
                    if (index > 0) {
                        Text(text = " • ", color = TextTertiary, fontSize = 15.sp)
                    }
                    Text(text = genre, color = TextSecondary, fontSize = 15.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 简介
            item.overview?.let { overview ->
                Text(
                    text = overview.take(150) + if (overview.length > 150) "..." else "",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 550.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 按钮
            Row {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = BackgroundDeep,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "立即播放",
                        color = BackgroundDeep,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                OutlinedButton(
                    onClick = { },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.4f))
                        )
                    )
                ) {
                    Text(
                        text = "详情",
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * 页面指示器
 */
@Composable
fun PageIndicator(
    isSelected: Boolean,
    index: Int
) {
    val width by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        ),
        label = "indicatorWidth"
    )
    
    val color by animateColorAsState(
        targetValue = if (isSelected) GradientStart else TextDisabled,
        animationSpec = tween(200),
        label = "indicatorColor"
    )
    
    Box(
        modifier = Modifier
            .width(width)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
