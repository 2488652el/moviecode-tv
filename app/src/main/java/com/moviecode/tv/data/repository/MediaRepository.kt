package com.moviecode.tv.data.repository

import com.moviecode.tv.data.api.TmdbApi
import com.moviecode.tv.data.api.model.*
import com.moviecode.tv.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val tmdbApi: TmdbApi
) {
    companion object {
        const val API_KEY = "399994aec1d62a35d2047e22ea74176d"
    }

    suspend fun searchMedia(query: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.searchMulti(API_KEY, query)
            val items = response.results
                .filter { it.mediaType == "movie" || it.mediaType == "tv" }
                .mapNotNull { result ->
                    when (result.mediaType) {
                        "movie" -> result.toMovieMediaItem()
                        "tv" -> result.toTvMediaItem()
                        else -> null
                    }
                }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularMovies(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getPopularMovies(API_KEY, page = page)
            val items = response.results.map { it.toMediaItem(MediaType.MOVIE) }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTopRatedMovies(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getTopRatedMovies(API_KEY, page = page)
            val items = response.results.map { it.toMediaItem(MediaType.MOVIE) }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularTvShows(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getPopularTvShows(API_KEY, page = page)
            val items = response.results.map { it.toMediaItem(MediaType.TV_SHOW) }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTopRatedTvShows(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getTopRatedTvShows(API_KEY, page = page)
            val items = response.results.map { it.toMediaItem(MediaType.TV_SHOW) }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieDetails(tmdbId: Int): Result<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val details = tmdbApi.getMovieDetails(tmdbId, API_KEY)
            val year = details.releaseDate?.take(4)?.toIntOrNull()
            val genres = details.genres?.map { it.name } ?: emptyList()
            Result.success(
                MediaItem(
                    title = details.title,
                    originalTitle = details.originalTitle,
                    overview = details.overview,
                    posterPath = details.posterPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.POSTER_SIZE + it },
                    backdropPath = details.backdropPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.BACKDROP_SIZE + it },
                    type = MediaType.MOVIE,
                    year = year,
                    rating = details.voteAverage,
                    voteCount = details.voteCount,
                    tmdbId = details.id,
                    genres = genres,
                    runtime = details.runtime,
                    status = details.status
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTvDetails(tmdbId: Int): Result<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val details = tmdbApi.getTvDetails(tmdbId, API_KEY)
            val year = details.firstAirDate?.take(4)?.toIntOrNull()
            val genres = details.genres?.map { it.name } ?: emptyList()
            Result.success(
                MediaItem(
                    title = details.name,
                    originalTitle = details.originalName,
                    overview = details.overview,
                    posterPath = details.posterPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.POSTER_SIZE + it },
                    backdropPath = details.backdropPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.BACKDROP_SIZE + it },
                    type = MediaType.TV_SHOW,
                    year = year,
                    rating = details.voteAverage,
                    voteCount = details.voteCount,
                    tmdbId = details.id,
                    genres = genres,
                    status = details.status,
                    seasonCount = details.numberOfSeasons,
                    episodeCount = details.numberOfEpisodes
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieCredits(tmdbId: Int): Result<List<CastMember>> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getMovieCredits(tmdbId, API_KEY)
            val cast = response.cast?.take(20)?.mapIndexed { index, actor ->
                CastMember(
                    name = actor.name,
                    character = actor.character,
                    profilePath = actor.profilePath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.PROFILE_SIZE + it },
                    order = actor.order ?: index
                )
            } ?: emptyList()
            Result.success(cast)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTvCredits(tmdbId: Int): Result<List<CastMember>> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getTvCredits(tmdbId, API_KEY)
            val cast = response.cast?.take(20)?.mapIndexed { index, actor ->
                CastMember(
                    name = actor.name,
                    character = actor.character,
                    profilePath = actor.profilePath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.PROFILE_SIZE + it },
                    order = actor.order ?: index
                )
            } ?: emptyList()
            Result.success(cast)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTvSeason(tvId: Int, seasonNumber: Int): Result<Season> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getTvSeason(tvId, seasonNumber, API_KEY)
            Result.success(
                Season(
                    name = response.name,
                    overview = response.overview,
                    posterPath = response.posterPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.POSTER_SIZE + it },
                    seasonNumber = response.seasonNumber,
                    episodeCount = response.episodes?.size ?: 0
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTvEpisodes(tvId: Int, seasonNumber: Int): Result<List<Episode>> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getTvSeason(tvId, seasonNumber, API_KEY)
            val episodes = response.episodes?.map { ep ->
                Episode(
                    tvShowId = tvId,
                    seasonNumber = ep.seasonNumber,
                    episodeNumber = ep.episodeNumber,
                    title = ep.name,
                    overview = ep.overview,
                    stillPath = ep.stillPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.STILL_SIZE + it },
                    airDate = ep.airDate,
                    runtime = ep.runtime
                )
            } ?: emptyList()
            Result.success(episodes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSimilarMovies(tmdbId: Int): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getSimilarMovies(tmdbId, API_KEY)
            val items = response.results.map { it.toMediaItem(MediaType.MOVIE) }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSimilarTvShows(tmdbId: Int): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbApi.getSimilarTvShows(tmdbId, API_KEY)
            val items = response.results.map { it.toMediaItem(MediaType.TV_SHOW) }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Extension functions for mapping
    private fun TmdbMovie.toMediaItem(type: MediaType) = MediaItem(
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        posterPath = posterPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.POSTER_SIZE + it },
        backdropPath = backdropPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.BACKDROP_SIZE + it },
        type = type,
        year = releaseDate?.take(4)?.toIntOrNull(),
        rating = voteAverage,
        voteCount = voteCount,
        tmdbId = id
    )

    private fun TmdbTv.toMediaItem(type: MediaType) = MediaItem(
        title = name,
        originalTitle = originalName,
        overview = overview,
        posterPath = posterPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.POSTER_SIZE + it },
        backdropPath = backdropPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.BACKDROP_SIZE + it },
        type = type,
        year = firstAirDate?.take(4)?.toIntOrNull(),
        rating = voteAverage,
        voteCount = voteCount,
        tmdbId = id,
        seasonCount = numberOfSeasons,
        episodeCount = numberOfEpisodes
    )

    private fun TmdbSearchResult.toMovieMediaItem(): MediaItem? {
        if (title.isNullOrBlank()) return null
        return MediaItem(
            title = title,
            originalTitle = originalTitle,
            overview = overview,
            posterPath = posterPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.POSTER_SIZE + it },
            backdropPath = backdropPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.BACKDROP_SIZE + it },
            type = MediaType.MOVIE,
            year = releaseDate?.take(4)?.toIntOrNull(),
            rating = voteAverage ?: 0f,
            voteCount = voteCount ?: 0,
            tmdbId = id
        )
    }

    private fun TmdbSearchResult.toTvMediaItem(): MediaItem? {
        if (name.isNullOrBlank()) return null
        return MediaItem(
            title = name,
            originalTitle = originalName,
            overview = overview,
            posterPath = posterPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.POSTER_SIZE + it },
            backdropPath = backdropPath?.let { TmdbApi.IMAGE_BASE_URL + TmdbApi.BACKDROP_SIZE + it },
            type = MediaType.TV_SHOW,
            year = firstAirDate?.take(4)?.toIntOrNull(),
            rating = voteAverage ?: 0f,
            voteCount = voteCount ?: 0,
            tmdbId = id
        )
    }
}
