package com.moviecode.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.moviecode.tv.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.downloadDataStore: DataStore<Preferences> by preferencesDataStore(name = "download_store")

/**
 * 下载状态
 */
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * 下载任务数据类
 */
data class DownloadTask(
    val id: String,
    val mediaItem: MediaItem,
    val episodeNumber: Int? = null,
    val seasonNumber: Int? = null,
    val url: String,
    val fileName: String,
    val filePath: String = "",
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Float = 0f,
    val error: String? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

/**
 * 下载仓库
 * 管理离线下载任务、进度、状态
 */
class DownloadRepository(private val context: Context) {
    
    private val gson = Gson()
    
    companion object {
        private val TASKS_KEY = stringPreferencesKey("download_tasks")
        private val DOWNLOAD_PATH_KEY = stringPreferencesKey("download_path")
        private val MAX_CONCURRENT_KEY = intPreferencesKey("max_concurrent")
        
        @Volatile
        private var INSTANCE: DownloadRepository? = null
        
        fun getInstance(context: Context): DownloadRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 获取下载任务列表 Flow
     */
    fun getTasksFlow(): Flow<List<DownloadTask>> {
        return context.downloadDataStore.data.map { preferences ->
            val json = preferences[TASKS_KEY] ?: "[]"
            try {
                val type = object : TypeToken<List<DownloadTask>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * 获取所有任务
     */
    suspend fun getTasks(): List<DownloadTask> {
        return getTasksFlow().first()
    }
    
    /**
     * 添加下载任务
     */
    suspend fun addTask(
        mediaItem: MediaItem,
        url: String,
        fileName: String,
        episodeNumber: Int? = null,
        seasonNumber: Int? = null
    ): String {
        val id = "download_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
        val task = DownloadTask(
            id = id,
            mediaItem = mediaItem,
            url = url,
            fileName = fileName,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber
        )
        
        val currentTasks = getTasks().toMutableList()
        currentTasks.add(task)
        saveTasks(currentTasks)
        
        return id
    }
    
    /**
     * 更新任务进度
     */
    suspend fun updateProgress(id: String, downloadedBytes: Long, totalBytes: Long? = null) {
        val tasks = getTasks().toMutableList()
        val index = tasks.indexOfFirst { it.id == id }
        if (index != -1) {
            val task = tasks[index]
            val newTotal = totalBytes ?: task.totalBytes
            val progress = if (newTotal > 0) (downloadedBytes.toFloat() / newTotal) * 100 else 0f
            tasks[index] = task.copy(
                downloadedBytes = downloadedBytes,
                totalBytes = newTotal,
                progress = progress.coerceAtMost(100f),
                status = if (progress >= 100f) DownloadStatus.COMPLETED else DownloadStatus.DOWNLOADING,
                startedAt = task.startedAt ?: System.currentTimeMillis()
            )
            saveTasks(tasks)
        }
    }
    
    /**
     * 更新任务状态
     */
    suspend fun updateStatus(id: String, status: DownloadStatus, error: String? = null) {
        val tasks = getTasks().toMutableList()
        val index = tasks.indexOfFirst { it.id == id }
        if (index != -1) {
            val task = tasks[index]
            tasks[index] = task.copy(
                status = status,
                error = error,
                startedAt = if (status == DownloadStatus.DOWNLOADING) (task.startedAt ?: System.currentTimeMillis()) else task.startedAt,
                completedAt = if (status == DownloadStatus.COMPLETED) System.currentTimeMillis() else task.completedAt,
                retryCount = if (status == DownloadStatus.FAILED) task.retryCount + 1 else task.retryCount
            )
            saveTasks(tasks)
        }
    }
    
    /**
     * 暂停任务
     */
    suspend fun pauseTask(id: String) {
        updateStatus(id, DownloadStatus.PAUSED)
    }
    
    /**
     * 恢复任务
     */
    suspend fun resumeTask(id: String) {
        updateStatus(id, DownloadStatus.PENDING)
    }
    
    /**
     * 取消任务
     */
    suspend fun cancelTask(id: String) {
        updateStatus(id, DownloadStatus.CANCELLED)
    }
    
    /**
     * 重试任务
     */
    suspend fun retryTask(id: String) {
        val tasks = getTasks().toMutableList()
        val index = tasks.indexOfFirst { it.id == id }
        if (index != -1) {
            val task = tasks[index]
            tasks[index] = task.copy(
                status = DownloadStatus.PENDING,
                error = null,
                retryCount = 0
            )
            saveTasks(tasks)
        }
    }
    
    /**
     * 移除任务
     */
    suspend fun removeTask(id: String) {
        val tasks = getTasks().toMutableList()
        tasks.removeAll { it.id == id }
        saveTasks(tasks)
    }
    
    /**
     * 清空已完成任务
     */
    suspend fun clearCompleted() {
        val tasks = getTasks().toMutableList()
        tasks.removeAll { it.status == DownloadStatus.COMPLETED }
        saveTasks(tasks)
    }
    
    /**
     * 清空所有任务
     */
    suspend fun clearAll() {
        saveTasks(emptyList())
    }
    
    /**
     * 获取单个任务
     */
    suspend fun getTask(id: String): DownloadTask? {
        return getTasks().find { it.id == id }
    }
    
    /**
     * 获取按状态过滤的任务
     */
    suspend fun getTasksByStatus(status: DownloadStatus): List<DownloadTask> {
        return getTasks().filter { it.status == status }
    }
    
    /**
     * 获取活跃任务（下载中或等待中）
     */
    suspend fun getActiveTasks(): List<DownloadTask> {
        return getTasks().filter {
            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING
        }
    }
    
    /**
     * 设置下载路径
     */
    suspend fun setDownloadPath(path: String) {
        context.downloadDataStore.edit { preferences ->
            preferences[DOWNLOAD_PATH_KEY] = path
        }
    }
    
    /**
     * 获取下载路径
     */
    suspend fun getDownloadPath(): String {
        return context.downloadDataStore.data.first()[DOWNLOAD_PATH_KEY] ?: ""
    }
    
    /**
     * 设置最大并发数
     */
    suspend fun setMaxConcurrent(max: Int) {
        context.downloadDataStore.edit { preferences ->
            preferences[MAX_CONCURRENT_KEY] = max
        }
    }
    
    /**
     * 获取最大并发数
     */
    suspend fun getMaxConcurrent(): Int {
        return context.downloadDataStore.data.first()[MAX_CONCURRENT_KEY] ?: 2
    }
    
    /**
     * 保存任务列表
     */
    private suspend fun saveTasks(tasks: List<DownloadTask>) {
        context.downloadDataStore.edit { preferences ->
            preferences[TASKS_KEY] = gson.toJson(tasks)
        }
    }
    
    /**
     * 格式化文件大小
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes == 0L) return "0 B"
        val k = 1024.0
        val sizes = listOf("B", "KB", "MB", "GB", "TB")
        val i = kotlin.math.floor(kotlin.math.log(bytes) / kotlin.math.log(k)).toInt()
        return "${String.format("%.2f", bytes / kotlin.math.pow(k, i.toDouble()))} ${sizes[i]}"
    }
}
