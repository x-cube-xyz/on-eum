package com.selfspotify.oneum

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.selfspotify.oneum.data.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AppFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun navigationLinkValidationAndVideoQuality() {
        rule.onNodeWithText("보관함").performClick()
        rule.onNodeWithText("나의 보관함").assertIsDisplayed()
        rule.onNodeWithText("다운로드").performClick()
        rule.onNodeWithTag("download-button").assertIsNotEnabled()
        rule.onNodeWithTag("youtube-url").performTextInput("https://youtube.com.evil.test/watch?v=BaW_jenozKc")
        rule.onNodeWithTag("download-button").assertIsNotEnabled()
        rule.onNodeWithTag("youtube-url").performTextReplacement("https://youtu.be/BaW_jenozKc")
        rule.runOnUiThread { WindowCompat.getInsetsController(rule.activity.window, rule.activity.window.decorView).hide(WindowInsetsCompat.Type.ime()) }
        rule.waitUntil(5000) { rule.onAllNodesWithTag("bottom-navigation").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("download-button").assertIsEnabled()
        rule.onNodeWithTag("include-video").performScrollTo().performClick()
        rule.onNodeWithText("720p · HD").assertIsDisplayed()
        rule.onNodeWithText("1080p · Full HD").performClick()
        rule.onNodeWithText("설정").performClick()
        rule.onNodeWithText("기기 설정에 맞춘 테마").assertIsDisplayed()
    }

    @Test fun localAudioPlaybackAndPersistentFavorites() {
        val app = rule.activity.application as OneumApplication
        val file = File(app.library.mediaDirectory, "testfixture/audio.wav").apply { parentFile?.mkdirs() }
        file.writeBytes(silentWav())
        val track = Track("testfixture", "로컬 재생 검증", "온음 테스트", 20,
            "https://www.youtube.com/watch?v=BaW_jenozKc", file.absolutePath)
        val model = ViewModelProvider(rule.activity)[MainViewModel::class.java]
        try {
            runBlocking { app.library.upsert(track); app.library.toggleFavorite(track.id); app.library.load() }
            assertTrue(app.library.tracks.value.first { it.id == track.id }.favorite)
            rule.waitUntil(10000) { model.controller.value != null }
            rule.runOnUiThread { model.play(track) }
            rule.waitUntil(15000) { model.playback.value.isPlaying }
            rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            Thread.sleep(900)
            assertTrue("Playback should continue while the activity is stopped", model.playback.value.isPlaying)
            rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            rule.runOnUiThread { model.seekTo(5000) }
            rule.waitUntil(5000) { model.playback.value.position >= 5000 }
            rule.runOnUiThread { model.togglePlay() }
            rule.waitUntil(5000) { !model.playback.value.isPlaying }
            assertEquals(track.id, model.playback.value.trackId)
        } finally {
            rule.runOnUiThread { model.controller.value?.run { stop(); clearMediaItems() } }
            runBlocking { app.library.remove(track.id) }
        }
    }

    @Test fun captureSystemDarkAndLightScreens() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshots = File(rule.activity.filesDir, "screenshots").apply { mkdirs() }
        for ((mode, name) in listOf("yes" to "dark", "no" to "light")) {
            instrumentation.uiAutomation.executeShellCommand("cmd uimode night $mode").close()
            Thread.sleep(1500)
            rule.waitForIdle()
            rule.onNodeWithText("홈").performClick()
            rule.waitForIdle()
            instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
                File(screenshots, "home-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            rule.onNodeWithText("다운로드").performClick()
            if (rule.onAllNodesWithText("720p · HD").fetchSemanticsNodes().isEmpty()) rule.onNodeWithTag("include-video").performClick()
            rule.waitForIdle()
            instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
                File(screenshots, "download-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }

    private fun silentWav(): ByteArray {
        val sampleRate = 8000
        val size = sampleRate * 20 * 2
        return ByteBuffer.allocate(44 + size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + size); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(sampleRate); putInt(sampleRate * 2)
            putShort(2); putShort(16); put("data".toByteArray()); putInt(size)
            put(ByteArray(size))
        }.array()
    }
}
