package com.selfspotify.oneum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView
import com.selfspotify.oneum.MainViewModel
import com.selfspotify.oneum.PlaybackState
import com.selfspotify.oneum.data.Track

@Composable
fun MiniPlayer(state: PlaybackState, model: MainViewModel, onOpen: () -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).testTag("mini-player").padding(start = 9.dp, end = 5.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(state.artworkUrl, state.title, Modifier.size(43.dp), corner = 8.dp)
            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                Text(state.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = model::togglePlay) { Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (state.isPlaying) "일시정지" else "재생", Modifier.size(29.dp)) }
            IconButton(onClick = model::next, modifier = Modifier.size(40.dp)) { Icon(Icons.Filled.SkipNext, "다음 곡") }
        }
        LinearProgressIndicator(progress = { if (state.duration > 0) state.position.toFloat() / state.duration else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp), trackColor = MaterialTheme.colorScheme.outlineVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerSheet(state: PlaybackState, track: Track?, model: MainViewModel, controller: MediaController?, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val systemInsets = WindowInsets.safeDrawing
    val sheetHeight = with(density) {
        (windowHeight - systemInsets.getTop(this).toDp() - systemInsets.getBottom(this).toDp()).coerceAtLeast(1.dp) * .94f
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background, dragHandle = null,
        contentWindowInsets = { WindowInsets.safeDrawing }) {
        BoxWithConstraints(Modifier.fillMaxWidth().heightIn(max = sheetHeight)) {
            val horizontalPadding = pageHorizontalPadding()
            val contentWidth = (maxWidth - horizontalPadding * 2).coerceAtLeast(0.dp)
            val coverSize = minOf(contentWidth, maxHeight * .42f)
            val videoWidth = minOf(contentWidth, maxHeight * .42f * (16f / 9f))
            Column(Modifier.fillMaxSize().padding(horizontal = horizontalPadding)) {
                PlayerHeader(state, track, model, onDismiss)
                LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("player-content"),
                    contentPadding = PaddingValues(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    item {
                        if (state.isVideo && controller != null) {
                            AndroidView(factory = { context -> PlayerView(context).apply { useController = false; player = controller } },
                                update = { it.player = controller }, onRelease = { it.player = null },
                                modifier = Modifier.width(videoWidth).aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp)))
                        } else Artwork(state.artworkUrl, state.title, Modifier.size(coverSize), corner = 24.dp)
                    }
                    item {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(state.title, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(state.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    item { PlaybackPosition(state, model) }
                    item { PlaybackButtons(state, model) }
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.OfflinePin, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("내 기기에서 재생 중", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (track?.videoPath != null) TextButton(onClick = { model.play(track, !state.isVideo) }) {
                                Text(if (state.isVideo) "음원으로 듣기" else "뮤비 보기")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerHeader(state: PlaybackState, track: Track?, model: MainViewModel, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDismiss) { Icon(Icons.Outlined.KeyboardArrowDown, "재생 화면 닫기", Modifier.size(28.dp)) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (state.isVideo) "NOW WATCHING" else "NOW PLAYING", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("나의 보관함에서", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { track?.let(model::favorite) }, enabled = track != null) {
            Icon(if (track?.favorite == true) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                if (track?.favorite == true) "좋아요 취소" else "좋아요 추가", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PlaybackPosition(state: PlaybackState, model: MainViewModel) {
    var seek by remember(state.trackId) { mutableStateOf<Float?>(null) }
    Column(Modifier.fillMaxWidth()) {
        Slider(value = seek ?: state.position.toFloat().coerceIn(0f, state.duration.toFloat().coerceAtLeast(1f)),
            onValueChange = { seek = it }, onValueChangeFinished = { seek?.let { model.seekTo(it.toLong()) }; seek = null },
            valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f), enabled = state.duration > 0)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(seek?.toLong() ?: state.position), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTime(state.duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlaybackButtons(state: PlaybackState, model: MainViewModel) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 280.dp
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (compact) Arrangement.SpaceEvenly else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                if (!compact) ShuffleButton(state, model)
                IconButton(onClick = model::previous) { Icon(Icons.Filled.SkipPrevious, "이전 곡", Modifier.size(35.dp)) }
                FilledIconButton(onClick = model::togglePlay, modifier = Modifier.size(70.dp).testTag("player-play-button"), shape = CircleShape) {
                    Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (state.isPlaying) "일시정지" else "재생", Modifier.size(37.dp))
                }
                IconButton(onClick = model::next) { Icon(Icons.Filled.SkipNext, "다음 곡", Modifier.size(35.dp)) }
                if (!compact) RepeatButton(state, model)
            }
            if (compact) Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ShuffleButton(state, model)
                RepeatButton(state, model)
            }
        }
    }
}

@Composable
private fun ShuffleButton(state: PlaybackState, model: MainViewModel) {
    IconButton(onClick = model::toggleShuffle) {
        Icon(Icons.Outlined.Shuffle, "셔플 재생", tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RepeatButton(state: PlaybackState, model: MainViewModel) {
    IconButton(onClick = model::toggleRepeat) {
        Icon(if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat, "반복 재생",
            tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
