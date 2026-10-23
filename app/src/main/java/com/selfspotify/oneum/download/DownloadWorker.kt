package com.selfspotify.oneum.download

import android.content.Context
import android.os.SystemClock
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.selfspotify.oneum.OneumApplication
import com.selfspotify.oneum.data.Track
import com.selfspotify.oneum.domain.DownloadFormats
import com.selfspotify.oneum.domain.DownloadErrors
import com.selfspotify.oneum.domain.VideoQuality
import com.selfspotify.oneum.domain.YouTubeLink
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.coroutineContext

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val library get() = (applicationContext as OneumApplication).library
    private val processId = id.toString()
    private var lastProgressAt = 0L
    private var lastStage = ""
    private val notifications by lazy { DownloadNotifications(applicationContext, id) }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val link = YouTubeLink.parse(inputData.getString("url").orEmpty())
            ?: return@withContext Result.failure(workDataOf("error" to "올바른 유튜브 영상 링크를 입력해 주세요."))
        var audioSaved = false
        var title = "영상 정보를 확인하고 있습니다"
        try {
            setForeground(notifications.foreground(DownloadProgress(title, "다운로드 준비 중")))
            progress(title, "다운로드 엔진 준비 중")
            library.load()
            coroutineContext.ensureActive()

            val response = DownloaderRuntime.execute(applicationContext, request = {
                YoutubeDLRequest(link.url).apply {
                    addOption("--dump-single-json"); addOption("--skip-download"); addOption("--no-playlist")
                    addOption("--socket-timeout", "30")
                }
            }, processId = processId, onStatus = { progress(title, it) })
            val info = JSONObject(response.out)
            title = (info.optString("track").takeIf { it.isNotBlank() } ?: info.optString("title", "제목 없는 음악")).take(240)
            val artist = info.optString("artist").takeIf { it.isNotBlank() && it != "null" }
                ?: info.optString("uploader", "알 수 없는 아티스트")
            val directory = File(library.mediaDirectory, link.videoId).apply { mkdirs() }

            progress(title, "음원 다운로드 중", 0f)
            DownloaderRuntime.execute(applicationContext, request = { request(link, File(directory, "audio.%(ext)s")).apply {
                addOption("-f", "bestaudio/best")
                addOption("--extract-audio"); addOption("--audio-format", "mp3"); addOption("--audio-quality", "0")
                addOption("--embed-metadata")
            } }, processId = processId,
                onStatus = { progress(title, if (it == "유튜브 요청 처리 중") "음원 다운로드 중" else it) },
                callback = { percent, _, line ->
                    val processing = line.contains("[ExtractAudio]") || line.contains("[Metadata]")
                    progress(title, if (processing) "음원 변환 및 저장 중" else "음원 다운로드 중", if (processing) null else percent)
                })
            coroutineContext.ensureActive()
            val audio = File(directory, "audio.mp3")
            check(audio.isFile && audio.length() > 0) { "저장된 음원 파일을 확인할 수 없습니다." }
            val previous = library.tracks.value.find { it.id == link.videoId }
            var track = Track(
                id = link.videoId, title = title, artist = artist,
                durationSeconds = info.optDouble("duration", 0.0).toInt(), sourceUrl = link.url,
                audioPath = audio.absolutePath, artworkUrl = info.optString("thumbnail").takeIf { it.isNotBlank() },
                videoPath = previous?.videoPath, videoHeight = previous?.videoHeight,
            )
            library.upsert(track)
            audioSaved = true

            if (inputData.getBoolean("includeVideo", false)) {
                val quality = if (inputData.getInt("height", 720) == 1080) VideoQuality.FULL_HD else VideoQuality.HD
                progress(title, "${quality.height}p 뮤비 다운로드 중", 0f)
                DownloaderRuntime.execute(applicationContext, request = { request(link, File(directory, "video-${quality.height}.%(ext)s")).apply {
                    addOption("-f", DownloadFormats.video(quality))
                    addOption("--merge-output-format", "mp4")
                    addOption("--remux-video", "mp4")
                } }, processId = processId,
                    onStatus = { progress(title, if (it == "유튜브 요청 처리 중") "${quality.height}p 뮤비 다운로드 중" else it) },
                    callback = { percent, _, line ->
                        val processing = line.contains("[Merger]") || line.contains("[VideoRemuxer]")
                        progress(title, if (processing) "${quality.height}p 뮤비 병합 및 저장 중" else "${quality.height}p 뮤비 다운로드 중", if (processing) null else percent)
                    })
                coroutineContext.ensureActive()
                val video = File(directory, "video-${quality.height}.mp4")
                check(video.isFile && video.length() > 0) { "저장된 뮤비 파일을 확인할 수 없습니다." }
                track = track.copy(videoPath = video.absolutePath, videoHeight = quality.height)
                library.upsert(track)
            }
            Result.success(workDataOf("title" to title, "message" to "보관함에 저장했습니다."))
        } catch (cancelled: CancellationException) {
            YoutubeDL.destroyProcessById(processId)
            throw cancelled
        } catch (error: Exception) {
            val detail = error.message.orEmpty()
            val message = DownloadErrors.message(detail)
            Result.failure(workDataOf(
                "title" to title,
                "error" to if (audioSaved) "음원은 저장했습니다. 뮤비 다운로드 실패: $message" else message,
                "audioSaved" to audioSaved,
                "detail" to detail.takeLast(1800),
            ))
        } finally {
            if (isStopped) YoutubeDL.destroyProcessById(processId)
        }
    }

    private fun request(link: YouTubeLink, output: File) = YoutubeDLRequest(link.url).apply {
        addOption("--no-playlist"); addOption("--no-overwrites")
        addOption("--newline"); addOption("--retries", "3")
        addOption("--socket-timeout", "30")
        addOption("-o", output.absolutePath)
    }

    private fun progress(title: String, stage: String, percent: Float? = null) {
        if (isStopped) return
        val snapshot = DownloadProgress(title, stage, percent)
        val now = SystemClock.elapsedRealtime()
        if (stage == lastStage && now - lastProgressAt < 750 && snapshot.boundedPercent != 100f) return
        lastProgressAt = now
        lastStage = stage
        setProgressAsync(workDataOf("title" to title, "stage" to stage, "percent" to (snapshot.boundedPercent ?: 0f),
            "indeterminate" to (snapshot.boundedPercent == null)))
        setForegroundAsync(notifications.foreground(snapshot))
    }

    override suspend fun getForegroundInfo() = notifications.foreground(DownloadProgress("온음", "다운로드 준비 중"))

    companion object { const val TAG = "oneum_download" }
}
