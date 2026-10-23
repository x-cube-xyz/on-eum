package com.selfspotify.oneum

import android.media.MediaMetadataRetriever
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.selfspotify.oneum.download.DownloadWorker
import com.selfspotify.oneum.download.DownloaderRuntime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class DownloaderSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun bundledNativeRuntimeInitializes() = runBlocking {
        DownloaderRuntime.initialize(rule.activity.application)
        assertTrue(File(rule.activity.applicationInfo.nativeLibraryDir, "libpython.so").isFile)
        assertTrue(File(rule.activity.applicationInfo.nativeLibraryDir, "libffmpeg.so").isFile)
        assertTrue(File(rule.activity.applicationInfo.nativeLibraryDir, "libqjs.so").isFile)
    }

    /** Opt in with -Pandroid.testInstrumentationRunnerArguments.oneum.network=true. */
    @Test fun downloadPublicYtDlpTestClip() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("oneum.network") == "true")
        val app = rule.activity.application as OneumApplication
        val manager = WorkManager.getInstance(app)
        val videoId = "gHKT4uU8Zng"
        val height = InstrumentationRegistry.getArguments().getString("oneum.videoHeight")?.toIntOrNull() ?: 0
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf("url" to "https://www.youtube.com/watch?v=$videoId", "includeVideo" to (height > 0), "height" to height)).build()
        try {
            manager.enqueue(request).result.get()
            val result = withTimeout(180000) { manager.getWorkInfoByIdFlow(request.id).first { it?.state?.isFinished == true }!! }
            assertEquals("${result.outputData.getString("error")} ${result.outputData.getString("detail")}", WorkInfo.State.SUCCEEDED, result.state)
            val track = app.library.tracks.value.first { it.id == videoId }
            assertTrue(File(track.audioPath).length() > 0)
            if (height > 0) {
                assertEquals(height, track.videoHeight)
                assertTrue(track.videoPath != null && File(track.videoPath!!).length() > 0)
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(track.videoPath)
                    assertEquals(height, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt())
                } finally { retriever.release() }
                val model = ViewModelProvider(rule.activity)[MainViewModel::class.java]
                rule.runOnUiThread { model.play(track, true) }
                rule.waitUntil(15000) { model.playback.value.isPlaying && model.playback.value.isVideo }
                rule.runOnUiThread { model.controller.value?.run { stop(); clearMediaItems() } }
            }
        } finally {
            manager.cancelWorkById(request.id)
            app.library.remove(videoId)
        }
    }
}
