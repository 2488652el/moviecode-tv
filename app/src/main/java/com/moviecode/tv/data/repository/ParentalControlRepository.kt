package com.moviecode.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.moviecode.tv.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.parentalDataStore: DataStore<Preferences> by preferencesDataStore(name = "parental_store")

/**
 * 家长控制仓库
 * 管理家长控制设置、PIN验证、内容过滤等功能
 */
class ParentalControlRepository(private val context: Context) {
    
    private val gson = Gson()
    
    companion object {
        private val SETTINGS_KEY = stringPreferencesKey("parental_settings")
        private val IS_UNLOCKED_KEY = booleanPreferencesKey("is_unlocked")
        private val UNLOCK_UNTIL_KEY = longPreferencesKey("unlock_until")
        
        @Volatile
        private var INSTANCE: ParentalControlRepository? = null
        
        fun getInstance(context: Context): ParentalControlRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ParentalControlRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 获取家长控制设置 Flow
     */
    fun getSettingsFlow(): Flow<ParentalSettings> {
        return context.parentalDataStore.data.map { preferences ->
            val json = preferences[SETTINGS_KEY]
            if (json != null) {
                try {
                    gson.fromJson(json, ParentalSettings::class.java)
                } catch (e: Exception) {
                    ParentalSettings()
                }
            } else {
                ParentalSettings()
            }
        }
    }
    
    /**
     * 获取当前设置
     */
    suspend fun getSettings(): ParentalSettings {
        return getSettingsFlow().first()
    }
    
    /**
     * 保存设置
     */
    suspend fun saveSettings(settings: ParentalSettings) {
        context.parentalDataStore.edit { preferences ->
            preferences[SETTINGS_KEY] = gson.toJson(settings)
        }
    }
    
    /**
     * 启用/禁用家长控制
     */
    suspend fun setEnabled(enabled: Boolean) {
        val settings = getSettings().copy(isEnabled = enabled)
        saveSettings(settings)
    }
    
    /**
     * 设置 PIN 码
     * @return true 如果设置成功，false 如果 PIN 格式不正确
     */
    suspend fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val settings = getSettings().copy(pin = pin)
        saveSettings(settings)
        return true
    }
    
    /**
     * 验证 PIN 码
     */
    suspend fun verifyPin(pin: String): Boolean {
        return getSettings().pin == pin
    }
    
    /**
     * 设置内容分级
     */
    suspend fun setContentRating(rating: ContentRating) {
        val settings = getSettings().copy(contentRating = rating)
        saveSettings(settings)
    }
    
    /**
     * 屏蔽题材
     */
    suspend fun blockGenre(genreId: Int) {
        val settings = getSettings()
        val newBlockGenres = settings.blockGenres + genreId
        saveSettings(settings.copy(blockGenres = newBlockGenres))
    }
    
    /**
     * 取消屏蔽题材
     */
    suspend fun unblockGenre(genreId: Int) {
        val settings = getSettings()
        val newBlockGenres = settings.blockGenres - genreId
        saveSettings(settings.copy(blockGenres = newBlockGenres))
    }
    
    /**
     * 设置允许的媒体类型
     */
    suspend fun setAllowedMediaTypes(types: Set<MediaType>) {
        val settings = getSettings().copy(allowedMediaTypes = types)
        saveSettings(settings)
    }
    
    /**
     * 设置每日观看时长限制
     */
    suspend fun setDailyWatchLimit(minutes: Int) {
        val settings = getSettings().copy(dailyWatchLimit = minutes)
        saveSettings(settings)
    }
    
    /**
     * 屏蔽媒体
     */
    suspend fun blockMedia(mediaId: String) {
        val settings = getSettings()
        val newBlocked = settings.blockedMediaIds + mediaId
        saveSettings(settings.copy(blockedMediaIds = newBlocked))
    }
    
    /**
     * 取消屏蔽媒体
     */
    suspend fun unblockMedia(mediaId: String) {
        val settings = getSettings()
        val newBlocked = settings.blockedMediaIds - mediaId
        saveSettings(settings.copy(blockedMediaIds = newBlocked))
    }
    
    /**
     * 检查内容是否允许访问
     */
    suspend fun isContentAllowed(item: MediaItem): Boolean {
        val settings = getSettings()
        
        // 如果家长控制未启用，允许所有内容
        if (!settings.isEnabled) {
            return true
        }
        
        // 检查手动屏蔽的媒体
        if (settings.blockedMediaIds.contains(item.id)) {
            return false
        }
        
        // 检查媒体类型
        val mediaType = when (item.type) {
            "movie" -> MediaType.MOVIE
            "tv" -> MediaType.TV_SHOW
            "anime" -> MediaType.ANIME
            else -> return true
        }
        if (!settings.allowedMediaTypes.contains(mediaType)) {
            return false
        }
        
        // 检查题材屏蔽（通过题材名称匹配）
        if (item.genres.isNotEmpty()) {
            val matureKeywords = listOf("horror", "thriller", "war", "crime", "documentary", 
                "恐怖", "战争", "犯罪", "惊悚", "纪录片")
            if (settings.blockGenres.isNotEmpty() && 
                item.genres.any { genre -> matureKeywords.any { keyword -> 
                    genre.lowercase().contains(keyword) 
                }}
            ) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * 检查是否已解锁
     */
    suspend fun isUnlocked(): Boolean {
        val preferences = context.parentalDataStore.data.first()
        val isUnlocked = preferences[IS_UNLOCKED_KEY] ?: false
        val unlockUntil = preferences[UNLOCK_UNTIL_KEY] ?: 0L
        
        return if (isUnlocked && unlockUntil > System.currentTimeMillis()) {
            true
        } else {
            // 已过期，重置状态
            if (isUnlocked) {
                lock()
            }
            false
        }
    }
    
    /**
     * 临时解锁
     * @param durationMs 解锁时长（毫秒）
     */
    suspend fun unlockTemporarily(durationMs: Long) {
        context.parentalDataStore.edit { preferences ->
            preferences[IS_UNLOCKED_KEY] = true
            preferences[UNLOCK_UNTIL_KEY] = System.currentTimeMillis() + durationMs
        }
    }
    
    /**
     * 锁定
     */
    suspend fun lock() {
        context.parentalDataStore.edit { preferences ->
            preferences[IS_UNLOCKED_KEY] = false
            preferences[UNLOCK_UNTIL_KEY] = 0L
        }
    }
    
    /**
     * 验证 PIN 并解锁
     * @param pin 输入的 PIN
     * @param durationMs 解锁时长（毫秒）
     * @return true 如果验证成功并已解锁
     */
    suspend fun verifyAndUnlock(pin: String, durationMs: Long = 30 * 60 * 1000): Boolean {
        return if (verifyPin(pin)) {
            unlockTemporarily(durationMs)
            true
        } else {
            false
        }
    }
    
    /**
     * 检查 PIN 格式是否有效（4位数字）
     */
    private fun isValidPin(pin: String): Boolean {
        return pin.matches(Regex("^\\d{4}$"))
    }
}
