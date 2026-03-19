package com.moviecode.tv.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.moviecode.tv.domain.model.Episode
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.domain.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaScannerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MediaScanner"
        
        // Video file extensions
        private val VIDEO_EXTENSIONS = listOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", 
            "m4v", "mpeg", "mpg", "ts", "mts", "m2ts", "vob"
        )
        
        // Anime keywords
        private val ANIME_KEYWORDS = listOf(
            "[anime]", "[Anime]", "[ANIME]",
            "[BTC]', '[Heathen]', '[HR-DSB]', '[MKV]', '[NC]", 
            "【动漫】", "[动漫]", "动漫",
            "endless", "e-nova", "KTKJ", "Nekomoe", 
            "KYY", "Lily", "ReinForce", "philosophy-raws"
        )
        
        // Movie patterns
        private val MOVIE_PATTERNS = listOf(
            Pattern.compile("^(.+?)[.\\s](\\d{4})[.\\s]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)\\s*\\((\\d{4})\\)\\s*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)[.\\s](\\d{4})$", Pattern.CASE_INSENSITIVE)
        )
        
        // TV patterns
        private val TV_PATTERNS = listOf(
            Pattern.compile("^(.+?)[.\\s]S(\\d{2})E(\\d{2})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)[.\\s]S(\\d{2})E(\\d{1,3})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)[.\\s](\\d{1,2})x(\\d{2})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)[.\\s]Season[._\\s]*(\\d+)[.\\s]*Episode[._\\s]*(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)[.\\s]E(\\d{2,3})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)[.\\s]EP(\\d{2,3})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)[.\\s]第(\\d+)集", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)[.\\s]第(\\d+)话", Pattern.CASE_INSENSITIVE)
        )
    }

    data class ScanResult(
        val totalFiles: Int = 0,
        val movies: List<MediaItem> = emptyList(),
        val tvShows: List<MediaItem> = emptyList(),
        val anime: List<MediaItem> = emptyList()
    )

    suspend fun scanMediaLibrary(
        paths: List<String> = getDefaultPaths(),
        excludePatterns: List<String> = listOf("sample", "trailer", "bonus", "extras")
    ): Result<ScanResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting media scan for paths: $paths")
            val allFiles = mutableListOf<File>()
            
            for (path in paths) {
                val rootDir = File(path)
                if (rootDir.exists() && rootDir.isDirectory) {
                    scanDirectory(rootDir, allFiles, excludePatterns)
                }
            }
            
            val mediaItems = allFiles.mapNotNull { file -> parseMediaFile(file) }
            
            val movies = mediaItems.filter { it.type == MediaType.MOVIE }
            val tvShows = mediaItems.filter { it.type == MediaType.TV_SHOW }
            val anime = mediaItems.filter { it.type == MediaType.ANIME }
            
            Log.d(TAG, "Scan complete: ${movies.size} movies, ${tvShows.size} TV shows, ${anime.size} anime")
            
            Result.success(ScanResult(
                totalFiles = allFiles.size,
                movies = movies,
                tvShows = tvShows,
                anime = anime
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Scan failed", e)
            Result.failure(e)
        }
    }

    private fun scanDirectory(
        dir: File,
        result: MutableList<File>,
        excludePatterns: List<String>
    ) {
        if (!dir.canRead()) return
        
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // Skip hidden directories and system folders
                if (!file.name.startsWith(".") && 
                    file.name !in listOf("System Volume Information", "$RECYCLE.BIN")) {
                    scanDirectory(file, result, excludePatterns)
                }
            } else if (file.isFile) {
                val extension = file.extension.lowercase()
                val fileName = file.name.lowercase()
                
                if (extension in VIDEO_EXTENSIONS &&
                    excludePatterns.none { fileName.contains(it.lowercase()) }) {
                    result.add(file)
                }
            }
        }
    }

    private fun parseMediaFile(file: File): MediaItem? {
        val fileName = file.nameWithoutExtension
        val isAnime = ANIME_KEYWORDS.any { fileName.contains(it, ignoreCase = true) }
        
        // Try TV patterns first
        for (pattern in TV_PATTERNS) {
            val matcher = pattern.matcher(fileName)
            if (matcher.find()) {
                val title = matcher.group(1)?.trim()?.replace(".", " ") ?: return null
                
                return if (isAnime) {
                    MediaItem(
                        title = title,
                        type = MediaType.ANIME,
                        filePath = file.absolutePath,
                        lastModified = file.lastModified()
                    )
                } else {
                    MediaItem(
                        title = title,
                        type = MediaType.TV_SHOW,
                        filePath = file.absolutePath,
                        lastModified = file.lastModified()
                    )
                }
            }
        }
        
        // Try movie patterns
        for (pattern in MOVIE_PATTERNS) {
            val matcher = pattern.matcher(fileName)
            if (matcher.find()) {
                val title = matcher.group(1)?.trim()?.replace(".", " ") ?: return null
                val year = matcher.group(2)?.toIntOrNull()
                
                return MediaItem(
                    title = title,
                    type = MediaType.MOVIE,
                    year = year,
                    filePath = file.absolutePath,
                    lastModified = file.lastModified()
                )
            }
        }
        
        // Default to movie if no pattern matched
        return MediaItem(
            title = fileName.replace(".", " ").replace("_", " "),
            type = MediaType.MOVIE,
            filePath = file.absolutePath,
            lastModified = file.lastModified()
        )
    }

    private fun getDefaultPaths(): List<String> {
        val paths = mutableListOf<String>()
        
        // Internal storage
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)?.let {
            if (it.exists()) paths.add(it.absolutePath)
        }
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let {
            if (it.exists()) paths.add(it.absolutePath)
        }
        
        // External storage
        val externalDirs = context.getExternalFilesDirs(null)
        externalDirs?.forEach { dir ->
            dir?.parentFile?.parentFile?.parentFile?.let { storage ->
                val movies = File(storage, "Movies")
                val downloads = File(storage, "Download")
                if (movies.exists()) paths.add(movies.absolutePath)
                if (downloads.exists()) paths.add(downloads.absolutePath)
            }
        }
        
        return paths.distinct()
    }
}
