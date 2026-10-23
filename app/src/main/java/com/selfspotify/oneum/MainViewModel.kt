package com.selfspotify.oneum

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.selfspotify.oneum.data.Track
import com.selfspotify.oneum.domain.VideoQuality
import com.selfspotify.oneum.domain.YouTubeLink
import com.selfspotify.oneum.download.DownloadWorker
import com.selfspotify.oneum.download.DownloaderRuntime
import com.selfspotify.oneum.playback.PlaybackService
import com.selfspotify.oneum.search.SearchController
import com.selfspotify.oneum.search.YouTubeSearchRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class PlaybackState(
    val trackId: String? = null,
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val isVideo: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as OneumApplication
    val library = app.library
    val tracks = library.tracks
    val search = SearchController(YouTubeSearchRepository(app), viewModelScope)
    private val workManager = WorkManager.getInstance(application)
    val downloads = workManager.getWorkInfosByTagFlow(DownloadWorker.TAG)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val mutablePlayback = MutableStateFlow(PlaybackState())
    val playback = mutablePlayback.asStateFlow()
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message = mutableMessage.asStateFlow()
    private val mutableUpdating = MutableStateFlow(false)
    val updating = mutableUpdating.asStateFlow()
    private val mutableController = MutableStateFlow<MediaController?>(null)
    val controller = mutableController.asStateFlow()
    private val controllerFuture = MediaController.Builder(application,
        SessionToken(application, ComponentName(application, PlaybackService::class.java))).buildAsync()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refreshPlayback()
        override fun onPlayerError(error: PlaybackException) {
            mutableMessage.value = "파일을 재생할 수 없습니다. 보관함의 파일 상태를 확인해 주세요."
        }
    }

    init {
        controllerFuture.addListener({
            runCatching {
                mutableController.value = controllerFuture.get().also { it.addListener(listener) }
                refreshPlayback()
            }.onFailure { mutableMessage.value = "재생기를 연결하지 못했습니다. 앱을 다시 실행해 주세요." }
        }, ContextCompat.getMainExecutor(application))
        viewModelScope.launch {
            while (true) { refreshPlayback(); delay(500) }
        }
    }

    fun download(input: String, includeVideo: Boolean, quality: VideoQuality): Boolean {
        if (mutableUpdating.value) {
            mutableMessage.value = "다운로드 엔진 업데이트가 끝난 뒤 다시 시도해 주세요."
            return false
        }
        val link = YouTubeLink.parse(input)
        if (link == null) {
            mutableMessage.value = "유튜브 영상 링크를 확인해 주세요."
            return false
        }
        val active = downloads.value.any { !it.state.isFinished && "video:${link.videoId}" in it.tags }
        if (active) {
            mutableMessage.value = "이미 대기 중이거나 다운로드 중인 영상입니다."
            return false
        }
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf("url" to link.url, "includeVideo" to includeVideo, "height" to quality.height))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(DownloadWorker.TAG).addTag("video:${link.videoId}")
            .addTag("quality:${if (includeVideo) quality.height else 0}")
            .addTag("created:${System.currentTimeMillis()}").build()
        workManager.enqueueUniqueWork("download:${link.videoId}", ExistingWorkPolicy.KEEP, request)
        mutableMessage.value = "다운로드 목록에 추가했습니다."
        return true
    }

    fun cancelDownload(id: UUID) { workManager.cancelWorkById(id) }
    fun clearDownloadHistory() { workManager.pruneWork() }
    fun retryDownload(info: WorkInfo) {
        val id = info.tags.firstOrNull { it.startsWith("video:") }?.substringAfter(':') ?: return
        val height = info.tags.firstOrNull { it.startsWith("quality:") }?.substringAfter(':')?.toIntOrNull() ?: 0
        download("https://youtu.be/$id", height != 0, if (height == 1080) VideoQuality.FULL_HD else VideoQuality.HD)
    }

    fun play(track: Track, video: Boolean = false) {
        val player = mutableController.value ?: return
        val path = if (video) track.videoPath else track.audioPath
        if (path == null || !File(path).isFile) {
            mutableMessage.value = "저장된 파일을 찾을 수 없습니다. 다시 다운로드해 주세요."
            return
        }
        val queue = if (video) listOf(track) else tracks.value
        player.setMediaItems(queue.map { item ->
            MediaItem.Builder().setMediaId(item.id)
                .setUri(Uri.fromFile(File(if (video) item.videoPath!! else item.audioPath)))
                .setMediaMetadata(MediaMetadata.Builder().setTitle(item.title).setArtist(item.artist)
                    .setArtworkUri(item.artworkUrl?.let(Uri::parse))
                    .setExtras(Bundle().apply { putBoolean("video", video) }).build()).build()
        }, queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0), 0)
        player.prepare(); player.play()
        viewModelScope.launch { library.markPlayed(track.id) }
    }

    fun togglePlay() { mutableController.value?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun previous() { mutableController.value?.seekToPrevious() }
    fun next() { mutableController.value?.seekToNext() }
    fun seekTo(position: Long) { mutableController.value?.seekTo(position) }
    fun toggleShuffle() { mutableController.value?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun toggleRepeat() { mutableController.value?.let { it.repeatMode = (it.repeatMode + 1) % 3 } }
    fun favorite(track: Track) { viewModelScope.launch { library.toggleFavorite(track.id) } }
    fun remove(track: Track) {
        if (downloads.value.any { !it.state.isFinished && "video:${track.id}" in it.tags }) {
            mutableMessage.value = "이 음악의 다운로드가 끝나거나 취소된 뒤 삭제해 주세요."
            return
        }
        if (mutablePlayback.value.trackId == track.id) mutableController.value?.run { stop(); clearMediaItems() }
        viewModelScope.launch {
            runCatching { library.remove(track.id) }
                .onFailure { mutableMessage.value = "파일을 삭제하지 못했습니다. 다시 시도해 주세요." }
        }
    }

    fun updateDownloader() {
        if (mutableUpdating.value) return
        if (downloads.value.any { !it.state.isFinished } || search.state.value.loading) {
            mutableMessage.value = "검색 또는 다운로드가 끝난 뒤 엔진을 업데이트해 주세요."
            return
        }
        viewModelScope.launch {
            mutableUpdating.value = true
            try {
                val version = DownloaderRuntime.update(app)
                mutableMessage.value = "다운로드 엔진을 업데이트했습니다. ($version)"
            } catch (error: Exception) {
                mutableMessage.value = "업데이트하지 못했습니다. 네트워크 연결을 확인해 주세요."
            } finally { mutableUpdating.value = false }
        }
    }

    fun clearMessage() { mutableMessage.value = null }
    fun reportMessage(message: String) { mutableMessage.value = message }

    private fun refreshPlayback() {
        val player = mutableController.value ?: return
        val metadata = player.mediaMetadata
        mutablePlayback.value = PlaybackState(
            trackId = player.currentMediaItem?.mediaId,
            title = metadata.title?.toString().orEmpty(), artist = metadata.artist?.toString().orEmpty(),
            artworkUrl = metadata.artworkUri?.toString(), isPlaying = player.isPlaying,
            isVideo = metadata.extras?.getBoolean("video") ?: false,
            position = player.currentPosition.coerceAtLeast(0), duration = player.duration.coerceAtLeast(0),
            shuffle = player.shuffleModeEnabled, repeatMode = player.repeatMode,
        )
    }

    override fun onCleared() {
        mutableController.value?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}
