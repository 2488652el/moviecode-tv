package com.moviecode.tv.util

import java.io.File

/**
 * 字幕文件信息
 */
data class SubtitleFile(
    val path: String,
    val name: String,
    val type: String
)

/**
 * 字幕搜索工具
 * 自动搜索与视频文件同名的字幕文件
 */
object SubtitleFinder {
    
    // 支持的字幕格式
    private val SUBTITLE_EXTENSIONS = listOf("srt", "ass", "ssa", "vtt", "sub", "subrip")
    
    // 可能的字幕目录名
    private val SUBTITLE_DIRS = listOf(
        "",           // 同目录
        "Subs",       // Subs 文件夹
        "字幕",        // 中文"字幕"文件夹
        "subtitles",  // 英文 subtitles
        "SUB",        // SUB 大写
        "Subtitles"   // Subtitles
    )
    
    /**
     * 搜索与视频文件关联的字幕文件
     * @param videoPath 视频文件路径
     * @return 找到的字幕文件列表
     */
    fun searchSubtitles(videoPath: String): List<SubtitleFile> {
        val videoFile = File(videoPath)
        if (!videoFile.exists()) return emptyList()
        
        val videoDir = videoFile.parentFile ?: return emptyList()
        val videoName = videoFile.nameWithoutExtension.lowercase()
        
        val subtitles = mutableSetOf<SubtitleFile>()
        
        // 遍历所有可能的字幕目录
        for (dirName in SUBTITLE_DIRS) {
            val searchDir = if (dirName.isEmpty()) {
                videoDir
            } else {
                File(videoDir, dirName)
            }
            
            if (!searchDir.exists() || !searchDir.isDirectory) continue
            
            searchDir.listFiles()?.forEach { file ->
                if (!file.isFile) return@forEach
                
                val ext = file.extension.lowercase()
                val fileName = file.nameWithoutExtension.lowercase()
                
                // 检查是否是支持的字幕格式
                if (!SUBTITLE_EXTENSIONS.contains(ext)) return@forEach
                
                // 检查文件名是否与视频相关
                // 匹配规则：字幕名包含视频名 或 视频名包含字幕名
                val isRelated = fileName.contains(videoName) ||
                        videoName.contains(fileName) ||
                        fuzzyMatch(fileName, videoName)
                
                if (isRelated) {
                    subtitles.add(SubtitleFile(
                        path = file.absolutePath,
                        name = file.name,
                        type = ext
                    ))
                }
            }
        }
        
        // 父目录搜索（有些字幕放在上级目录）
        videoDir.parentFile?.let { parentDir ->
            parentDir.listFiles()?.forEach { file ->
                if (!file.isFile) return@forEach
                
                val ext = file.extension.lowercase()
                val fileName = file.nameWithoutExtension.lowercase()
                
                if (SUBTITLE_EXTENSIONS.contains(ext) && fileName.contains(videoName)) {
                    subtitles.add(SubtitleFile(
                        path = file.absolutePath,
                        name = file.name,
                        type = ext
                    ))
                }
            }
        }
        
        return subtitles.toList()
    }
    
    /**
     * 模糊匹配
     * 移除特殊字符后比较
     */
    private fun fuzzyMatch(name1: String, name2: String): Boolean {
        val clean1 = name1.replace(Regex("[^a-z0-9]"), "")
        val clean2 = name2.replace(Regex("[^a-z0-9]"), "")
        
        return clean1.contains(clean2) || clean2.contains(clean1)
    }
    
    /**
     * 获取字幕类型描述
     */
    fun getSubtitleTypeName(type: String): String {
        return when (type.lowercase()) {
            "srt" -> "SubRip"
            "ass", "ssa" -> "Advanced SubStation Alpha"
            "vtt" -> "WebVTT"
            "sub", "subrip" -> "SubViewer"
            else -> type.uppercase()
        }
    }
}
