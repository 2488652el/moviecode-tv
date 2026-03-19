package com.moviecode.tv.data.api.model

import com.google.gson.annotations.SerializedName

// Base responses
data class TmdbSearchResponse(
    @SerializedName("page") val page: Int,
    @SerializedName("results") val results: List<TmdbSearchResult>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class TmdbSearchResult(
    @SerializedName("id") val id: Int,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Float?,
    @SerializedName("vote_count") val voteCount: Int?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("genre_ids") val genreIds: List<Int>?
)

// Movie responses
data class TmdbMovieListResponse(
    @SerializedName("page") val page: Int,
    @SerializedName("results") val results: List<TmdbMovie>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class TmdbMovie(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Float,
    @SerializedName("vote_count") val voteCount: Int,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("genre_ids") val genreIds: List<Int>?,
    @SerializedName("popularity") val popularity: Float?,
    @SerializedName("adult") val adult: Boolean?
)

data class TmdbMovieDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Float,
    @SerializedName("vote_count") val voteCount: Int,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("genres") val genres: List<TmdbGenre>?,
    @SerializedName("runtime") val runtime: Int?,
    @SerializedName("status") val status: String?,
    @SerializedName("tagline") val tagline: String?,
    @SerializedName("budget") val budget: Long?,
    @SerializedName("revenue") val revenue: Long?,
    @SerializedName("imdb_id") val imdbId: String?
)

// TV responses
data class TmdbTvListResponse(
    @SerializedName("page") val page: Int,
    @SerializedName("results") val results: List<TmdbTv>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class TmdbTv(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Float,
    @SerializedName("vote_count") val voteCount: Int,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("genre_ids") val genreIds: List<Int>?,
    @SerializedName("popularity") val popularity: Float?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int?
)

data class TmdbTvDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Float,
    @SerializedName("vote_count") val voteCount: Int,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("genres") val genres: List<TmdbGenre>?,
    @SerializedName("status") val status: String?,
    @SerializedName("tagline") val tagline: String?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int?,
    @SerializedName("seasons") val seasons: List<TmdbSeasonInfo>?
)

data class TmdbSeasonInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("air_date") val airDate: String?
)

// Credits
data class TmdbCreditsResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("cast") val cast: List<TmdbCast>?,
    @SerializedName("crew") val crew: List<TmdbCrew>?
)

data class TmdbCast(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("character") val character: String?,
    @SerializedName("profile_path") val profilePath: String?,
    @SerializedName("order") val order: Int?
)

data class TmdbCrew(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("job") val job: String?,
    @SerializedName("department") val department: String?,
    @SerializedName("profile_path") val profilePath: String?
)

// Season & Episode
data class TmdbSeason(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("episodes") val episodes: List<TmdbEpisode>?
)

data class TmdbEpisode(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("overview") val overview: String?,
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("runtime") val runtime: Int?,
    @SerializedName("vote_average") val voteAverage: Float?,
    @SerializedName("vote_count") val voteCount: Int?
)

// Common
data class TmdbGenre(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)
