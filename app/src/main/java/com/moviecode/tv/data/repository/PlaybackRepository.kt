package com.moviecode.tv.data.repository

import android.content.Context
import com.moviecode.tv.data.local.AppDatabase
import com.moviecode.tv.data.local.PlaybackProgress
import kotlinx.coroutines.flow.Flow

/**
 * 播放进度仓库
 * 处理播放进度的存取逻辑
 */
class PlaybackRepository(context: Context) {
    
    private val database = AppDatabase.getInstance(context)
    private val dao = database.playbackProgressDao()
    
    /**
     * 获取影片播放进度
     */
    suspend fun getProgress(mediaId: String, filePath: String): PlaybackProgress? {
        return dao.getProgress(mediaId, filePath)
    }
    
    /**
     * 保存播放进度
     * 只保存进度在5%-95%之间的影片
     */
    suspend fun saveProgress(
        mediaId: String,
        filePath: String,
        currentTime: Double,
        duration: Double
    ) {
        val percent = if (duration > 0) currentTime / duration else 0.0
        
        // 只保存5%-95%之间的进度
        if (percent in 0.05..0.95) {
            dao.saveProgress(
                PlaybackProgress(
                    mediaId = mediaId,
                    filePath = filePath,
                    currentTime = currentTime,
                    duration = duration,
                    lastWatched = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * 获取最近播放历史
     */
    fun getRecentProgress(): Flow<List<PlaybackProgress>> {
        return dao.getRecentProgress()
    }
    
    /**
     * 获取未看完的影片
     */
    fun getInProgressMedia(): Flow<List<PlaybackProgress>> {
        return dao.getInProgressMedia()
    }
    
    /**
     * 删除指定影片的播放进度
     */
    suspend fun deleteProgress(mediaId: String) {
        dao.deleteProgress(mediaId)
    }
    
    /**
     * 清除所有播放历史
     */
    suspend fun clearAllProgress() {
        dao.clearAll()
    }
    
    /**
     * 检查是否需要显示续播提示
     */
    suspend fun shouldShowResumeDialog(mediaId: String, filePath: String): PlaybackProgress? {
        val progress = getProgress(mediaId, filePath) ?: return null
        val percent = progress.currentTime / progress.duration
        // 只在5%-95%之间显示续播提示
        return if (percent in 0.05..0.95) progress else null
    }
}
