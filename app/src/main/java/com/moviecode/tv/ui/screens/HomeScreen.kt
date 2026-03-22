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
import com.moviecode.tv.data.repository.ParentalControlRepository
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

// 甯搁噺瀹氫箟
object HomeScreenConstants {
    const val SEARCH_DEBOUNCE_MS = 300L
    const val OVERVIEW_MAX_LENGTH = 200
    const val MAX_GENRES_DISPLAYED = 3
    const val HERO_HEIGHT = 480.dp
    const val SEARCH_BAR_MAX_WIDTH = 500.dp
    const val HERO_AUTO_SCROLL_INTERVAL_MS = 6000L
}

/**
 * 濯掍綋鍒嗙被鏋氫妇
 */
enum class MediaCategory(val title: String, val mediaType: MediaType?) {
    ALL("鎺ㄨ崘", null),
    MOVIES("鐢靛奖", MediaType.MOVIE),
    TV_SHOWS("鐢佃鍓?, MediaType.TV_SHOW),
    ANIME("鍔ㄦ极", MediaType.ANIME)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,`n    private val parentalControlRepository: ParentalControlRepository`n) : ViewModel() {
    
    // 鍏ㄩ儴鍒嗙被鏁版嵁
    private val _allPopularMovies = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _allTopRatedMovies = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _allPopularTvShows = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _allTopRatedTvShows = MutableStateFlow<List<MediaItem>>(emptyList())
    
    // 褰撳墠閫変腑鐨勫垎绫?
    private val _selectedCategory = MutableStateFlow(MediaCategory.ALL)
    val selectedCategory: StateFlow<MediaCategory> = _selectedCategory.asStateFlow()
    
    // Hero 杞挱鏁版嵁锛堜粠鎵€鏈夊垎绫讳腑鑾峰彇锛?
    private val _heroItems = MutableStateFlow<List<MediaItem>>(emptyList())`n    val heroItems: StateFlow<List<MediaItem>> = _heroItems.asStateFlow()`n    `n    // 家长控制过滤后的数据`n    private val _filteredHeroItems = MutableStateFlow<List<MediaItem>>(emptyList())`n    val filteredHeroItems: StateFlow<List<MediaItem>> = _filteredHeroItems.asStateFlow()
    
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
    
    private fun updateHeroItems() {`n        viewModelScope.launch {`n            val filteredItems = applyParentalFilter(_heroItems.value)`n            _filteredHeroItems.value = filteredItems`n        }
        val category = _selectedCategory.value
        val items = when (category) {
            MediaCategory.ALL -> (_allPopularMovies.value + _allPopularTvShows.value).take(10)
            MediaCategory.MOVIES -> _allPopularMovies.value
            MediaCategory.TV_SHOWS -> _allPopularTvShows.value
            MediaCategory.ANIME -> _allPopularTvShows.value.take(5) // 鍔ㄦ极鏆傜敤鍓ч泦鏁版嵁
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
                
                // 鍒濆鍖?Hero 鏁版嵁
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
    
    private suspend fun applyParentalFilter(items: List<MediaItem>): List<MediaItem> {`n        return items.filter { parentalControlRepository.isContentAllowed(it) }`n    }`n`n    fun clearError() {
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
    val heroItems by viewModel.filteredHeroItems.collectAsState()
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
            // 鎼滅储鏍?
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
            
            // 閿欒鎻愮ず
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
            
            // 涓诲唴瀹瑰尯鍩?
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
            placeholder = { Text("鎼滅储鐢靛奖銆佺數瑙嗗墽銆佸姩婕?..", color = TextTertiary) },
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
                Text("閲嶈瘯")
            }
            TextButton(onClick = onDismiss) {
                Text("鍏抽棴")
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
                text = "鎼滅储 \"$query\" 鐨勭粨鏋?,
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
                    title = "娌℃湁鎵惧埌缁撴灉",
                    message = "璇曡瘯鍏朵粬鍏抽敭璇?,
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
 * 澧炲己鐗堥椤靛唴瀹癸紙甯﹀垎绫荤瓫閫夊拰 Hero 杞挱锛?
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
        // ===== Hero 杞挱鍖哄煙 =====
        item {
            Column {
                // 鍒嗙被绛涢€?Tab
                CategoryTabBar(
                    selectedCategory = selectedCategory,
                    onCategorySelected = onCategorySelected
                )
                
                // Hero 杞挱
                if (heroItems.isNotEmpty()) {
                    HeroCarousel(
                        items = heroItems,
                        onItemClick = onMediaSelected
                    )
                }
            }
        }
        
        // ===== 鏍规嵁鍒嗙被鏄剧ず涓嶅悓鍐呭 =====
        when (selectedCategory) {
            MediaCategory.ALL -> {
                // 鎺ㄨ崘锛氭贩鍚堟樉绀?
                item {
                    EnhancedMediaRow(
                        title = "鐑棬鐢靛奖",
                        items = heroItems.filter { it.type == MediaType.MOVIE }.take(10),
                        selectedIndex = 0,
                        onItemSelected = {},
                        onItemClicked = onMediaSelected
                    )
                }
                item {
                    EnhancedMediaRow(
                        title = "鐑棬鐢佃鍓?,
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
                        title = "鐑棬鐢靛奖",
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
                        title = "鐑棬鐢佃鍓?,
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
                        title = "鐑棬鍔ㄦ极",
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
 * 鍒嗙被 Tab 鏍?
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
 * 鍒嗙被绛涢€夋寜閽?
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
            // 鍒嗙被鍥炬爣
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
 * Hero 杞挱缁勪欢
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit
) {
    if (items.isEmpty()) return
    
    // 鑷姩杞挱鐘舵€?
    var currentPage by remember { mutableIntStateOf(0) }
    
    // Pager 鐘舵€?
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { items.size }
    )
    
    // 鐩戝惉椤甸潰鍙樺寲
    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage
    }
    
    // 鑷姩杞挱
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
        // 杞挱鍐呭
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            CarouselPage(
                item = items[page],
                onClick = { onItemClick(items[page]) }
            )
        }
        
        // 搴曢儴娓愬彉
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
        
        // 鎸囩ず鍣?
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
        
        // 宸﹀彸瀵艰埅鎸夐挳
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 宸︽寜閽?
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
                        contentDescription = "涓婁竴寮?,
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // 鍙虫寜閽?
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
                        contentDescription = "涓嬩竴寮?,
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * 杞挱椤甸潰
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
        // 鑳屾櫙鍥剧墖
        AsyncImage(
            model = item.backdropPath ?: item.posterPath,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 娓愬彉閬僵
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
        
        // 宸︿晶鍏夋檿
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
        
        // 鍐呭
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 64.dp)
        ) {
            // 绫诲瀷鏍囩
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
                        MediaType.MOVIE -> "鐢靛奖"
                        MediaType.TV_SHOW -> "鐢佃鍓?
                        MediaType.ANIME -> "鍔ㄦ极"
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 鏍囬
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
            
            // 鍏冧俊鎭?
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.year?.let {
                    Text(text = "$it", color = TextSecondary, fontSize = 15.sp)
                    Text(text = " 鈥?", color = TextTertiary, fontSize = 15.sp)
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
                    Text(text = " 鈥?", color = TextTertiary, fontSize = 15.sp)
                }
                
                item.genres.take(2).forEachIndexed { index, genre ->
                    if (index > 0) {
                        Text(text = " 鈥?", color = TextTertiary, fontSize = 15.sp)
                    }
                    Text(text = genre, color = TextSecondary, fontSize = 15.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 绠€浠?
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
            
            // 鎸夐挳
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
                        text = "绔嬪嵆鎾斁",
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
                        text = "璇︽儏",
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * 椤甸潰鎸囩ず鍣?
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





