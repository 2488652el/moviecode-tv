package com.moviecode.tv.data.repository

import android.content.Context
import com.moviecode.tv.data.local.AppDatabase
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.domain.model.MediaType
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/**
 * 推荐条目数据类
 */
data class RecommendationItem(
    val item: MediaItem,
    val score: Float,
    val reason: String
)

/**
 * 用户偏好数据类
 */
data class UserPreferences(
    val movieRatio: Float = 0f,
    val tvRatio: Float = 0f,
    val animeRatio: Float = 0f,
    val totalWatched: Int = 0,
    val preferredTypes: List<MediaType> = emptyList()
)

/**
 * 推荐服务仓库
 * 基于观看历史和内容相似度提供个性化推荐
 */
class RecommendationRepository(private val context: Context) {
    
    private val historyRepository = HistoryRepository.getInstance(context)
    private val database = AppDatabase.getInstance(context)
    
    companion object {
        @Volatile
        private var INSTANCE: RecommendationRepository? = null
        
        fun getInstance(context: Context): RecommendationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecommendationRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 获取个性化推荐
     */
    suspend fun getRecommendations(items: List<MediaItem>, limit: Int = 10): List<RecommendationItem> {
        val history = historyRepository.getHistoryFlow().first()
        
        if (history.isEmpty()) {
            return getPopularItems(items, limit)
        }

        val recommendations = mutableListOf<RecommendationItem>()
        val watchedIds = history.map { it.mediaId }.toSet()
        val preferences = analyzePreferences(history)

        for (item in items) {
            if (watchedIds.contains(item.id)) continue
            
            val score = calculateScore(item, preferences)
            
            if (score > 0) {
                recommendations.add(
                    RecommendationItem(
                        item = item,
                        score = score,
                        reason = getRecommendationReason(item, preferences)
                    )
                )
            }
        }

        return recommendations
            .sortedByDescending { it.score }
            .take(limit)
    }
    
    /**
     * 获取相似内容推荐
     */
    suspend fun getSimilarItems(item: MediaItem, items: List<MediaItem>, limit: Int = 6): List<RecommendationItem> {
        val recommendations = mutableListOf<RecommendationItem>()
        
        for (other in items) {
            if (other.id == item.id) continue
            
            val similarity = calculateSimilarity(item, other)
            
            if (similarity > 0.3f) {
                recommendations.add(
                    RecommendationItem(
                        item = other,
                        score = similarity,
                        reason = "与《${item.title}》相似"
                    )
                )
            }
        }

        return recommendations
            .sortedByDescending { it.score }
            .take(limit)
    }
    
    /**
     * 获取热门推荐
     */
    fun getPopularItems(items: List<MediaItem>, limit: Int = 10): List<RecommendationItem> {
        return items
            .filter { (it.rating ?: 0f) > 7.0f }
            .sortedByDescending { it.rating }
            .take(limit)
            .map { item ->
                RecommendationItem(
                    item = item,
                    score = item.rating ?: 0f,
                    reason = "热门推荐"
                )
            }
    }
    
    /**
     * 获取最新上线推荐
     */
    fun getNewReleases(items: List<MediaItem>, limit: Int = 10): List<RecommendationItem> {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        
        return items
            .sortedByDescending { it.year ?: 0 }
            .take(limit)
            .map { item ->
                val yearDiff = currentYear - (item.year ?: currentYear)
                RecommendationItem(
                    item = item,
                    score = (100 - yearDiff).toFloat(),
                    reason = if (yearDiff < 1) "最新上线" else "${item.year}年"
                )
            }
    }
    
    /**
     * 分析用户偏好
     */
    private fun analyzePreferences(history: List<PlayHistoryEntry>): UserPreferences {
        val typeCount = mutableMapOf<MediaType, Int>()
        
        for (entry in history) {
            if (entry.progress > 0) {
                val type = when (entry.type) {
                    "movie" -> MediaType.MOVIE
                    "tv" -> MediaType.TV_SHOW
                    else -> MediaType.ANIME
                }
                typeCount[type] = (typeCount[type] ?: 0) + 1
            }
        }

        val total = typeCount.values.sum().toFloat()
        if (total == 0f) return UserPreferences()

        val movieRatio = (typeCount[MediaType.MOVIE] ?: 0) / total
        val tvRatio = (typeCount[MediaType.TV_SHOW] ?: 0) / total
        val animeRatio = (typeCount[MediaType.ANIME] ?: 0) / total

        val preferredTypes = typeCount.entries
            .sortedByDescending { it.value }
            .take(2)
            .map { it.key }

        return UserPreferences(
            movieRatio = movieRatio,
            tvRatio = tvRatio,
            animeRatio = animeRatio,
            totalWatched = history.size,
            preferredTypes = preferredTypes
        )
    }
    
    /**
     * 计算推荐分数
     */
    private fun calculateScore(item: MediaItem, preferences: UserPreferences): Float {
        var score = 50f

        // 类型匹配分数
        val typeRatio = when (item.type) {
            MediaType.MOVIE -> preferences.movieRatio
            MediaType.TV_SHOW -> preferences.tvRatio
            MediaType.ANIME -> preferences.animeRatio
        }
        score += typeRatio * 30f

        // 评分分数
        item.rating?.let { rating ->
            score += (rating / 10f) * 20f
        }

        // 热度分数
        if ((item.rating ?: 0f) > 7.5f && preferences.totalWatched > 5) {
            score += 10f
        }

        // 新鲜度分数
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val yearDiff = currentYear - (item.year ?: currentYear)
        when {
            yearDiff < 1 -> score += 15f
            yearDiff < 3 -> score += 10f
            yearDiff < 5 -> score += 5f
        }

        // 随机性
        score += Random.nextFloat() * 5f

        return score
    }
    
    /**
     * 计算内容相似度
     */
    private fun calculateSimilarity(item1: MediaItem, item2: MediaItem): Float {
        var similarity = 0f
        var total = 0f

        // 类型相同
        if (item1.type == item2.type) {
            similarity += 0.4f
        }
        total += 0.4f

        // 年份相近
        val yearDiff = abs((item1.year ?: 0) - (item2.year ?: 0))
        if (yearDiff < 3) {
            similarity += 0.2f * (1 - yearDiff / 3f)
        }
        total += 0.2f

        // 评分相近
        val ratingDiff = abs((item1.rating ?: 5f) - (item2.rating ?: 5f))
        if (ratingDiff < 1) {
            similarity += 0.2f * (1 - ratingDiff)
        }
        total += 0.2f

        // 类型标签匹配
        val genres1 = item1.genres.map { it.lowercase() }
        val genres2 = item2.genres.map { it.lowercase() }
        val commonGenres = genres1.intersect(genres2.toSet())
        if (commonGenres.isNotEmpty()) {
            val maxLen = max(genres1.size, genres2.size)
            similarity += 0.2f * (commonGenres.size.toFloat() / maxLen)
        }
        total += 0.2f

        return if (total > 0) similarity / total else 0f
    }
    
    /**
     * 获取推荐原因
     */
    private fun getRecommendationReason(item: MediaItem, preferences: UserPreferences): String {
        val reasons = mutableListOf<String>()

        // 类型偏好
        if (preferences.preferredTypes.contains(item.type)) {
            reasons.add(
                when (item.type) {
                    MediaType.MOVIE -> "您喜欢电影"
                    MediaType.TV_SHOW -> "您喜欢剧集"
                    MediaType.ANIME -> "您喜欢动漫"
                }
            )
        }

        // 高评分
        item.rating?.let { rating ->
            if (rating >= 8.0f) {
                reasons.add("高评分佳作")
            }
        }

        // 新片
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        if (item.year != null && item.year >= currentYear - 1) {
            reasons.add("最新上线")
        }

        // 类型标签
        if (item.genres.isNotEmpty()) {
            reasons.add(item.genres.first())
        }

        return reasons.firstOrNull() ?: "为您推荐"
    }
}
