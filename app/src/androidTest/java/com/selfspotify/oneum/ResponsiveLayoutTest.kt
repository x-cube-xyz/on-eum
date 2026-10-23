package com.selfspotify.oneum

import android.graphics.Bitmap
import android.app.UiAutomation
import android.content.res.Configuration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.selfspotify.oneum.data.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONObject

class ResponsiveLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun verifyDeviceProfile() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val profile = InstrumentationRegistry.getArguments().getString("oneum.layoutProfile") ?: "default"
        if (profile.contains("landscape")) {
            assertTrue(instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_90))
            rule.waitUntil(10000) { rule.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        }
        rule.runOnIdle {
            val config = rule.activity.resources.configuration
            val cutout = rule.activity.window.decorView.rootWindowInsets?.displayCutout
            val values = JSONObject().put("widthDp", config.screenWidthDp).put("heightDp", config.screenHeightDp)
                .put("fontScale", config.fontScale).put("landscape", config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                .put("cutoutLeft", cutout?.safeInsetLeft ?: 0).put("cutoutTop", cutout?.safeInsetTop ?: 0)
                .put("cutoutRight", cutout?.safeInsetRight ?: 0).put("cutoutBottom", cutout?.safeInsetBottom ?: 0)
            val directory = File(rule.activity.filesDir, "layout-screenshots").apply { mkdirs() }
            File(directory, "$profile-config.json").writeText(values.toString(2))
            if (profile.contains("landscape")) assertTrue(config.screenWidthDp > config.screenHeightDp)
            if (profile.contains("cutout")) assertNotNull("Cutout profile must include a display cutout", cutout)
        }
    }

    @Test fun homeAndDownloadActionsRemainReachable() {
        rule.onNodeWithTag("home-content").performScrollToNode(hasTestTag("home-download-button"))
        rule.onNodeWithTag("home-download-button").performScrollTo().assertIsDisplayed()
        val titleBottom = rule.onNodeWithTag("home-hero-title").fetchSemanticsNode().boundsInRoot.bottom
        val buttonTop = rule.onNodeWithTag("home-download-button").fetchSemanticsNode().boundsInRoot.top
        assertTrue("Hero text must not overlap the action", titleBottom <= buttonTop)
        assertNoTextOverflow(rule.onNodeWithTag("home-hero-title"))
        rule.onNodeWithTag("home-download-button").performClick()
        rule.onNodeWithTag("download-content").performScrollToNode(hasTestTag("include-video"))
        rule.onNodeWithTag("include-video").performScrollTo().performClick()
        listOf("720p · HD", "1080p · Full HD").forEach { label ->
            rule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
            assertNoTextOverflow(rule.onNodeWithText(label, useUnmergedTree = true))
        }
        rule.onNodeWithText("1080p · Full HD").performClick()
        rule.onNodeWithTag("download-button").performScrollTo().assertIsDisplayed()
        screenshot("download-options")
        rule.onNodeWithText("설정").performClick()
        rule.onNodeWithTag("settings-content").performScrollToNode(hasText("오픈소스 라이선스"))
        rule.onNodeWithText("오픈소스 라이선스").assertIsDisplayed().performClick()
        rule.onNodeWithText("확인").assertIsDisplayed().performClick()
    }

    @Test fun keyboardDoesNotCoverInputsOrDownloadAction() {
        rule.onNodeWithText("다운로드").performClick()
        rule.onNodeWithTag("download-content").performScrollToNode(hasTestTag("youtube-url"))
        rule.onNodeWithTag("youtube-url").performScrollTo().performClick()
            .performTextInput("https://youtu.be/gHKT4uU8Zng")
        rule.waitUntil(10000) { rule.onAllNodesWithTag("bottom-navigation").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithTag("download-button").performScrollTo().assertIsDisplayed().assertIsEnabled()
        assertNoTextOverflow(rule.onNodeWithText("다운로드", useUnmergedTree = true))
        screenshot("keyboard")
        rule.runOnUiThread { WindowCompat.getInsetsController(rule.activity.window, rule.activity.window.decorView).hide(WindowInsetsCompat.Type.ime()) }
    }

    @Test fun playerControlsRemainReachableAndWork() {
        val app = rule.activity.application as OneumApplication
        val model = ViewModelProvider(rule.activity)[MainViewModel::class.java]
        val file = File(app.library.mediaDirectory, "layoutfixture/audio.wav").apply { parentFile?.mkdirs() }
        file.writeBytes(silentWav())
        val track = Track("layoutfixture", "작은 화면과 큰 글자에서도 조작 가능한 재생 화면 검증", "레이아웃 검증 아티스트", 120,
            "https://www.youtube.com/watch?v=gHKT4uU8Zng", file.absolutePath)
        try {
            runBlocking { app.library.upsert(track) }
            rule.waitUntil(10000) { model.controller.value != null }
            rule.runOnUiThread { model.play(track) }
            rule.waitUntil(15000) { model.playback.value.isPlaying }
            val miniPlayer = rule.onNodeWithTag("mini-player").fetchSemanticsNode().boundsInRoot
            val cutoutLeft = rule.activity.window.decorView.rootWindowInsets?.displayCutout?.safeInsetLeft ?: 0
            assertTrue("Mini player must avoid the display cutout", miniPlayer.left >= cutoutLeft)
            rule.onNodeWithTag("mini-player").performClick()
            rule.onNodeWithTag("player-content").performScrollToNode(hasTestTag("player-play-button"))
            rule.onNodeWithTag("player-play-button").assertIsDisplayed().performClick()
            rule.waitUntil(5000) { !model.playback.value.isPlaying }
            rule.onNodeWithContentDescription("셔플 재생").performScrollTo().assertIsDisplayed().performClick()
            rule.waitUntil(5000) { model.playback.value.shuffle }
            rule.onNodeWithContentDescription("반복 재생").performScrollTo().assertIsDisplayed().performClick()
            rule.onNodeWithTag("player-content").performScrollToNode(hasText("내 기기에서 재생 중"))
            screenshot("player-footer")
            rule.onNodeWithText("내 기기에서 재생 중").assertIsDisplayed()
            screenshot("player-controls")
            rule.onNodeWithContentDescription("재생 화면 닫기").assertIsDisplayed().performClick()
        } finally {
            rule.runOnUiThread { model.controller.value?.run { stop(); clearMediaItems() } }
            runBlocking { app.library.remove(track.id) }
        }
    }

    private fun assertNoTextOverflow(node: SemanticsNodeInteraction) {
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Expected text layout", layouts.isNotEmpty())
        val overflow = layouts.any { layout ->
            layout.multiParagraph.didExceedMaxLines || (0 until layout.lineCount).any { line ->
                layout.getLineLeft(line) < -1f || layout.getLineRight(line) > layout.size.width + 1f ||
                    layout.getLineBottom(line) > layout.size.height + 1f
            }
        }
        if (overflow) screenshot("text-overflow")
        assertFalse("Text must fit its measured bounds: " + layouts.joinToString { layout ->
            "${layout.layoutInput.text.text}: size=${layout.size}, height=${layout.multiParagraph.height}, width=${layout.multiParagraph.width}, " +
                "widthOverflow=${layout.didOverflowWidth}, heightOverflow=${layout.didOverflowHeight}, constraints=${layout.layoutInput.constraints}"
        }, overflow)
    }

    private fun screenshot(name: String) {
        rule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val profile = InstrumentationRegistry.getArguments().getString("oneum.layoutProfile") ?: "default"
        val directory = File(rule.activity.filesDir, "layout-screenshots").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot().let { image ->
            File(directory, "$profile-$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun silentWav(): ByteArray {
        val sampleRate = 8000
        val size = sampleRate * 120 * 2
        return ByteBuffer.allocate(44 + size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + size); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(sampleRate); putInt(sampleRate * 2)
            putShort(2); putShort(16); put("data".toByteArray()); putInt(size); put(ByteArray(size))
        }.array()
    }
}
