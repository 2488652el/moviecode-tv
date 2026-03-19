package com.moviecode.tv.data.api

import com.moviecode.tv.data.api.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        const val POSTER_SIZE = "w500"
        const val BACKDROP_SIZE = "w1280"
        const val PROFILE_SIZE = "w185"
        const val STILL_SIZE = "w300"
    }

    // Search
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "zh-CN",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    // Movies
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN",
        @Query("page") page: Int = 1
    ): TmdbMovieListResponse

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN",
        @Query("page") page: Int = 1
    ): TmdbMovieListResponse

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN",
        @Query("page") page: Int = 1
    ): TmdbMovieListResponse

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN",
        @Query("page") page: Int = 1
    ): TmdbMovieListResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN"
    ): TmdbMovieDetails

    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbCreditsResponse

    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN",
        @Query("page") page: Int = 1
    ): TmdbMovieListResponse

    // TV Shows
    @GET("tv/popular")
    suspend fun getPopularTvShows(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN",
        @Query("page") page: Int = 1
    ): TmdbTvListResponse

    @GET("tv/top_rated")
    suspend fun getTopRatedTvShows(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN",
        @Query("page") page: Int = 1
    ): TmdbTvListResponse

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN"
    ): TmdbTvDetails

    @GET("tv/{tv_id}/credits")
    suspend fun getTvCredits(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): TmdbCreditsResponse

    @GET("tv/{tv_id}/similar")
    suspend fun getSimilarTvShows(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN",
        @Query("page") page: Int = 1
    ): TmdbTvListResponse

    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getTvSeason(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN"
    ): TmdbSeason

    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}")
    suspend fun getTvEpisode(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Path("episode_number") episodeNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN"
    ): TmdbEpisode

    // Discover
    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): TmdbMovieListResponse

    @GET("discover/tv")
    suspend fun discoverTvShows(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "zh-CN",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): TmdbTvListResponse
}
