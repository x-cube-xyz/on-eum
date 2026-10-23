package com.selfspotify.oneum.domain

import com.selfspotify.oneum.download.DownloadProgress
import org.junit.Assert.*
import org.junit.Test

class DownloadErrorsTest {
    @Test fun `403 log requests engine refresh and becomes a concise error`() {
        val log = "WARNING: Your yt-dlp version is older than 90 days!\nERROR: unable to download video data: HTTP Error 403: Forbidden"
        assertTrue(DownloadErrors.shouldRefreshEngine(log))
        val message = DownloadErrors.message(log)
        assertFalse(message.contains("WARNING"))
        assertFalse(message.contains("Forbidden"))
        assertTrue(message.contains("다시 시도"))
        assertFalse(DownloadErrors.shouldRefreshEngine("Sign in to confirm you're not a bot"))
    }

    @Test fun `notification distinguishes known progress from preparation and processing`() {
        assertEquals("음원 다운로드 중 · 47%", DownloadProgress("음악", "음원 다운로드 중", 47.8f).caption)
        assertEquals(0f, DownloadProgress("음악", "다운로드 중", 0f).boundedPercent)
        assertEquals(100f, DownloadProgress("음악", "다운로드 중", 120f).boundedPercent)
        assertNull(DownloadProgress("음악", "변환 중", -1f).boundedPercent)
        assertNull(DownloadProgress("음악", "준비 중", Float.NaN).boundedPercent)
        assertEquals("변환 중", DownloadProgress("음악", "변환 중").caption)
    }
}
