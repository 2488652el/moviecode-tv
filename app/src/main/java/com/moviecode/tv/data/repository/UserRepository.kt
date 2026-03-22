package com.moviecode.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.moviecode.tv.domain.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_data")

/**
 * 用户配置
 */
data class User(
    val id: String,
    val name: String,
    val avatar: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 用户设置
 */
data class UserSettings(
    val theme: String = "dark",
    val defaultMediaType: String = "all",
    val autoPlay: Boolean = true,
    val parentalControlEnabled: Boolean = false,
    val parentalControlPin: String? = null
)

/**
 * 收藏项
 */
data class FavoriteItem(
    val id: String,
    val userId: String,
    val mediaItem: MediaItem,
    val addedAt: Long = System.currentTimeMillis(),
    val note: String? = null
)

/**
 * 播放列表
 */
data class Playlist(
    val id: String,
    val userId: String,
    val name: String,
    val description: String? = null,
    val items: List<MediaItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 观看记录
 */
data class WatchRecord(
    val id: String,
    val userId: String,
    val mediaItem: MediaItem,
    val progress: Int = 0, // 0-100
    val duration: Long = 0,
    val watchedDuration: Long = 0,
    val completedAt: Long? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val lastWatchedAt: Long = System.currentTimeMillis()
)

/**
 * 用户仓库 - 管理多用户、收藏夹、播放列表、观看统计
 */
@Singleton
class UserRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.userDataStore

    companion object {
        // Keys
        private val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        private val USERS_JSON = stringPreferencesKey("users_json")
        private val FAVORITES_JSON = stringPreferencesKey("favorites_json")
        private val PLAYLISTS_JSON = stringPreferencesKey("playlists_json")
        private val WATCH_RECORDS_JSON = stringPreferencesKey("watch_records_json")
    }

    // ==================== 用户管理 ====================

    val currentUserId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[CURRENT_USER_ID] ?: "default"
    }

    suspend fun setCurrentUser(userId: String) {
        dataStore.edit { prefs ->
            prefs[CURRENT_USER_ID] = userId
        }
    }

    fun getUsers(): List<User> {
        // 从 DataStore 读取并解析
        return emptyList() // 简化实现
    }

    // ==================== 收藏夹 ====================

    val favorites: Flow<List<FavoriteItem>> = dataStore.data.map { prefs ->
        parseFavorites(prefs[FAVORITES_JSON])
    }

    suspend fun addFavorite(userId: String, mediaItem: MediaItem, note: String? = null) {
        dataStore.edit { prefs ->
            val current = parseFavorites(prefs[FAVORITES_JSON]).toMutableList()
            // 避免重复
            if (current.none { it.userId == userId && it.mediaItem.id == mediaItem.id }) {
                current.add(FavoriteItem(
                    id = "fav_${System.currentTimeMillis()}",
                    userId = userId,
                    mediaItem = mediaItem,
                    note = note
                ))
                prefs[FAVORITES_JSON] = serializeFavorites(current)
            }
        }
    }

    suspend fun removeFavorite(userId: String, mediaId: String) {
        dataStore.edit { prefs ->
            val current = parseFavorites(prefs[FAVORITES_JSON]).toMutableList()
            current.removeAll { it.userId == userId && it.mediaItem.id == mediaId }
            prefs[FAVORITES_JSON] = serializeFavorites(current)
        }
    }

    suspend fun getUserFavorites(userId: String): List<FavoriteItem> {
        var result = emptyList<FavoriteItem>()
        dataStore.data.collect { prefs ->
            result = parseFavorites(prefs[FAVORITES_JSON]).filter { it.userId == userId }
        }
        return result
    }

    fun isFavorite(userId: String, mediaId: String, favorites: List<FavoriteItem>): Boolean {
        return favorites.any { it.userId == userId && it.mediaItem.id == mediaId }
    }

    // ==================== 播放列表 ====================

    val playlists: Flow<List<Playlist>> = dataStore.data.map { prefs ->
        parsePlaylists(prefs[PLAYLISTS_JSON])
    }

    suspend fun createPlaylist(userId: String, name: String, description: String? = null): Playlist {
        val playlist = Playlist(
            id = "pl_${System.currentTimeMillis()}",
            userId = userId,
            name = name,
            description = description
        )
        dataStore.edit { prefs ->
            val current = parsePlaylists(prefs[PLAYLISTS_JSON]).toMutableList()
            current.add(playlist)
            prefs[PLAYLISTS_JSON] = serializePlaylists(current)
        }
        return playlist
    }

    suspend fun deletePlaylist(playlistId: String) {
        dataStore.edit { prefs ->
            val current = parsePlaylists(prefs[PLAYLISTS_JSON]).toMutableList()
            current.removeAll { it.id == playlistId }
            prefs[PLAYLISTS_JSON] = serializePlaylists(current)
        }
    }

    suspend fun addToPlaylist(playlistId: String, mediaItem: MediaItem) {
        dataStore.edit { prefs ->
            val current = parsePlaylists(prefs[PLAYLISTS_JSON]).toMutableList()
            val index = current.indexOfFirst { it.id == playlistId }
            if (index >= 0) {
                val playlist = current[index]
                if (playlist.items.none { it.id == mediaItem.id }) {
                    current[index] = playlist.copy(
                        items = playlist.items + mediaItem,
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }
            prefs[PLAYLISTS_JSON] = serializePlaylists(current)
        }
    }

    suspend fun removeFromPlaylist(playlistId: String, mediaId: String) {
        dataStore.edit { prefs ->
            val current = parsePlaylists(prefs[PLAYLISTS_JSON]).toMutableList()
            val index = current.indexOfFirst { it.id == playlistId }
            if (index >= 0) {
                val playlist = current[index]
                current[index] = playlist.copy(
                    items = playlist.items.filter { it.id != mediaId },
                    updatedAt = System.currentTimeMillis()
                )
            }
            prefs[PLAYLISTS_JSON] = serializePlaylists(current)
        }
    }

    // ==================== 观看记录 ====================

    val watchRecords: Flow<List<WatchRecord>> = dataStore.data.map { prefs ->
        parseWatchRecords(prefs[WATCH_RECORDS_JSON])
    }

    suspend fun addWatchRecord(userId: String, mediaItem: MediaItem, progress: Int, duration: Long) {
        dataStore.edit { prefs ->
            val current = parseWatchRecords(prefs[WATCH_RECORDS_JSON]).toMutableList()
            val existingIndex = current.indexOfFirst { 
                it.userId == userId && it.mediaItem.id == mediaItem.id 
            }
            
            if (existingIndex >= 0) {
                // 更新现有记录
                val existing = current[existingIndex]
                current[existingIndex] = existing.copy(
                    progress = progress,
                    watchedDuration = duration * progress / 100,
                    lastWatchedAt = System.currentTimeMillis(),
                    completedAt = if (progress >= 95) System.currentTimeMillis() else null
                )
            } else {
                // 新增记录
                current.add(WatchRecord(
                    id = "rec_${System.currentTimeMillis()}",
                    userId = userId,
                    mediaItem = mediaItem,
                    progress = progress,
                    duration = duration,
                    watchedDuration = duration * progress / 100,
                    completedAt = if (progress >= 95) System.currentTimeMillis() else null
                ))
            }
            prefs[WATCH_RECORDS_JSON] = serializeWatchRecords(current)
        }
    }

    suspend fun updateProgress(userId: String, mediaId: String, progress: Int, watchedDuration: Long) {
        dataStore.edit { prefs ->
            val current = parseWatchRecords(prefs[WATCH_RECORDS_JSON]).toMutableList()
            val index = current.indexOfFirst { 
                it.userId == userId && it.mediaItem.id == mediaId 
            }
            if (index >= 0) {
                current[index] = current[index].copy(
                    progress = progress,
                    watchedDuration = watchedDuration,
                    lastWatchedAt = System.currentTimeMillis()
                )
            }
            prefs[WATCH_RECORDS_JSON] = serializeWatchRecords(current)
        }
    }

    suspend fun markCompleted(userId: String, mediaId: String) {
        dataStore.edit { prefs ->
            val current = parseWatchRecords(prefs[WATCH_RECORDS_JSON]).toMutableList()
            val index = current.indexOfFirst { 
                it.userId == userId && it.mediaItem.id == mediaId 
            }
            if (index >= 0) {
                current[index] = current[index].copy(
                    progress = 100,
                    completedAt = System.currentTimeMillis()
                )
            }
            prefs[WATCH_RECORDS_JSON] = serializeWatchRecords(current)
        }
    }

    // ==================== 观看统计 ====================

    suspend fun getWatchStats(userId: String, days: Int = 30): WatchStats {
        var stats = WatchStats(0, 0, 0, emptyList())
        dataStore.data.collect { prefs ->
            val records = parseWatchRecords(prefs[WATCH_RECORDS_JSON])
                .filter { it.userId == userId }
            
            val cutoff = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
            val recentRecords = records.filter { it.lastWatchedAt > cutoff }
            
            stats = WatchStats(
                totalWatchTime = recentRecords.sumOf { it.watchedDuration },
                totalItems = recentRecords.size,
                completedItems = recentRecords.count { it.completedAt != null },
                dailyStats = calculateDailyStats(recentRecords, days)
            )
        }
        return stats
    }

    // ==================== 序列化/反序列化 ====================

    private fun parseFavorites(json: String?): List<FavoriteItem> {
        if (json.isNullOrEmpty()) return emptyList()
        // 简化实现，实际应使用 Gson/Kotlinx Serialization
        return emptyList()
    }

    private fun serializeFavorites(items: List<FavoriteItem>): String {
        // 简化实现
        return ""
    }

    private fun parsePlaylists(json: String?): List<Playlist> {
        if (json.isNullOrEmpty()) return emptyList()
        return emptyList()
    }

    private fun serializePlaylists(items: List<Playlist>): String {
return ""
    }

    private fun parseWatchRecords(json: String?): List<WatchRecord> {
        if (json.isNullOrEmpty()) return emptyList()
        return emptyList()
    }

    private fun serializeWatchRecords(items: List<WatchRecord>): String {
        return ""
    }

    private fun calculateDailyStats(records: List<WatchRecord>, days: Int): List<DailyStat> {
        val stats = mutableMapOf<String, DailyStat>()
        val now = System.currentTimeMillis()
        
        // 初始化
        for (i in 0 until days) {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date(now - i * 24 * 60 * 60 * 1000))
            stats[date] = DailyStat(date, 0, 0)
        }
        
        // 填充
        records.forEach { record ->
            val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date(record.lastWatchedAt))
            stats[date]?.let { stat ->
                stats[date] = stat.copy(
                    watchTime = stat.watchTime + record.watchedDuration,
                    itemsCount = stat.itemsCount + 1
                )
            }
        }
        
        return stats.values.toList().sortedBy { it.date }
    }
}

/**
 * 观看统计
 */
data class WatchStats(
    val totalWatchTime: Long,
    val totalItems: Int,
    val completedItems: Int,
    val dailyStats: List<DailyStat>
)

/**
 * 每日统计
 */
data class DailyStat(
    val date: String,
    val watchTime: Long,
    val itemsCount: Int
)
