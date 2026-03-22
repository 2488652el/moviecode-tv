package com.moviecode.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.moviecode.tv.data.api.TmdbApi
import com.moviecode.tv.data.api.model.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tmdbCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "tmdb_cache")

@Singleton
class TmdbCacheRepository @Inject constructor(
    private val context: Context,
    private val api: TmdbApi,
    private val apiKey: String
) {
    private val gson = Gson()
    
    companion object {
        private const val CACHE_DURATION_MS = 7 * 24 * 60 * 60 * 1000L
        private fun timestampKey(key: String) = longPreferencesKey("ts_$key")
        private fun cacheKey(key: String) = stringPreferencesKey("cache_$key")
    }
    
    private val dataStore = context.tmdbCacheDataStore
    
    private suspend fun isCacheValid(key: String): Boolean {
        val timestamp = dataStore.data.first()[timestampKey(key)] ?: return false
        return System.currentTimeMillis() - timestamp < CACHE_DURATION_MS
    }
    
    private suspend inline fun <reified T> saveCache(key: String, data: T) {
        val json = gson.toJson(data)
        dataStore.edit { preferences ->
            preferences[cacheKey(key)] = json
            preferences[timestampKey(key)] = System.currentTimeMillis()
        }
    }
    
    private suspend inline fun <reified T> getCache(key: String): T? {
        val json: String = dataStore.data.first()[cacheKey(key)] ?: return null
        return try {
            gson.fromJson(json, object : TypeToken<T>() {}.type)
        } catch (e: Exception) {
            null
        }
    }
    
    // 搜索电影
    suspend fun searchMovies(query: String): List<TmdbSearchResult> {
        val key = "search_movie_$query"
        if (isCacheValid(key)) {
            getCache<List<TmdbSearchResult>>(key)?.let { return it }
        }
        return try {
            val response = api.searchMovies(apiKey, query)
            saveCache(key, response.results)
            response.results
        } catch (e: Exception) {
            getCache<List<TmdbSearchResult>>(key) ?: emptyList()
        }
    }
    
    // 搜索电视剧
    suspend fun searchTVShows(query: String): List<TmdbSearchResult> {
        val key = "search_tv_$query"
        if (isCacheValid(key)) {
            getCache<List<TmdbSearchResult>>(key)?.let { return it }
        }
        return try {
            val response = api.searchMulti(apiKey, query)
            saveCache(key, response.results)
            response.results
        } catch (e: Exception) {
            getCache<List<TmdbSearchResult>>(key) ?: emptyList()
        }
    }
    
    // 电影详情
    suspend fun getMovieDetails(movieId: Int): TmdbMovieDetails? {
        val key = "movie_details_$movieId"
        if (isCacheValid(key)) {
            getCache<TmdbMovieDetails>(key)?.let { return it }
        }
        return try {
            val details = api.getMovieDetails(movieId, apiKey)
            saveCache(key, details)
            details
        } catch (e: Exception) {
            getCache<TmdbMovieDetails>(key)
        }
    }
    
    // 电视剧详情
    suspend fun getTvDetails(tvId: Int): TmdbTvDetails? {
        val key = "tv_details_$tvId"
        if (isCacheValid(key)) {
            getCache<TmdbTvDetails>(key)?.let { return it }
        }
        return try {
            val details = api.getTvDetails(tvId, apiKey)
            saveCache(key, details)
            details
        } catch (e: Exception) {
            getCache<TmdbTvDetails>(key)
        }
    }
    
    // 电影演员
    suspend fun getMovieCredits(movieId: Int): TmdbCreditsResponse? {
        val key = "credits_movie_$movieId"
        if (isCacheValid(key)) {
            getCache<TmdbCreditsResponse>(key)?.let { return it }
        }
        return try {
            val credits = api.getMovieCredits(movieId, apiKey)
            saveCache(key, credits)
            credits
        } catch (e: Exception) {
            getCache<TmdbCreditsResponse>(key)
        }
    }
    
    // 电视剧演员
    suspend fun getTvCredits(tvId: Int): TmdbCreditsResponse? {
        val key = "credits_tv_$tvId"
        if (isCacheValid(key)) {
            getCache<TmdbCreditsResponse>(key)?.let { return it }
        }
        return try {
            val credits = api.getTvCredits(tvId, apiKey)
            saveCache(key, credits)
            credits
        } catch (e: Exception) {
            getCache<TmdbCreditsResponse>(key)
        }
    }
    
    // 热门电影
    suspend fun getPopularMovies(page: Int = 1): List<TmdbSearchResult> {
        val key = "popular_movies_$page"
        if (isCacheValid(key)) {
            getCache<List<TmdbSearchResult>>(key)?.let { return it }
        }
        return try {
            val response = api.getPopularMovies(apiKey, page = page)
            saveCache(key, response.results)
            response.results
        } catch (e: Exception) {
            getCache<List<TmdbSearchResult>>(key) ?: emptyList()
        }
    }
    
    // 评分最高电影
    suspend fun getTopRatedMovies(page: Int = 1): List<TmdbSearchResult> {
        val key = "toprated_movies_$page"
        if (isCacheValid(key)) {
            getCache<List<TmdbSearchResult>>(key)?.let { return it }
        }
        return try {
            val response = api.getTopRatedMovies(apiKey, page = page)
            saveCache(key, response.results)
            response.results
        } catch (e: Exception) {
            getCache<List<TmdbSearchResult>>(key) ?: emptyList()
        }
    }
    
    // 热门电视剧
    suspend fun getPopularTVShows(page: Int = 1): List<TmdbSearchResult> {
        val key = "popular_tv_$page"
        if (isCacheValid(key)) {
            getCache<List<TmdbSearchResult>>(key)?.let { return it }
        }
        return try {
            val response = api.getPopularTvShows(apiKey, page = page)
            saveCache(key, response.results)
            response.results
        } catch (e: Exception) {
            getCache<List<TmdbSearchResult>>(key) ?: emptyList()
        }
    }
    
    // 评分最高电视剧
    suspend fun getTopRatedTVShows(page: Int = 1): List<TmdbSearchResult> {
        val key = "toprated_tv_$page"
        if (isCacheValid(key)) {
            getCache<List<TmdbSearchResult>>(key)?.let { return it }
        }
        return try {
            val response = api.getTopRatedTvShows(apiKey, page = page)
            saveCache(key, response.results)
            response.results
        } catch (e: Exception) {
            getCache<List<TmdbSearchResult>>(key) ?: emptyList()
        }
    }
    
    // 相似电影
    suspend fun getSimilarMovies(movieId: Int, page: Int = 1): List<TmdbSearchResult> {
        val key = "similar_movies_${movieId}_$page"
        if (isCacheValid(key)) {
            getCache<List<TmdbSearchResult>>(key)?.let { return it }
        }
        return try {
            val response = api.getSimilarMovies(movieId, apiKey, page = page)
            saveCache(key, response.results)
            response.results
        } catch (e: Exception) {
            getCache<List<TmdbSearchResult>>(key) ?: emptyList()
        }
    }
    
    // 相似电视剧
    suspend fun getSimilarTVShows(tvId: Int, page: Int = 1): List<TmdbSearchResult> {
        val key = "similar_tv_${tvId}_$page"
        if (isCacheValid(key)) {
            getCache<List<TmdbSearchResult>>(key)?.let { return it }
        }
        return try {
            val response = api.getSimilarTvShows(tvId, apiKey, page = page)
            saveCache(key, response.results)
            response.results
        } catch (e: Exception) {
            getCache<List<TmdbSearchResult>>(key) ?: emptyList()
        }
    }
    
    // 工具方法
    fun getProfileUrl(path: String?): String? = path?.let { "${TmdbApi.IMAGE_BASE_URL}/${TmdbApi.PROFILE_SIZE}$it" }
    fun getPosterUrl(path: String?): String? = path?.let { "${TmdbApi.IMAGE_BASE_URL}/${TmdbApi.POSTER_SIZE}$it" }
    fun getBackdropUrl(path: String?): String? = path?.let { "${TmdbApi.IMAGE_BASE_URL}/${TmdbApi.BACKDROP_SIZE}$it" }
    fun getStillUrl(path: String?): String? = path?.let { "${TmdbApi.IMAGE_BASE_URL}/${TmdbApi.STILL_SIZE}$it" }
    
    suspend fun clearCache() { dataStore.edit { it.clear() } }
}
