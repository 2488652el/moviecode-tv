package com.moviecode.tv.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.moviecode.tv.data.repository.MediaRepository
import com.moviecode.tv.domain.model.CastMember
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.domain.model.MediaType
import com.moviecode.tv.ui.components.MediaPosterCard
import com.moviecode.tv.ui.components.NavigationItem
import com.moviecode.tv.ui.components.TvNavigationRail
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
    const val HERO_HEIGHT = 400.dp
    const val SEARCH_BAR_MAX_WIDTH = 500.dp
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {
    
    private val _popularMovies = MutableStateFlow<List<MediaItem>>(emptyList())
    val popularMovies: StateFlow<List<MediaItem>> = _popularMovies.asStateFlow()
    
    private val _topRatedMovies = MutableStateFlow<List<MediaItem>>(emptyList())
    val topRatedMovies: StateFlow<List<MediaItem>> = _topRatedMovies.asStateFlow()
    
    private val _popularTvShows = MutableStateFlow<List<MediaItem>>(emptyList())
    val popularTvShows: StateFlow<List<MediaItem>> = _popularTvShows.asStateFlow()
    
    private val _topRatedTvShows = MutableStateFlow<List<MediaItem>>(emptyList())
    val topRatedTvShows: StateFlow<List<MediaItem>> = _topRatedTvShows.asStateFlow()
    
    private val _heroItem = MutableStateFlow<MediaItem?>(null)
    val heroItem: StateFlow<MediaItem?> = _heroItem.asStateFlow()
    
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
    
    init {
        loadContent()
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
                    _popularMovies.value = it
                    if (_heroItem.value == null && it.isNotEmpty()) {
                        _heroItem.value = it.first() // 使用第一个而非随机
                    }
                } ?: run {
                    _error.value = "Failed to load popular movies"
                }
                
                topRatedMoviesResult.getOrNull()?.let {
                    _topRatedMovies.value = it
                }
                
                popularTvResult.getOrNull()?.let {
                    _popularTvShows.value = it
                }
                
                topRatedTvResult.getOrNull()?.let {
                    _topRatedTvShows.value = it
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // 防抖搜索
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
}

@Composable
fun HomeScreen(
    onMediaSelected: (MediaItem) -> Unit,
    selectedNavItem: NavigationItem,
    onNavItemSelected: (NavigationItem) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val popularMovies by viewModel.popularMovies.collectAsState()
    val topRatedMovies by viewModel.topRatedMovies.collectAsState()
    val popularTvShows by viewModel.popularTvShows.collectAsState()
    val topRatedTvShows by viewModel.topRatedTvShows.collectAsState()
    val heroItem by viewModel.heroItem.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var searchText by remember { mutableStateOf("") }
    
    Row(modifier = Modifier.fillMaxSize().background(TvBackground)) {
        TvNavigationRail(
            selectedItem = selectedNavItem,
            onItemSelected = onNavItemSelected
        )
        
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // ===== 固定搜索栏 (不遮挡海报) =====
            SearchBar(
                query = searchText,
                onQueryChange = { 
                    searchText = it
                    viewModel.search(it)
                },
                isSearching = isSearching,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            )
            
            // ===== 错误提示 =====
            error?.let { errorMessage ->
                ErrorBanner(
                    message = errorMessage,
                    onRetry = { viewModel.loadContent() },
                    onDismiss = { viewModel.clearError() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 8.dp)
                )
            }
            
            // ===== 主内容区域 =====
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Primary
                        )
                    }
                    
                    // 搜索结果视图
                    searchQuery.isNotBlank() -> {
                        SearchResultsView(
                            query = searchQuery,
                            results = searchResults,
                            isSearching = isSearching,
                            onMediaSelected = onMediaSelected
                        )
                    }
                    
                    // 正常首页视图
                    else -> {
                        HomeContentView(
                            popularMovies = popularMovies,
                            topRatedMovies = topRatedMovies,
                            popularTvShows = popularTvShows,
                            topRatedTvShows = topRatedTvShows,
                            heroItem = heroItem,
                            onMediaSelected = onMediaSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
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
            placeholder = { Text("Search movies, TV shows...", color = TextTertiary) },
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
                        color = Primary,
                        strokeWidth = 2.dp
                    )
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Divider,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = Primary
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
            containerColor = Error.copy(alpha = 0.1f)
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
                tint = Error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = Error,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
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
        // 搜索标题
        item {
            Text(
                text = "Search Results for \"$query\"",
                color = TextPrimary,
                fontSize = 24.sp,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }
        
        // 加载中
        if (isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
        }
        
        // 无结果提示
        if (!isSearching && results.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No results found",
                    message = "Try searching with different keywords",
                    modifier = Modifier.padding(32.dp)
                )
            }
        }
        
        // 搜索结果
        if (results.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(results) { item ->
                        MediaPosterCard(
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

@Composable
fun HomeContentView(
    popularMovies: List<MediaItem>,
    topRatedMovies: List<MediaItem>,
    popularTvShows: List<MediaItem>,
    topRatedTvShows: List<MediaItem>,
    heroItem: MediaItem?,
    onMediaSelected: (MediaItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Hero Banner
        item {
            heroItem?.let { hero ->
                HeroBanner(
                    item = hero,
                    onClick = { onMediaSelected(hero) }
                )
            }
        }
        
        // Movies sections
        if (popularMovies.isNotEmpty()) {
            item {
                MediaRow(
                    title = "Popular Movies",
                    items = popularMovies,
                    selectedIndex = 0,
                    onItemSelected = {},
                    onItemClicked = onMediaSelected
                )
            }
        }
        
        if (topRatedMovies.isNotEmpty()) {
            item {
                MediaRow(
                    title = "Top Rated Movies",
                    items = topRatedMovies,
                    selectedIndex = 0,
                    onItemSelected = {},
                    onItemClicked = onMediaSelected
                )
            }
        }
        
        // TV Shows sections
        if (popularTvShows.isNotEmpty()) {
            item {
                MediaRow(
                    title = "Popular TV Shows",
                    items = popularTvShows,
                    selectedIndex = 0,
                    onItemSelected = {},
                    onItemClicked = onMediaSelected
                )
            }
        }
        
        if (topRatedTvShows.isNotEmpty()) {
            item {
                MediaRow(
                    title = "Top Rated TV Shows",
                    items = topRatedTvShows,
                    selectedIndex = 0,
                    onItemSelected = {},
                    onItemClicked = onMediaSelected
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
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

@Composable
fun HeroBanner(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeScreenConstants.HERO_HEIGHT)
            .focusable()
    ) {
        // Backdrop image
        AsyncImage(
            model = item.backdropPath ?: item.posterPath,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TvBackground.copy(alpha = 0.7f),
                            TvBackground
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(48.dp)
        ) {
            Text(
                text = item.title,
                color = TextPrimary,
                fontSize = 48.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Genres and rating
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.year != null) {
                    Text(
                        text = "${item.year}",
                        color = TextSecondary,
                        fontSize = 18.sp
                    )
                    Text(
                        text = " • ",
                        color = TextSecondary,
                        fontSize = 18.sp
                    )
                }
                
                item.genres.take(HomeScreenConstants.MAX_GENRES_DISPLAYED).forEachIndexed { index, genre ->
                    Text(
                        text = genre,
                        color = TextSecondary,
                        fontSize = 18.sp
                    )
                    if (index < HomeScreenConstants.MAX_GENRES_DISPLAYED - 1) {
                        Text(
                            text = " • ",
                            color = TextSecondary,
                            fontSize = 18.sp
                        )
                    }
                }
                
                if (item.rating > 0) {
                    Text(
                        text = " • ",
                        color = TextSecondary,
                        fontSize = 18.sp
                    )
                    Text(
                        text = String.format("%.1f", item.rating),
                        color = Yellow,
                        fontSize = 18.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Overview
            item.overview?.let { overview ->
                Text(
                    text = overview.take(HomeScreenConstants.OVERVIEW_MAX_LENGTH) + 
                           if (overview.length > HomeScreenConstants.OVERVIEW_MAX_LENGTH) "..." else "",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier.width(800.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Play button
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Play",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                )
            }
        }
    }
}
