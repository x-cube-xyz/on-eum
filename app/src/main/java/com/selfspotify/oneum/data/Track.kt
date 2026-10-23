package com.selfspotify.oneum.data

import org.json.JSONObject

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val sourceUrl: String,
    val audioPath: String,
    val videoPath: String? = null,
    val videoHeight: Int? = null,
    val artworkUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val favorite: Boolean = false,
    val lastPlayedAt: Long = 0,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("title", title); put("artist", artist)
        put("durationSeconds", durationSeconds); put("sourceUrl", sourceUrl)
        put("audioPath", audioPath); put("videoPath", videoPath)
        put("videoHeight", videoHeight); put("artworkUrl", artworkUrl)
        put("addedAt", addedAt); put("favorite", favorite); put("lastPlayedAt", lastPlayedAt)
    }

    companion object {
        fun fromJson(json: JSONObject) = Track(
            id = json.getString("id"), title = json.getString("title"),
            artist = json.optString("artist", "알 수 없는 아티스트"),
            durationSeconds = json.optInt("durationSeconds"), sourceUrl = json.getString("sourceUrl"),
            audioPath = json.getString("audioPath"),
            videoPath = json.optString("videoPath").takeIf { it.isNotBlank() && it != "null" },
            videoHeight = json.optInt("videoHeight").takeIf { it > 0 },
            artworkUrl = json.optString("artworkUrl").takeIf { it.isNotBlank() && it != "null" },
            addedAt = json.optLong("addedAt"), favorite = json.optBoolean("favorite"),
            lastPlayedAt = json.optLong("lastPlayedAt"),
        )
    }
}
