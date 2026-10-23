package com.selfspotify.oneum.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.selfspotify.oneum.MainViewModel
import com.selfspotify.oneum.data.Track
import com.selfspotify.oneum.domain.VideoQuality
import com.selfspotify.oneum.domain.YouTubeLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OneumApp(model: MainViewModel, sharedUrl: StateFlow<String?>, onShareConsumed: () -> Unit) {
    val tracks by model.tracks.collectAsStateWithLifecycle()
    val downloads by model.downloads.collectAsStateWithLifecycle()
    val playback by model.playback.collectAsStateWithLifecycle()
    val controller by model.controller.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val updating by model.updating.collectAsStateWithLifecycle()
    val search by model.search.state.collectAsStateWithLifecycle()
    val shared by sharedUrl.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf("전체") }
    var url by rememberSaveable { mutableStateOf("") }
    var includeVideo by rememberSaveable { mutableStateOf(false) }
    var fullHd by rememberSaveable { mutableStateOf(false) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var deleteTrack by remember { mutableStateOf<Track?>(null) }
    var pendingExport by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val exportAudio = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg")) { destination ->
        val source = pendingExport
        pendingExport = null
        if (destination != null && source != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(destination)?.use { output -> File(source).inputStream().use { it.copyTo(output) } }
                        ?: error("Cannot open export destination")
                }
            }.onSuccess { model.reportMessage("음원 파일을 내보냈습니다.") }.onFailure { model.reportMessage("파일을 내보내지 못했습니다.") }
        }
    }
    val exportVideo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { destination ->
        val source = pendingExport
        pendingExport = null
        if (destination != null && source != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(destination)?.use { output -> File(source).inputStream().use { it.copyTo(output) } }
                        ?: error("Cannot open export destination")
                }
            }.onSuccess { model.reportMessage("뮤비 파일을 내보냈습니다.") }.onFailure { model.reportMessage("파일을 내보내지 못했습니다.") }
        }
    }
    val onExport: (Track, Boolean) -> Unit = { track, video ->
        pendingExport = if (video) track.videoPath else track.audioPath
        val filename = track.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120)
        if (video) exportVideo.launch("$filename.mp4") else exportAudio.launch("$filename.mp3")
    }
    LaunchedEffect(shared) {
        shared?.let { url = it; page = 2; onShareConsumed() }
    }
    LaunchedEffect(message) {
        message?.let { text -> snackbar.showSnackbar(text); model.clearMessage() }
    }

    OneumTheme {
        Scaffold(containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (!WindowInsets.isImeVisible) Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
                    if (playback.trackId != null) MiniPlayer(playback, model) { showPlayer = true }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp,
                        modifier = Modifier.testTag("bottom-navigation"),
                        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)) {
                        val pages = listOf(0, 4, 1, 2, 3)
                        val icons = listOf(Icons.Outlined.Home, Icons.Outlined.Search, Icons.Outlined.LibraryMusic, Icons.Outlined.FileDownload, Icons.Outlined.Tune)
                        listOf("홈", "검색", "보관함", "다운로드", "설정").forEachIndexed { index, label ->
                            NavigationBarItem(selected = page == pages[index], onClick = { page = pages[index] },
                                icon = { Icon(icons[index], label) }, label = { Text(label, style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer))
                        }
                    }
                }
            }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                when (page) {
                    0 -> HomeScreen(tracks, playback, model, { page = 2 }, { liked -> filter = if (liked) "좋아요" else "전체"; page = 1 },
                        { showPlayer = true }, onExport, onSearch = { page = 4 }) { deleteTrack = it }
                    1 -> LibraryScreen(tracks, playback, model, filter, { filter = it }, { page = 2 },
                        { showPlayer = true }, onExport) { deleteTrack = it }
                    2 -> DownloadScreen(model, downloads, url, { url = it }, includeVideo, { includeVideo = it },
                        if (fullHd) VideoQuality.FULL_HD else VideoQuality.HD, { fullHd = it == VideoQuality.FULL_HD },
                        selection = search.results.firstOrNull { it.url == YouTubeLink.parse(url)?.url }, onSearch = { page = 4 }) {
                        if (model.download(url, includeVideo, if (fullHd) VideoQuality.FULL_HD else VideoQuality.HD)) {
                            url = ""
                            if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                != android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    3 -> SettingsScreen(model, tracks, updating)
                    4 -> SearchScreen(search, tracks.map { it.id }.toSet(), updating,
                        model.search::setQuery, model.search::search, model.search::cancel) { video ->
                        url = video.url
                        page = 2
                    }
                }
            }
        }
        if (showPlayer && playback.trackId != null) PlayerSheet(playback, tracks.find { it.id == playback.trackId }, model, controller) { showPlayer = false }
        deleteTrack?.let { track ->
            AlertDialog(onDismissRequest = { deleteTrack = null }, title = { Text("기기에서 삭제할까요?") },
                text = { Text("'${track.title}'의 음원과 뮤비가 보관함 및 기기에서 삭제됩니다.", modifier = Modifier.verticalScroll(rememberScrollState())) },
                confirmButton = { TextButton(onClick = { model.remove(track); deleteTrack = null }) { Text("삭제", color = MaterialTheme.colorScheme.error) } },
                dismissButton = { TextButton(onClick = { deleteTrack = null }) { Text("취소") } })
        }
    }
}
