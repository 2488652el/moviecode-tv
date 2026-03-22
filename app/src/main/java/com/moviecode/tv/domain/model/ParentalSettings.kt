package com.moviecode.tv.domain.model

/**
 * 年龄分级枚举
 */
enum class ContentRating(val label: String, val minAge: Int, val description: String) {
    G("G级", 0, "所有年龄"),
    PG("PG级", 10, "建议家长指导"),
    PG13("PG-13", 13, "13岁以上"),
    R("R级", 17, "17岁以上"),
    NC17("NC-17", 18, "成人内容"),
    ADULTS_ONLY("仅成人", 21, "最高限制")
}

/**
 * 媒体类型
 */
enum class MediaType {
    MOVIE,
    TV_SHOW,
    ANIME
}

/**
 * 家长控制设置数据类
 */
data class ParentalSettings(
    val isEnabled: Boolean = false,
    val pin: String = "0000", // 4位数字密码
    val contentRating: ContentRating = ContentRating.R, // 允许观看的最高分级
    val blockGenres: Set<Int> = emptySet(), // 屏蔽的题材ID列表 (TMDB genre IDs)
    val allowedMediaTypes: Set<MediaType> = setOf(MediaType.MOVIE, MediaType.TV_SHOW, MediaType.ANIME), // 允许的媒体类型
    val dailyWatchLimit: Int = 0, // 每日观看时长限制(分钟), 0表示不限制
    val blockedMediaIds: Set<String> = emptySet() // 手动屏蔽的媒体ID
)

/**
 * 需要家长控制的内容标签 (TMDB genre IDs)
 * 这些类型的内容应该被屏蔽
 */
object MatureGenreIds {
    val HORROR = 27
    val CRIME = 80
    val DOCUMENTARY = 99
    val WAR = 10752
    val THRILLER = 53
    
    val ALL = setOf(HORROR, CRIME, DOCUMENTARY, WAR, THRILLER)
}
