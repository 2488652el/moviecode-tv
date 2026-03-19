package com.moviecode.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.moviecode.tv.data.repository.MediaRepository
import com.moviecode.tv.domain.model.CastMember
import com.moviecode.tv.domain.model.Episode
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.domain.model.MediaType
import com.moviecode.tv.domain.model.Season
import com.moviecode.tv.ui.components.MediaPosterCard
import com.moviecode.tv.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val tmdbId: Int = savedStateHandle.get<Int>("tmdbId") ?: 0
    private val mediaTypeStr: String = savedStateHandle.get<String>("mediaType") ?: "MOVIE"
    private val mediaType = MediaType.valueOf(mediaTypeStr)
    
    private val _mediaItem = MutableStateFlow<MediaItem?>(null)
    val mediaItem: StateFlow<MediaItem?> = _mediaItem.asStateFlow()
    
    private val _cast = MutableStateFlow<List<CastMember>>(emptyList())
    val cast: StateFlow<List<CastMember>> = _cast.asStateFlow()
    
    private val _seasons = MutableStateFlow<List<Season>>(emptyList())
    val seasons: StateFlow<List<Season>> = _seasons.asStateFlow()
    
    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes: StateFlow<List<Episode>> = _episodes.asStateFlow()
    
    private val _similar = MutableStateFlow<List<MediaItem>>(emptyList())
    val similar: StateFlow<List<MediaItem>> = _similar.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedSeason = MutableStateFlow<Int?>(null)
    val selectedSeason: StateFlow<Int?> = _selectedSeason.asStateFlow()
    
    init {
        loadDetails()
    }
    
    private fun loadDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (mediaType) {
                    MediaType.MOVIE -> {
                        mediaRepository.getMovieDetails(tmdbId).getOrNull()?.let {
                            _mediaItem.value = it
                        }
                        mediaRepository.getMovieCredits(tmdbId).getOrNull()?.let {
                            _cast.value = it
                        }
                        mediaRepository.getSimilarMovies(tmdbId).getOrNull()?.let {
                            _similar.value = it
                        }
                    }
                    MediaType.TV_SHOW, MediaType.ANIME -> {
                        mediaRepository.getTvDetails(tmdbId).getOrNull()?.let {
                            _mediaItem.value = it
                        }
                        mediaRepository.getTvCredits(tmdbId).getOrNull()?.let {
                            _cast.value = it
                        }
                        mediaRepository.getSimilarTvShows(tmdbId).getOrNull()?.let {
                            _similar.value = it
                        }
                        // Load first season by default
                        _selectedSeason.value = 1
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectSeason(seasonNumber: Int) {
        _selectedSeason.value = seasonNumber
        viewModelScope.launch {
            mediaRepository.getTvEpisodes(tmdbId, seasonNumber).getOrNull()?.let {
                _episodes.value = it
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val mediaItem by viewModel.mediaItem.collectAsState()
    val cast by viewModel.cast.collectAsState()
    val seasons by viewModel.seasons.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val similar by viewModel.similar.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(TvBackground)) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Primary
            )
        } else {
            mediaItem?.let { item ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Hero with backdrop
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(500.dp)
                        ) {
                            AsyncImage(
                                model = item.backdropPath ?: item.posterPath,
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                TvBackground.copy(alpha = 0.95f),
                                                TvBackground
                                            )
                                        )
                                    )
                            )
                            
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(32.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    color = TextPrimary,
                                    fontSize = 42.sp
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    item.year?.let {
                                        Text(text = "$it", color = TextSecondary, fontSize = 16.sp)
                                        Text(text = " • ", color = TextSecondary)
                                    }
                                    item.rating.let {
                                        Text(text = String.format("%.1f", it), color = Yellow, fontSize = 16.sp)
                                        Text(text = " • ", color = TextSecondary)
                                    }
                                    item.runtime?.let {
                                        Text(text = "${it} min", color = TextSecondary, fontSize = 16.sp)
                                    }
                                    if (item.type == MediaType.TV_SHOW) {
                                        item.seasonCount?.let {
                                            Text(text = " • $it Seasons", color = TextSecondary, fontSize = 16.sp)
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row {
                                    Button(
                                        onClick = { 
                                            item.filePath?.let { onPlay(it) }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Play")
                                    }
                                    
                                    Spacer(Modifier.width(16.dp))
                                    
                                    OutlinedButton(
                                        onClick = { },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("+ Add to List")
                                    }
                                }
                            }
                        }
                    }
                    
                    // Overview
                    item {
                        item.overview?.let { overview ->
                            Text(
                                text = overview,
                                color = TextSecondary,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                            )
                        }
                    }
                    
                    // Cast
                    if (cast.isNotEmpty()) {
                        item {
                            Text(
                                text = "Cast",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                            )
                        }
                        
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(cast) { member ->
                                    CastCard(member = member)
                                }
                            }
                        }
                    }
                    
                    // Seasons and Episodes (for TV shows)
                    if (item.type == MediaType.TV_SHOW) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = "Episodes",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                            )
                        }
                        
                        // Season selector
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items((1..(item.seasonCount ?: 1)).toList()) { seasonNum ->
                                    FilterChip(
                                        selected = selectedSeason == seasonNum,
                                        onClick = { viewModel.selectSeason(seasonNum) },
                                        label = { Text("Season $seasonNum") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Primary,
                                            selectedLabelColor = TextPrimary
                                        )
                                    )
                                }
                            }
                        }
                        
                        // Episodes list
                        items(episodes) { episode ->
                            EpisodeCard(
                                episode = episode,
                                onClick = { episode.filePath?.let { onPlay(it) } }
                            )
                        }
                    }
                    
                    // Similar
                    if (similar.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = "Similar",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                            )
                        }
                        
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(similar) { media ->
                                    MediaPosterCard(
                                        item = media,
                                        isSelected = false,
                                        onFocus = {},
                                        onClick = { },
                                        modifier = Modifier.width(140.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
        
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }
    }
}

@Composable
fun CastCard(member: CastMember) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        AsyncImage(
            model = member.profilePath,
            contentDescription = member.name,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(40.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = member.name,
            color = TextPrimary,
            fontSize = 12.sp
        )
        
        member.character?.let {
            Text(
                text = it,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun EpisodeCard(episode: Episode, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp)
            .focusable()
            .clip(RoundedCornerShape(8.dp))
            .background(CardBackground)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Episode thumbnail
        AsyncImage(
            model = episode.stillPath,
            contentDescription = episode.title,
            modifier = Modifier
                .width(200.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.episodeNumber}. ${episode.title}",
                color = TextPrimary,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            episode.airDate?.let {
                Text(text = it, color = TextTertiary, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            episode.overview?.let { overview ->
                Text(
                    text = overview.take(150) + if (overview.length > 150) "..." else "",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Text("Play")
        }
    }
}
