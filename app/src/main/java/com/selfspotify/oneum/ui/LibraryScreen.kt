package com.selfspotify.oneum.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.selfspotify.oneum.MainViewModel
import com.selfspotify.oneum.PlaybackState
import com.selfspotify.oneum.data.Track

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(tracks: List<Track>, playback: PlaybackState, model: MainViewModel,
                  filter: String, onFilter: (String) -> Unit, onDownload: () -> Unit, onPlayer: () -> Unit,
                  onExport: (Track, Boolean) -> Unit, onDelete: (Track) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var alphabetical by rememberSaveable { mutableStateOf(false) }
    val visible = tracks.filter { track ->
        (filter != "좋아요" || track.favorite) && (filter != "뮤비" || track.videoPath != null) &&
            (query.isBlank() || track.title.contains(query.trim(), true) || track.artist.contains(query.trim(), true))
    }.let { if (alphabetical) it.sortedBy { track -> track.title.lowercase() } else it }
    LazyColumn(contentPadding = PaddingValues(horizontal = pageHorizontalPadding(), vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeader("MY COLLECTION", "나의 보관함", "좋아하는 음악이 모이는 곳.") }
        item {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), placeholder = { Text("음악, 아티스트 검색") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) }, trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "검색 지우기") }
                })
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("전체", "좋아요", "뮤비").forEach { value -> FilterChip(selected = filter == value, onClick = { onFilter(value) }, label = { Text(value) },
                    leadingIcon = if (filter == value) ({ Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) }) else null) }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${visible.size}곡", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                TextButton(onClick = { alphabetical = !alphabetical }) {
                    Icon(Icons.AutoMirrored.Outlined.Sort, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text(if (alphabetical) "이름순" else "최근 추가순")
                }
            }
        }
        if (visible.isEmpty()) item {
            EmptyState(Icons.Outlined.LibraryMusic, if (query.isNotEmpty()) "검색 결과가 없습니다" else "아직 저장한 음악이 없어요",
                if (query.isNotEmpty()) "다른 제목이나 아티스트로 검색해 보세요." else "음악을 가져오면 오프라인에서도 감상할 수 있습니다.",
                if (query.isEmpty()) "음악 가져오기" else null, onDownload)
        } else items(visible, key = { it.id }) { track ->
            TrackRow(track, playback.trackId == track.id, onPlay = { model.play(track); onPlayer() },
                onFavorite = { model.favorite(track) }, onVideo = { model.play(track, true); onPlayer() },
                onExport = { onExport(track, it) }, onDelete = { onDelete(track) })
        }
    }
}
