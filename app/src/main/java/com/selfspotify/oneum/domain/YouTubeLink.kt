package com.selfspotify.oneum.domain

import java.net.URI

data class YouTubeLink(val videoId: String) {
    val url: String get() = "https://www.youtube.com/watch?v=$videoId"

    companion object {
        private val videoIdPattern = Regex("^[A-Za-z0-9_-]{11}$")
        private val allowedHosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com")

        fun parse(input: String): YouTubeLink? = runCatching {
            val text = input.trim()
            val uri = URI(if (text.startsWith("https://") || text.startsWith("http://")) text else "https://$text")
            if (uri.scheme !in setOf("http", "https") || uri.userInfo != null || uri.port != -1) return null
            val host = uri.host?.lowercase() ?: return null
            val segments = uri.path.orEmpty().trim('/').split('/')
            val id = when {
                host == "youtu.be" && segments.size == 1 -> segments[0]
                host in allowedHosts && uri.path == "/watch" -> uri.rawQuery.orEmpty()
                    .split('&').firstOrNull { it.startsWith("v=") }?.substringAfter("v=")
                host in allowedHosts && segments.size == 2 && segments[0] in setOf("shorts", "live", "embed") -> segments[1]
                else -> null
            }
            id?.takeIf { videoIdPattern.matches(it) }?.let(::YouTubeLink)
        }.getOrNull()

        fun fromSharedText(text: String): YouTubeLink? =
            Regex("https?://[^\\s<>]+").findAll(text).mapNotNull { parse(it.value.trimEnd('.', ',', ')')) }.firstOrNull()
    }
}

enum class VideoQuality(val height: Int) { HD(720), FULL_HD(1080) }

object DownloadFormats {
    fun video(quality: VideoQuality): String {
        val height = quality.height
        return "bv[height=$height][ext=mp4]+ba[ext=m4a]/b[height=$height][ext=mp4]/bv[height=$height]+ba/b[height=$height]"
    }
}
