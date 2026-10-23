package com.selfspotify.oneum.download

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import com.selfspotify.oneum.domain.DownloadErrors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object DownloaderRuntime {
    private val mutex = Mutex()
    private val admission = Mutex()
    private val activeRequests = MutableStateFlow(0)
    private const val UPDATE_INTERVAL = 24 * 60 * 60 * 1000L
    private const val RETRY_INTERVAL = 10 * 60 * 1000L
    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        mutex.withLock {
            YoutubeDL.init(context)
            FFmpeg.init(context)
        }
    }

    suspend fun update(context: Context): String = withContext(Dispatchers.IO) {
        initialize(context)
        admission.withLock {
            activeRequests.first { it == 0 }
            refresh(context)
            YoutubeDL.version(context) ?: "최신 버전"
        }
    }

    suspend fun execute(
        context: Context,
        request: () -> YoutubeDLRequest,
        processId: String,
        onStatus: (String) -> Unit = {},
        callback: ((Float, Long, String) -> Unit)? = null,
    ): YoutubeDLResponse = withContext(Dispatchers.IO) {
        initialize(context)
        repeat(2) { attempt ->
            admission.withLock {
                val prefs = context.getSharedPreferences("engine_updates", Context.MODE_PRIVATE)
                val now = System.currentTimeMillis()
                if (attempt > 0 || (now - prefs.getLong("checked", 0) >= UPDATE_INTERVAL &&
                            now - prefs.getLong("attempted", 0) >= RETRY_INTERVAL)) {
                    onStatus(if (attempt > 0) "엔진을 갱신하고 다시 시도하는 중" else "최신 다운로드 엔진 확인 중")
                    activeRequests.first { it == 0 }
                    try { refresh(context) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) {
                        if (attempt > 0) throw IllegalStateException("engine update failed", error)
                    }
                }
                activeRequests.update { it + 1 }
            }
            try {
                onStatus("유튜브 요청 처리 중")
                return@withContext runInterruptible { YoutubeDL.execute(request(), processId, callback) }
            } catch (cancelled: CancellationException) {
                YoutubeDL.destroyProcessById(processId)
                throw cancelled
            } catch (error: Exception) {
                if (attempt > 0 || !DownloadErrors.shouldRefreshEngine(error.message.orEmpty())) throw error
            } finally { activeRequests.update { it - 1 } }
        }
        error("YouTube request did not complete")
    }

    private suspend fun refresh(context: Context) {
        val prefs = context.getSharedPreferences("engine_updates", Context.MODE_PRIVATE)
        prefs.edit().putLong("attempted", System.currentTimeMillis()).apply()
        runInterruptible { YoutubeDL.updateYoutubeDL(context) }
        prefs.edit().putLong("checked", System.currentTimeMillis()).apply()
    }
}
