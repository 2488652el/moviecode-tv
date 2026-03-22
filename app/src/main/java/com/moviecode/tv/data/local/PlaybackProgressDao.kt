package com.moviecode.tv.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 播放进度数据访问对象
 */
@Dao
interface PlaybackProgressDao {
    
    /**
     * 获取指定影片的播放进度
     */
    @Query("SELECT * FROM playback_progress WHERE mediaId = :mediaId AND filePath = :filePath LIMIT 1")
    suspend fun getProgress(mediaId: String, filePath: String): PlaybackProgress?
    
    /**
     * 获取所有播放历史（按最近观看时间排序）
     */
    @Query("SELECT * FROM playback_progress ORDER BY lastWatched DESC LIMIT 50")
    fun getRecentProgress(): Flow<List<PlaybackProgress>>
    
    /**
     * 获取未看完的影片（进度在5%-95%之间）
     */
    @Query("SELECT * FROM playback_progress WHERE (currentTime / duration) BETWEEN 0.05 AND 0.95 ORDER BY lastWatched DESC")
    fun getInProgressMedia(): Flow<List<PlaybackProgress>>
    
    /**
     * 保存或更新播放进度
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: PlaybackProgress)
    
    /**
     * 删除指定影片的进度
     */
    @Query("DELETE FROM playback_progress WHERE mediaId = :mediaId")
    suspend fun deleteProgress(mediaId: String)
    
    /**
     * 清除所有播放历史
     */
    @Query("DELETE FROM playback_progress")
    suspend fun clearAll()
    
    /**
     * 获取播放历史数量
     */
    @Query("SELECT COUNT(*) FROM playback_progress")
    suspend fun getHistoryCount(): Int
}
