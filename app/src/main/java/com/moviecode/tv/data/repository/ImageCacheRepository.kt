package com.moviecode.tv.data.repository

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 图片缓存管理器
 * 使用 Coil 的内存缓存和磁盘缓存
 */
@Singleton
class ImageCacheRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    // 缓存配置
    companion object {
        private const val MEMORY_CACHE_PERCENT = 0.25 // 25% 内存
        private const val DISK_CACHE_SIZE_MB = 250L // 250MB 磁盘缓存
        private const val DISK_CACHE_NAME = "image_cache"
    }
    
    private var imageLoader: ImageLoader? = null
    
    /**
     * 获取优化的 ImageLoader
     * 配置了内存缓存和磁盘缓存
     */
    fun getOptimizedImageLoader(): ImageLoader {
        return imageLoader ?: ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(MEMORY_CACHE_PERCENT)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(DISK_CACHE_NAME))
                    .maxSizeBytes(DISK_CACHE_SIZE_MB * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build().also {
                imageLoader = it
            }
    }
    
    /**
     * 获取缓存统计信息
     */
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        val loader = getOptimizedImageLoader()
        
        CacheStats(
            memoryCacheSize = loader.memoryCache?.size ?: 0L,
            memoryCacheMaxSize = loader.memoryCache?.maxSize ?: 0L,
            diskCacheSize = loader.diskCache?.size ?: 0L,
            diskCacheMaxSize = DISK_CACHE_SIZE_MB * 1024 * 1024
        )
    }
    
    /**
     * 清除所有缓存
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        imageLoader?.memoryCache?.clear()
        imageLoader?.diskCache?.clear()
    }
    
    /**
     * 预加载图片到缓存
     */
    suspend fun preloadImages(urls: List<String>) = withContext(Dispatchers.IO) {
        urls.take(20).forEach { url ->
            try {
                val request = coil.request.ImageRequest.Builder(context)
                    .data(url)
                    .build()
                getOptimizedImageLoader().enqueue(request)
            } catch (e: Exception) {
                // 忽略单张图片加载失败
            }
        }
    }
}

/**
 * 缓存统计信息
 */
data class CacheStats(
    val memoryCacheSize: Long,
    val memoryCacheMaxSize: Long,
    val diskCacheSize: Long,
    val diskCacheMaxSize: Long
) {
    val memoryUsagePercent: Float
        get() = if (memoryCacheMaxSize > 0) 
            (memoryCacheSize.toFloat() / memoryCacheMaxSize) * 100 
        else 0f
    
    val diskUsagePercent: Float
        get() = if (diskCacheMaxSize > 0) 
            (diskCacheSize.toFloat() / diskCacheMaxSize) * 100 
        else 0f
    
    val memoryCacheSizeMB: Float
        get() = memoryCacheSize / (1024 * 1024)
    
    val diskCacheSizeMB: Float
        get() = diskCacheSize / (1024 * 1024)
}
