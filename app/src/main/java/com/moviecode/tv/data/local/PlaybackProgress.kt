package com.moviecode.tv.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 播放进度实体
 * 用于存储影片的播放进度，支持续播功能
 */
@Entity(tableName = "playback_progress")
data class PlaybackProgress(
    @PrimaryKey
    val mediaId: String,
    val filePath: String,
    val currentTime: Double,
    val duration: Double,
    val lastWatched: Long = System.currentTimeMillis()
) {
    /**
     * 获取播放百分比
     */
    val progressPercent: Int
        get() = if (duration > 0) ((currentTime / duration) * 100).toInt() else 0
}
