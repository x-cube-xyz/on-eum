package com.selfspotify.oneum.search

import com.selfspotify.oneum.domain.YouTubeLink
import org.json.JSONObject

data class SearchVideo(val id: String, val title: String, val channel: String, val durationSeconds: Int?, val thumbnail: String?) {
    val url: String get() = YouTubeLink(id).url
}

object SearchResults {
    fun parse(json: String): List<SearchVideo> {
        val entries = JSONObject(json).optJSONArray("entries") ?: return emptyList()
        return (0 until entries.length()).mapNotNull { index ->
            val entry = entries.optJSONObject(index) ?: return@mapNotNull null
            val id = entry.optString("id")
            if (YouTubeLink.parse("https://youtu.be/$id") == null ||
                entry.optString("live_status") in setOf("is_live", "is_upcoming") || entry.optBoolean("is_live")) return@mapNotNull null
            val title = entry.optString("title").takeIf { it.isNotBlank() && it != "null" } ?: return@mapNotNull null
            val thumbnails = entry.optJSONArray("thumbnails")
            val thumbnail = (0 until (thumbnails?.length() ?: 0)).mapNotNull { thumbnails?.optJSONObject(it)?.optString("url") }
                .lastOrNull { it.startsWith("https://") }
                ?: entry.optString("thumbnail").takeIf { it.startsWith("https://") }
                ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"
            SearchVideo(id, title, entry.optString("channel").takeIf { it.isNotBlank() && it != "null" }
                ?: entry.optString("uploader").takeIf { it.isNotBlank() && it != "null" } ?: "YouTube",
                entry.optDouble("duration", -1.0).takeIf { it.isFinite() && it > 0 }?.toInt(), thumbnail)
        }.distinctBy { it.id }
    }
}
