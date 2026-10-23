package com.selfspotify.oneum.domain

import org.junit.Assert.*
import org.junit.Test

class YouTubeLinkTest {
    private val id = "BaW_jenozKc"

    @Test fun `supported links normalize to a single video without playlist parameters`() {
        val links = listOf("https://youtu.be/$id?si=token", "https://www.youtube.com/watch?v=$id&list=PL123",
            "https://music.youtube.com/watch?v=$id", "https://m.youtube.com/shorts/$id", "youtube.com/live/$id")
        links.forEach { assertEquals("https://www.youtube.com/watch?v=$id", YouTubeLink.parse(it)?.url) }
    }

    @Test fun `rejects lookalike domains credentials ports playlists and malformed ids`() {
        listOf("https://youtube.com.evil.test/watch?v=$id", "https://evil.test/youtube.com/watch?v=$id",
            "https://youtube.com@evil.test/watch?v=$id", "https://user@youtube.com/watch?v=$id",
            "https://youtube.com:8443/watch?v=$id", "https://youtube.com/playlist?list=PL123",
            "https://youtu.be/abc", "file:///etc/passwd", "javascript:alert(1)", "").forEach { assertNull(it, YouTubeLink.parse(it)) }
    }

    @Test fun `shared text extracts a supported link without accepting unrelated links`() {
        assertEquals(id, YouTubeLink.fromSharedText("이 영상 보기 https://youtu.be/$id?si=hello")?.videoId)
        assertNull(YouTubeLink.fromSharedText("https://example.com/watch?v=$id"))
    }

    @Test fun `every video fallback requires exactly the selected height`() {
        VideoQuality.entries.forEach { quality ->
            val alternatives = DownloadFormats.video(quality).split('/')
            assertTrue(alternatives.all { it.contains("[height=${quality.height}]") })
            assertFalse(DownloadFormats.video(quality).contains("<="))
        }
        assertEquals(listOf(720, 1080), VideoQuality.entries.map { it.height })
    }
}
