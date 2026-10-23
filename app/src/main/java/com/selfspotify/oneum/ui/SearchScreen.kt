package com.selfspotify.oneum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.selfspotify.oneum.search.SearchState
import com.selfspotify.oneum.search.SearchVideo

@Composable
fun SearchScreen(state: SearchState, savedIds: Set<String>, updating: Boolean,
                 onQuery: (String) -> Unit, onSearch: () -> Unit, onCancel: () -> Unit,
                 onSelect: (SearchVideo) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = { keyboard?.hide(); onSearch() }
    LazyColumn(contentPadding = PaddingValues(horizontal = pageHorizontalPadding(), vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { PageHeader("FIND YOUR NEXT FAVORITE", "음악 찾기", "유튜브에서 검색하고, 내 보관함으로 가져오세요.") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = state.query, onValueChange = {
                    onQuery(it)
                    if (it.isBlank()) onCancel()
                }, modifier = Modifier.fillMaxWidth().testTag("youtube-search-query"), singleLine = true,
                    placeholder = { Text("노래 제목, 아티스트, 뮤직비디오", style = MaterialTheme.typography.bodyMedium) },
                    shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) IconButton(onClick = { onQuery(""); onCancel() }) { Icon(Icons.Outlined.Close, "검색어 지우기") }
                    }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (state.query.isNotBlank() && !updating) submit() }))
                Button(onClick = { submit() }, enabled = state.query.isNotBlank() && !updating &&
                    (!state.loading || state.query.trim() != state.submittedQuery),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("youtube-search-button"), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(20.dp)); Spacer(Modifier.width(7.dp)); Text("유튜브 검색")
                }
            }
        }
        when {
            state.loading -> item {
                Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                    Text("유튜브에서 음악을 찾고 있습니다", style = MaterialTheme.typography.titleMedium)
                    Text("처음 검색할 때는 엔진 준비에 시간이 걸릴 수 있습니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onCancel) { Text("검색 취소") }
                }
            }
            state.error != null -> item {
                EmptyState(Icons.Outlined.WifiOff, "검색을 완료하지 못했어요", state.error, "다시 검색", onSearch)
            }
            state.submittedQuery == null -> item {
                EmptyState(Icons.AutoMirrored.Outlined.ManageSearch, "어떤 음악을 찾고 있나요?", "좋아하는 곡이나 아티스트를 검색해 보세요.\n결과에서 영상을 고르면 다운로드할 수 있습니다.")
            }
            state.results.isEmpty() -> item {
                EmptyState(Icons.Outlined.SearchOff, "검색 결과가 없습니다", "다른 제목이나 아티스트로 검색해 보세요.")
            }
            else -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("'${state.submittedQuery}' 검색 결과", style = MaterialTheme.typography.titleMedium)
                        Text("${state.results.size}개 영상 · 가져올 영상을 선택하세요", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(state.results, key = { it.id }) { video -> SearchResultRow(video, video.id in savedIds) { onSelect(video) } }
                item {
                    Text("검색 결과는 최대 20개까지 표시합니다. 원하는 영상이 없으면 검색어를 바꿔 보세요.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(video: SearchVideo, saved: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
        .clickable(onClick = onClick).testTag("search-result-${video.id}").padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            Artwork(video.thumbnail, video.title, Modifier.width(106.dp).height(70.dp), corner = 10.dp)
            video.durationSeconds?.let { duration ->
                Text(formatTime(duration * 1000L), color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                        .background(Ink.copy(alpha = .85f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(video.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(video.channel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (saved) "보관함에 저장됨" else "가져오기", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Icon(Icons.Outlined.ChevronRight, "다운로드 옵션 선택", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
