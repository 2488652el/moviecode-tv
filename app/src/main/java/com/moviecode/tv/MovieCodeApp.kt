package com.moviecode.tv

import android.app.Application
import android.os.StrictMode
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.moviecode.tv.data.repository.MediaRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MovieCode 应用启动优化
 * 
 * 优化策略:
 * 1. 延迟初始化非关键组件
 * 2. 预加载热门数据
 * 3. 图片缓存优化
 */
@HiltAndroidApp
class MovieCodeApp : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject
    lateinit var mediaRepository: MediaRepository

    // 预加载计数器
    private var preloadedCount = 0
    private val maxPreloadCount = 50 // 最多预加载50张图片URL

    override fun onCreate() {
        super.onCreate()
        
        // 开发环境启用严格模式
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        
        // 延迟预加载数据
        applicationScope.launch(Dispatchers.IO) {
            preloadCriticalData()
        }
    }

    /**
     * 启用严格模式（仅开发环境）
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .penaltyLog()
                .build()
        )
        
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }

    /**
     * 预加载关键数据
     */
    private suspend fun preloadCriticalData() {
        try {
            // 只获取首页展示所需的数据
            val popularMovies = mediaRepository.getPopularMovies().getOrNull()?.take(10) ?: emptyList()
            
            // 收集图片URL用于预缓存
            val imageUrls = popularMovies
                .mapNotNull { it.posterPath }
                .take(maxPreloadCount)
            
            // 记录预加载的图片URL供ImageLoader使用
            preloadedImageUrls = imageUrls
            preloadedCount = imageUrls.size
            
        } catch (e: Exception) {
            // 忽略预加载错误，不影响应用启动
        }
    }

    /**
     * 创建优化的 ImageLoader
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 使用 25% 可用内存
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250 * 1024 * 1024) // 250MB
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .crossfade(300)
            .respectCacheHeaders(false)
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }

    companion object {
        // 存储预加载的图片URL
        var preloadedImageUrls: List<String> = emptyList()
            private set
    }
}

/**
 * 启动性能监控
 */
object StartupMonitor {
    private var startTime: Long = 0
    
    fun start() {
        startTime = System.currentTimeMillis()
    }
    
    fun getElapsedTime(): Long {
        return System.currentTimeMillis() - startTime
    }
    
    fun logStartupComplete(tag: String) {
        val elapsed = getElapsedTime()
        android.util.Log.i("Startup", "$tag completed in ${elapsed}ms")
    }
}
