package com.moviecode.tv.data.repository

import android.content.Context
import android.util.Log
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.domain.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 媒体服务器类型
 */
enum class MediaServerType {
    PLEX, EMBY, JELLYFIN
}

/**
 * 媒体服务器配置
 */
data class MediaServerConfig(
    val type: MediaServerType,
    val name: String,
    val url: String,
    val apiKey: String,
    val userId: String? = null
)

/**
 * 媒体服务器响应
 */
sealed class MediaServerResult<out T> {
    data class Success<T>(val data: T) : MediaServerResult<T>()
    data class Error(val message: String) : MediaServerResult<Nothing>()
}

/**
 * Plex/Emby 连接器仓库
 */
@Singleton
class MediaServerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val servers = mutableMapOf<String, MediaServerConfig>()

    companion object {
        private const val TAG = "MediaServerRepo"
    }

    /**
     * 添加服务器
     */
    fun addServer(config: MediaServerConfig) {
        servers[config.name] = config
        Log.d(TAG, "Added server: ${config.name}")
    }

    /**
     * 移除服务器
     */
    fun removeServer(name: String) {
        servers.remove(name)
    }

    /**
     * 获取服务器
     */
    fun getServer(name: String): MediaServerConfig? = servers[name]

    /**
     * 列出所有服务器
     */
    fun listServers(): List<MediaServerConfig> = servers.values.toList()

    /**
     * 测试连接
     */
    suspend fun testConnection(config: MediaServerConfig): MediaServerResult<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            when (config.type) {
                MediaServerType.PLEX -> testPlexConnection(config)
                MediaServerType.EMBY, MediaServerType.JELLYFIN -> testEmbyConnection(config)
            }
        } catch (e: Exception) {
            MediaServerResult.Error(e.message ?: "Connection failed")
        }
    }

    private fun testPlexConnection(config: MediaServerConfig): MediaServerResult<Map<String, String>> {
        val url = "${config.url}/api/v2/resources?includeHttps=1"
        val request = Request.Builder()
            .url(url)
            .header("X-Plex-Token", config.apiKey)
            .get()
            .build()

        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            MediaServerResult.Success(mapOf(
                "name" to "Plex Server",
                "version" to "Connected"
            ))
        } else {
            MediaServerResult.Error("Connection failed: ${response.code}")
        }
    }

    private fun testEmbyConnection(config: MediaServerConfig): MediaServerResult<Map<String, String>> {
        val url = "${config.url}/System/Info"
        val request = Request.Builder()
            .url(url)
            .header("X-Emby-Token", config.apiKey)
            .get()
            .build()

        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)
            MediaServerResult.Success(mapOf(
                "name" to (json.optString("ServerName", "Emby Server")),
                "version" to (json.optString("Version", "Unknown"))
            ))
        } else {
            MediaServerResult.Error("Connection failed: ${response.code}")
        }
    }

    /**
     * 获取媒体库列表
     */
    suspend fun getLibraries(config: MediaServerConfig): MediaServerResult<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            when (config.type) {
                MediaServerType.PLEX -> getPlexLibraries(config)
                MediaServerType.EMBY, MediaServerType.JELLYFIN -> getEmbyLibraries(config)
            }
        } catch (e: Exception) {
            MediaServerResult.Error(e.message ?: "Failed to get libraries")
        }
    }

    private fun getPlexLibraries(config: MediaServerConfig): MediaServerResult<List<MediaItem>> {
        val url = "${config.url}/api/v2/sections"
        val request = Request.Builder()
            .url(url)
            .header("X-Plex-Token", config.apiKey)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return MediaServerResult.Error("Failed: ${response.code}")

        val body = response.body?.string() ?: ""
        val items = parsePlexSections(body, config)
        return MediaServerResult.Success(items)
    }

    private fun getEmbyLibraries(config: MediaServerConfig): MediaServerResult<List<MediaItem>> {
        val url = "${config.url}/Library/MediaFolders"
        val request = Request.Builder()
            .url(url)
            .header("X-Emby-Token", config.apiKey)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return MediaServerResult.Error("Failed: ${response.code}")

        val body = response.body?.string() ?: "{}"
        val items = parseEmbyMediaFolders(body, config)
        return MediaServerResult.Success(items)
    }

    /**
     * 获取媒体库内容
     */
    suspend fun getLibraryContent(
        config: MediaServerConfig,
        libraryKey: String,
        type: String = "movie"
    ): MediaServerResult<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            when (config.type) {
                MediaServerType.PLEX -> getPlexLibraryContent(config, libraryKey, type)
                MediaServerType.EMBY, MediaServerType.JELLYFIN -> getEmbyLibraryContent(config, libraryKey, type)
            }
        } catch (e: Exception) {
            MediaServerResult.Error(e.message ?: "Failed to get content")
        }
    }

    private fun getPlexLibraryContent(
        config: MediaServerConfig,
        libraryKey: String,
        type: String
    ): MediaServerResult<List<MediaItem>> {
        val url = "${config.url}/api/v2/sections/$libraryKey/all?type=${if (type == "movie") 1 else 2}"
        val request = Request.Builder()
            .url(url)
            .header("X-Plex-Token", config.apiKey)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return MediaServerResult.Error("Failed: ${response.code}")

        val body = response.body?.string() ?: ""
        val items = parsePlexMetadata(body, config)
        return MediaServerResult.Success(items)
    }

    private fun getEmbyLibraryContent(
        config: MediaServerConfig,
        libraryKey: String,
        type: String
    ): MediaServerResult<List<MediaItem>> {
        val itemType = if (type == "movie") "Movie" else "Series"
        val url = "${config.url}/Items?ParentId=$libraryKey&IncludeItemTypes=$itemType&limit=50"
        val request = Request.Builder()
            .url(url)
            .header("X-Emby-Token", config.apiKey)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return MediaServerResult.Error("Failed: ${response.code}")

        val body = response.body?.string() ?: "{}"
        val items = parseEmbyItems(body, config)
        return MediaServerResult.Success(items)
    }

    /**
     * 搜索媒体
     */
    suspend fun searchMedia(config: MediaServerConfig, query: String): MediaServerResult<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            when (config.type) {
                MediaServerType.PLEX -> searchPlexMedia(config, query)
                MediaServerType.EMBY, MediaServerType.JELLYFIN -> searchEmbyMedia(config, query)
            }
        } catch (e: Exception) {
            MediaServerResult.Error(e.message ?: "Search failed")
        }
    }

    private fun searchPlexMedia(config: MediaServerConfig, query: String): MediaServerResult<List<MediaItem>> {
        val url = "${config.url}/api/v2/search?query=$query"
        val request = Request.Builder()
            .url(url)
            .header("X-Plex-Token", config.apiKey)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return MediaServerResult.Error("Failed: ${response.code}")

        val body = response.body?.string() ?: ""
        val items = parsePlexMetadata(body, config)
        return MediaServerResult.Success(items)
    }

    private fun searchEmbyMedia(config: MediaServerConfig, query: String): MediaServerResult<List<MediaItem>> {
        val url = "${config.url}/Items?searchTerm=$query&limit=20"
        val request = Request.Builder()
            .url(url)
            .header("X-Emby-Token", config.apiKey)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return MediaServerResult.Error("Failed: ${response.code}")

        val body = response.body?.string() ?: "{}"
        val items = parseEmbyItems(body, config)
        return MediaServerResult.Success(items)
    }

    // 解析 Plex 响应
    private fun parsePlexSections(body: String, config: MediaServerConfig): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        // 简化解析，实际需要 XML 解析
        return items
    }

    private fun parsePlexMetadata(body: String, config: MediaServerConfig): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        // 简化解析，实际需要 XML 解析
        return items
    }

    // 解析 Emby 响应
    private fun parseEmbyMediaFolders(body: String, config: MediaServerConfig): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        try {
            val json = JSONObject(body)
            val jsonItems = json.optJSONArray("Items") ?: return items
            
            for (i in 0 until jsonItems.length()) {
                val item = jsonItems.getJSONObject(i)
                val name = item.optString("Name", "")
                val id = item.optString("Id", "")
                val collectionType = item.optString("CollectionType", "")
                
                if (collectionType.isNotEmpty()) {
                    items.add(
                        MediaItem(
                            id = id.hashCode().toLong(),
                            tmdbId = null,
                            title = name,
                            type = when (collectionType) {
                                "movies" -> MediaType.MOVIE
                                "tvshows" -> MediaType.TV_SHOW
                                else -> MediaType.MOVIE
                            },
                            posterPath = getEmbyImageUrl(config, id, "Primary"),
                            year = null,
                            rating = null,
                            overview = null,
                            genres = emptyList(),
                            backdropPath = null,
                            releaseDate = null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error", e)
        }
        return items
    }

    private fun parseEmbyItems(body: String, config: MediaServerConfig): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        try {
            val json = JSONObject(body)
            val jsonItems = json.optJSONArray("Items") ?: return items
            
            for (i in 0 until jsonItems.length()) {
                val item = jsonItems.getJSONObject(i)
                val id = item.optString("Id", "")
                val name = item.optString("Name", "")
                val type = item.optString("Type", "")
                val year = item.optInt("ProductionYear", 0)
                val rating = item.optDouble("CommunityRating", 0.0)
                val overview = item.optString("Overview", "")
                
                items.add(
                    MediaItem(
                        id = id.hashCode().toLong(),
                        tmdbId = null,
                        title = name,
                        type = when (type) {
                            "Movie" -> MediaType.MOVIE
                            "Series" -> MediaType.TV_SHOW
                            else -> MediaType.MOVIE
                        },
                        posterPath = getEmbyImageUrl(config, id, "Primary"),
                        year = if (year > 0) year else null,
                        rating = if (rating > 0) rating else null,
                        overview = overview.takeIf { it.isNotEmpty() },
                        genres = emptyList(),
                        backdropPath = getEmbyImageUrl(config, id, "Backdrop", 0),
                        releaseDate = item.optString("PremiereDate", null)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error", e)
        }
        return items
    }

    private fun getEmbyImageUrl(config: MediaServerConfig, id: String, type: String, index: Int = 0): String? {
        return "${config.url}/Items/$id/Images/$type?X-Emby-Token=${config.apiKey}"
    }
}
