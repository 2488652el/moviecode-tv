package com.moviecode.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.moviecode.tv.data.local.AppDatabase
import com.moviecode.tv.data.local.PlaybackProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "history_store")

/**
 * 播放历史记录数据类
 */
data class PlayHistoryEntry(
    val mediaId: String,
    val title: String,
    val type: String,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val currentTime: Double,
    val duration: Double,
    val progress: Int,
    val lastWatched: Long,
    val episodeNumber: Int? = null,
    val seasonNumber: Int? = null
)

/**
 * 历史记录仓库
 * 管理播放历史，支持筛选、排序、最近50条等功能
 */
class HistoryRepository(private val context: Context) {
    
    private val gson = Gson()
    private val database = AppDatabase.getInstance(context)
    private val playbackDao = database.playbackProgressDao()
    
    companion object {
        private val HISTORY_KEY = stringPreferencesKey("play_history")
        private const val MAX_HISTORY = 50
        
        @Volatile
        private var INSTANCE: HistoryRepository? = null
        
        fun getInstance(context: Context): HistoryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HistoryRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 获取历史记录Flow
     */
    fun getHistoryFlow(): Flow<List<PlayHistoryEntry>> {
        return context.historyDataStore.data.map { preferences ->
            val json = preferences[HISTORY_KEY] ?: "[]"
            try {
                val type = object : TypeToken<List<PlayHistoryEntry>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * 获取最近的历史记录
     */
    suspend fun getRecentHistory(limit: Int = 10): List<PlayHistoryEntry> {
        return getHistoryFlow().first().take(limit)
    }
    
    /**
     * 获取按类型筛选的历史记录
     */
    suspend fun getHistoryByType(type: String): List<PlayHistoryEntry> {
        return getHistoryFlow().first().filter { it.type == type }
    }
    
    /**
     * 添加或更新历史记录
     */
    suspend fun addOrUpdateEntry(
        mediaId: String,
        title: String,
        type: String,
        posterPath: String? = null,
        backdropPath: String? = null,
        currentTime: Double,
        duration: Double,
        episodeNumber: Int? = null,
        seasonNumber: Int? = null
    ) {
        val currentList = getHistoryFlow().first().toMutableList()
        val progress = if (duration > 0) ((currentTime / duration) * 100).toInt() else 0
        
        // 移除已存在的相同记录
        currentList.removeAll { it.mediaId == mediaId }
        
        // 添加到列表开头
        currentList.add(0, PlayHistoryEntry(
            mediaId = mediaId,
            title = title,
            type = type,
            posterPath = posterPath,
            backdropPath = backdropPath,
            currentTime = currentTime,
            duration = duration,
            progress = progress,
            lastWatched = System.currentTimeMillis(),
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber
        ))
        
        // 限制最大数量
        val trimmedList = currentList.take(MAX_HISTORY)
        
        // 保存
        saveHistory(trimmedList)
    }
    
    /**
     * 删除指定历史记录
     */
    suspend fun deleteEntry(mediaId: String) {
        val currentList = getHistoryFlow().first().toMutableList()
        currentList.removeAll { it.mediaId == mediaId }
        saveHistory(currentList)
    }
    
    /**
     * 清除所有历史记录
     */
    suspend fun clearHistory() {
        saveHistory(emptyList())
    }
    
    /**
     * 获取指定记录
     */
    suspend fun getEntry(mediaId: String): PlayHistoryEntry? {
        return getHistoryFlow().first().find { it.mediaId == mediaId }
    }
    
    /**
     * 保存历史记录
     */
    private suspend fun saveHistory(list: List<PlayHistoryEntry>) {
        context.historyDataStore.edit { preferences ->
            preferences[HISTORY_KEY] = gson.toJson(list)
        }
    }
    
    /**
     * 获取历史记录数量
     */
    suspend fun getHistoryCount(): Int {
        return getHistoryFlow().first().size
    }
    
    /**
     * 获取按进度排序的历史记录
     */
    suspend fun getHistorySortedByProgress(): List<PlayHistoryEntry> {
        return getHistoryFlow().first().sortedByDescending { it.progress }
    }
}

/**
 * 主题模式
 */
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

/**
 * 主题设置数据类
 */
data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.DARK,
    val autoDarkStart: String = "22:00",
    val autoDarkEnd: String = "06:00"
)

/**
 * 主题设置仓库
 */
class ThemeRepository(private val context: Context) {
    
    private val dataStore = context.historyDataStore
    
    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val AUTO_DARK_START_KEY = stringPreferencesKey("auto_dark_start")
        private val AUTO_DARK_END_KEY = stringPreferencesKey("auto_dark_end")
        
        @Volatile
        private var INSTANCE: ThemeRepository? = null
        
        fun getInstance(context: Context): ThemeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 获取主题设置Flow
     */
    fun getThemeSettingsFlow(): Flow<ThemeSettings> {
        return dataStore.data.map { preferences ->
            ThemeSettings(
                mode = ThemeMode.valueOf(preferences[THEME_MODE_KEY] ?: ThemeMode.DARK.name),
                autoDarkStart = preferences[AUTO_DARK_START_KEY] ?: "22:00",
                autoDarkEnd = preferences[AUTO_DARK_END_KEY] ?: "06:00"
            )
        }
    }
    
    /**
     * 设置主题模式
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
    
    /**
     * 设置自动暗黑时间
     */
    suspend fun setAutoDarkTime(start: String, end: String) {
        dataStore.edit { preferences ->
            preferences[AUTO_DARK_START_KEY] = start
            preferences[AUTO_DARK_END_KEY] = end
        }
    }
    
    /**
     * 判断当前是否为深色模式（考虑自动时间）
     */
    suspend fun isDarkMode(): Boolean {
        val settings = getThemeSettingsFlow().first()
        return when (settings.mode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> {
                val now = java.util.Calendar.getInstance()
                val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMinute = now.get(java.util.Calendar.MINUTE)
                val currentTime = String.format("%02d:%02d", currentHour, currentMinute)
                
                val startHour = settings.autoDarkStart.substringBefore(":").toIntOrNull() ?: 22
                val endHour = settings.autoDarkEnd.substringBefore(":").toIntOrNull() ?: 6
                
                currentTime >= settings.autoDarkStart || currentTime < settings.autoDarkEnd
            }
        }
    }
}
