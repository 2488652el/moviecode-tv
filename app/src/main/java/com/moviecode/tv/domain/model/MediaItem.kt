package com.moviecode.tv.domain.model

data class MediaItem(
    val id: Long = 0,
    val title: String,
    val originalTitle: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val type: MediaType,
    val year: Int? = null,
    val rating: Float = 0f,
    val voteCount: Int = 0,
    val tmdbId: Int? = null,
    val imdbId: String? = null,
    val genres: List<String> = emptyList(),
    val runtime: Int? = null,
    val status: String? = null,
    val filePath: String? = null,
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val lastModified: Long = System.currentTimeMillis()
)

enum class MediaType {
    MOVIE,
    TV_SHOW,
    ANIME
}

data class Episode(
    val id: Long = 0,
    val tvShowId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val overview: String? = null,
    val stillPath: String? = null,
    val airDate: String? = null,
    val runtime: Int? = null,
    val filePath: String? = null
)

data class Season(
    val id: Long = 0,
    val tvShowId: Long,
    val seasonNumber: Int,
    val name: String,
    val overview: String? = null,
    val posterPath: String? = null,
    val episodeCount: Int = 0
)

data class CastMember(
    val id: Long = 0,
    val name: String,
    val character: String? = null,
    val profilePath: String? = null,
    val order: Int = 0
)
