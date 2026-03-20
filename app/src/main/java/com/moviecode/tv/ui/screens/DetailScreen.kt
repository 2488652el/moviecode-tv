package com.moviecode.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import com.moviecode.tv.ui.components.EnhancedMediaPosterCard
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
    
    Box(modifier = Modifier.fillMaxSize().background(BackgroundPrimary)) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = GradientStart
            )
        } else {
            mediaItem?.let { item ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Hero 区域
                    item {
                        EnhancedDetailHero(
                            item = item,
                            onPlay = { item.filePath?.let { onPlay(it) } }
                        )
                    }
                    
                    // 简介
                    item.overview?.let { overview ->
                        item {
                            Text(
                                text = overview,
                                color = TextSecondary,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp)
                            )
                        }
                    }
                    
                    // 演职员
                    if (cast.isNotEmpty()) {
                        item {
                            SectionHeader(title = "演职员")
                        }
                        
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 48.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                items(cast) { member ->
                                    CastCard(member = member)
                                }
                            }
                        }
                    }
                    
                    // 剧集（电视剧）
                    if (item.type == MediaType.TV_SHOW) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(title = "剧集")
                        }
                        
                        item {
                            SeasonSelector(
                                seasonCount = item.seasonCount ?: 1,
                                selectedSeason = selectedSeason ?: 1,
                                onSeasonSelected = { viewModel.selectSeason(it) }
                            )
                        }
                        
                        items(episodes) { episode ->
                            EnhancedEpisodeCard(
                                episode = episode,
                                onClick = { episode.filePath?.let { onPlay(it) } }
                            )
                        }
                    }
                    
                    // 相关推荐
                    if (similar.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(title = "相关推荐")
                        }
                        
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 48.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(similar) { media ->
                                    EnhancedMediaPosterCard(
                                        item = media,
                                        isSelected = false,
                                        onFocus = {},
                                        onClick = { },
                                        modifier = Modifier.width(160.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
        
        // 返回按钮
        FilledIconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = BackgroundCard.copy(alpha = 0.9f)
            )
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = TextPrimary
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp)
    )
}

@Composable
private fun EnhancedDetailHero(
    item: MediaItem,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        // 背景图
        AsyncImage(
            model = item.backdropPath ?: item.posterPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 多层渐变
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BackgroundPrimary.copy(alpha = 0.95f),
                            BackgroundPrimary
                        )
                    )
                )
        )
        
        // 左侧光晕
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(400.dp)
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            GradientStart.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(48.dp)
        ) {
            // 媒体类型标签
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 标题
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 48.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 元信息
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.year?.let {
                    Text(text = "$it", color = TextSecondary, fontSize = 15.sp)
                    Text(text = " • ", color = TextTertiary)
                }
                
                if (item.rating > 0) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = RatingGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " ${String.format("%.1f", item.rating)} ",
                        color = RatingGold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = " • ", color = TextTertiary)
                }
                
                item.runtime?.let {
                    Text(text = "${it} 分钟", color = TextSecondary, fontSize = 15.sp)
                    Text(text = " • ", color = TextTertiary)
                }
                
                if (item.type == MediaType.TV_SHOW) {
                    item.seasonCount?.let {
                        Text(text = "$it 季", color = TextSecondary, fontSize = 15.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 操作按钮
            Row {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = GradientStart),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "播放",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(Modifier.width(16.dp))
                
                OutlinedButton(
                    onClick = { },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            colors = listOf(TextSecondary.copy(alpha = 0.5f))
                        )
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "加入片单",
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonSelector(
    seasonCount: Int,
    selectedSeason: Int,
    onSeasonSelected: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items((1..seasonCount).toList()) { num ->
            FilterChip(
                selected = selectedSeason == num,
                onClick = { onSeasonSelected(num) },
                label = { Text("第 $num 季") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GradientStart,
                    selectedLabelColor = Color.White,
                    containerColor = BackgroundCard,
                    labelColor = TextSecondary
                )
            )
        }
    }
}

@Composable
private fun CastCard(member: CastMember) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        AsyncImage(
            model = member.profilePath,
            contentDescription = member.name,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(BackgroundCard),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = member.name,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        member.character?.let {
            Text(
                text = it,
                color = TextTertiary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EnhancedEpisodeCard(
    episode: Episode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 8.dp)
            .focusable()
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundCard)
            .border(
                width = 1.dp,
                color = BackgroundElevated,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 剧集缩略图
            AsyncImage(
                model = episode.stillPath,
                contentDescription = episode.title,
                modifier = Modifier
                    .width(200.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BackgroundElevated),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${episode.episodeNumber}. ${episode.title}",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                episode.airDate?.let {
                    Text(text = it, color = TextTertiary, fontSize = 12.sp)
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                episode.overview?.let { overview ->
                    Text(
                        text = overview.take(120) + if (overview.length > 120) "..." else "",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = GradientStart),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("播放", fontSize = 14.sp)
            }
        }
    }
}
