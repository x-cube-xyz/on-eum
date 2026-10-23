package com.selfspotify.oneum

import android.app.Notification
import android.app.NotificationManager
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.selfspotify.oneum.download.DownloadWorker
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.ConcurrentSkipListSet

class SearchIntegrationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun youtubeSearchSelectsDownloadOptions() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("oneum.network") == "true")
        val model = ViewModelProvider(rule.activity)[MainViewModel::class.java]
        rule.onNodeWithText("검색").performClick()
        rule.onNodeWithTag("youtube-search-query").performTextInput("dlp test video")
        rule.onNodeWithTag("youtube-search-query").performImeAction()
        rule.waitUntil(180000) { !model.search.state.value.loading && model.search.state.value.submittedQuery != null }
        val state = model.search.state.value
        assertNull(state.error, state.error)
        assertTrue("Expected actual YouTube results", state.results.isNotEmpty())
        val screenshots = File(rule.activity.filesDir, "screenshots").apply { mkdirs() }
        rule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { image ->
            File(screenshots, "youtube-search.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val video = state.results.first()
        rule.onNodeWithTag("search-result-${video.id}").performClick()
        rule.onNodeWithTag("youtube-url").assertTextContains(video.url)
        rule.onNodeWithText("선택한 영상").assertIsDisplayed()
        rule.onNodeWithTag("include-video").performScrollTo().performClick()
        rule.onNodeWithText("1080p · Full HD").performScrollTo().assertIsDisplayed()
    }

    @Test fun reportedVideoDownloadsWithVisiblePercentageNotification() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("oneum.network") == "true")
        val app = rule.activity.application as OneumApplication
        val manager = WorkManager.getInstance(app)
        val videoId = "vY8iwpN3GXQ"
        val request = OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(workDataOf(
            "url" to "https://www.youtube.com/watch?v=$videoId", "includeVideo" to false)).build()
        val observed = ConcurrentSkipListSet<Int>()
        val notificationManager = app.getSystemService(NotificationManager::class.java)
        val observer = launch(Dispatchers.IO) {
            while (isActive) {
                notificationManager.activeNotifications.firstOrNull { it.id == request.id.hashCode() }?.notification?.extras?.let { extras ->
                    if (!extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, true)) observed += extras.getInt(Notification.EXTRA_PROGRESS)
                }
                delay(50)
            }
        }
        try {
            manager.enqueue(request).result.get()
            val result = withTimeout(240000) { manager.getWorkInfoByIdFlow(request.id).first { it?.state?.isFinished == true }!! }
            assertEquals("${result.outputData.getString("error")} ${result.outputData.getString("detail")}", WorkInfo.State.SUCCEEDED, result.state)
            val track = app.library.tracks.value.first { it.id == videoId }
            assertTrue(File(track.audioPath).length() > 0)
            assertTrue("Expected percentage updates in a system notification: $observed", observed.any { it > 0 })
            File(app.filesDir, "search-validation.json").writeText(JSONObject().apply {
                put("videoId", videoId); put("title", track.title); put("engineVersion", YoutubeDL.version(app))
                put("audioBytes", File(track.audioPath).length()); put("notificationPercentages", observed.toString())
            }.toString(2))
        } finally {
            observer.cancelAndJoin()
            manager.cancelWorkById(request.id).result.get()
            app.library.remove(videoId)
        }
    }
}
