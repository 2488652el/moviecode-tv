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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()
    
    init {
        loadContent()
    }
    
    fun loadContent() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val popularMoviesResult = mediaRepository.getPopularMovies()
                val topRatedMoviesResult = mediaRepository.getTopRatedMovies()
                val popularTvResult = mediaRepository.getPopularTvShows()
                val topRatedTvResult = mediaRepository.getTopRatedTvShows()
                
                popularMoviesResult.getOrNull()?.let {
                    _popularMovies.value = it
                    if (_heroItem.value == null && it.isNotEmpty()) {
                        _heroItem.value = it.random()
                    }
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
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            mediaRepository.searchMedia(query).getOrNull()?.let {
                _searchResults.value = it
            }
        }
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
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    
    var searchText by remember { mutableStateOf("") }
    
    Row(modifier = Modifier.fillMaxSize().background(TvBackground)) {
        TvNavigationRail(
            selectedItem = selectedNavItem,
            onItemSelected = onNavItemSelected
        )
        
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Primary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = rememberLazyListState()
                ) {
                    // Search bar
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = {
                                    searchText = it
                                    viewModel.search(it)
                                },
                                placeholder = { Text("Search movies, TV shows...", color = TextTertiary) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TextSecondary
                                    )
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
                                    .width(500.dp)
                                    .focusable()
                            )
                        }
                    }
                    
                    // Search results
                    if (searchResults.isNotEmpty()) {
                        item {
                            Text(
                                text = "Search Results",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                            )
                        }
                        
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(searchResults) { item ->
                                    MediaPosterCard(
                                        item = item,
                                        isSelected = false,
                                        onFocus = {},
                                        onClick = { onMediaSelected(item) }
                                    )
                                }
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                    
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
                    item {
                        MediaRow(
                            title = "Popular Movies",
                            items = popularMovies,
                            selectedIndex = 0,
                            onItemSelected = {},
                            onItemClicked = onMediaSelected
                        )
                    }
                    
                    item {
                        MediaRow(
                            title = "Top Rated Movies",
                            items = topRatedMovies,
                            selectedIndex = 0,
                            onItemSelected = {},
                            onItemClicked = onMediaSelected
                        )
                    }
                    
                    // TV Shows sections
                    item {
                        MediaRow(
                            title = "Popular TV Shows",
                            items = popularTvShows,
                            selectedIndex = 0,
                            onItemSelected = {},
                            onItemClicked = onMediaSelected
                        )
                    }
                    
                    item {
                        MediaRow(
                            title = "Top Rated TV Shows",
                            items = topRatedTvShows,
                            selectedIndex = 0,
                            onItemSelected = {},
                            onItemClicked = onMediaSelected
                        )
                    }
                    
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
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
            .height(400.dp)
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
                
                item.genres.take(3).forEachIndexed { index, genre ->
                    Text(
                        text = genre,
                        color = TextSecondary,
                        fontSize = 18.sp
                    )
                    if (index < 2) {
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
                    text = overview.take(200) + if (overview.length > 200) "..." else "",
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
