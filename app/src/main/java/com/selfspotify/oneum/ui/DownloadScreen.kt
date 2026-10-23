package com.selfspotify.oneum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import com.selfspotify.oneum.MainViewModel
import com.selfspotify.oneum.domain.VideoQuality
import com.selfspotify.oneum.domain.YouTubeLink
import com.selfspotify.oneum.domain.DownloadErrors
import com.selfspotify.oneum.search.SearchVideo

@Suppress("DEPRECATION")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadScreen(model: MainViewModel, downloads: List<WorkInfo>, url: String, onUrl: (String) -> Unit,
                   includeVideo: Boolean, onIncludeVideo: (Boolean) -> Unit,
                   quality: VideoQuality, onQuality: (VideoQuality) -> Unit, selection: SearchVideo? = null, onSearch: () -> Unit = {}, onDownload: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val invalid = url.isNotBlank() && YouTubeLink.parse(url) == null
    val sorted = downloads.sortedByDescending { it.tags.firstOrNull { tag -> tag.startsWith("created:") }?.substringAfter(':')?.toLongOrNull() ?: 0 }
        .sortedBy { it.state.isFinished }.take(30)
    LazyColumn(modifier = Modifier.testTag("download-content"), contentPadding = PaddingValues(horizontal = pageHorizontalPadding(), vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { PageHeader("MAKE IT YOURS", "음악 가져오기", "링크를 붙여넣거나 유튜브에서 음악을 찾아보세요.") }
        item {
            OutlinedButton(onClick = onSearch, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("유튜브에서 검색")
            }
        }
        if (selection != null) item {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Artwork(selection.thumbnail, selection.title, Modifier.size(56.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("선택한 영상", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text(selection.title, style = MaterialTheme.typography.labelLarge, maxLines = 2)
                    Text(selection.channel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(17.dp)) {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.align(Alignment.CenterVertically), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Link, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(9.dp)); Text("YouTube 링크", style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(modifier = Modifier.align(Alignment.CenterVertically), onClick = {
                        val pasted = clipboard.getText()?.text.orEmpty()
                        if (pasted.isBlank()) model.reportMessage("클립보드에 복사된 링크가 없습니다.")
                        else onUrl(YouTubeLink.fromSharedText(pasted)?.url ?: pasted.trim())
                    }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("붙여넣기") }
                }
                OutlinedTextField(value = url, onValueChange = onUrl, modifier = Modifier.fillMaxWidth().testTag("youtube-url"), singleLine = true,
                    placeholder = { Text("https://youtu.be/…", style = MaterialTheme.typography.bodyMedium) },
                    shape = RoundedCornerShape(13.dp), isError = invalid,
                    supportingText = if (invalid) ({ Text("유튜브 영상 링크를 입력해 주세요.") }) else null,
                    trailingIcon = { if (url.isNotBlank()) IconButton(onClick = { onUrl("") }) { Icon(Icons.Outlined.Close, "링크 지우기") } })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(39.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("음원 저장", style = MaterialTheme.typography.labelLarge)
                        Text("MP3 · 항상 포함", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.CheckCircle, "항상 포함", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(39.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.SmartDisplay, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("뮤비도 함께 저장", style = MaterialTheme.typography.labelLarge)
                        Text("MP4 · 선택 사항", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = includeVideo, onCheckedChange = onIncludeVideo, modifier = Modifier.testTag("include-video"))
                }
                if (includeVideo) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        VideoQuality.entries.forEach { option ->
                            FilterChip(selected = quality == option, onClick = { onQuality(option) },
                                label = { Text("${option.height}p${if (option == VideoQuality.FULL_HD) " · Full HD" else " · HD"}") },
                                leadingIcon = if (quality == option) ({ Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) }) else null)
                        }
                    }
                    Text("선택한 화질이 제공되는 영상만 저장할 수 있습니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onDownload, enabled = YouTubeLink.parse(url) != null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("download-button"), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Outlined.FileDownload, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("다운로드")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.PhoneAndroid, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(5.dp))
                    Text("내 기기 · 온음 보관함에 저장", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("다운로드 목록", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (downloads.any { it.state.isFinished }) TextButton(onClick = model::clearDownloadHistory) { Text("기록 지우기", style = MaterialTheme.typography.labelMedium) }
            }
        }
        if (sorted.isEmpty()) item {
            EmptyState(Icons.Outlined.Downloading, "저장할 음악을 추가해 보세요", "다운로드 진행 상황을 여기서 확인할 수 있습니다.")
        } else items(sorted, key = { it.id }) { info -> DownloadRow(info, model) }
    }
}

@Composable
private fun DownloadRow(info: WorkInfo, model: MainViewModel) {
    val active = !info.state.isFinished
    val title = info.progress.getString("title") ?: info.outputData.getString("title") ?: "YouTube 음악"
    val stage = when (info.state) {
        WorkInfo.State.ENQUEUED -> "대기 중 · 네트워크 연결이 필요합니다"
        WorkInfo.State.BLOCKED -> "대기 중"
        WorkInfo.State.RUNNING -> info.progress.getString("stage") ?: "다운로드 준비 중"
        WorkInfo.State.SUCCEEDED -> "보관함에 저장 완료"
        WorkInfo.State.CANCELLED -> "취소됨 · 저장이 끝난 음원은 보관함에 유지됩니다"
        WorkInfo.State.FAILED -> info.outputData.getString("error")?.let { error ->
            if (error.contains("WARNING:") || error.contains("ERROR:")) DownloadErrors.message(error) else error
        } ?: "다운로드 실패"
    }
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(17.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(when (info.state) { WorkInfo.State.SUCCEEDED -> Icons.Outlined.CheckCircle; WorkInfo.State.FAILED -> Icons.Outlined.ErrorOutline; else -> Icons.Outlined.FileDownload },
                null, Modifier.size(21.dp), tint = if (info.state == WorkInfo.State.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 2, modifier = Modifier.weight(1f))
            if (active) IconButton(onClick = { model.cancelDownload(info.id) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Close, "다운로드 취소") }
            else if (info.state != WorkInfo.State.SUCCEEDED) IconButton(onClick = { model.retryDownload(info) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Refresh, "다시 다운로드") }
        }
        if (info.state == WorkInfo.State.RUNNING) {
            val percent = info.progress.getFloat("percent", 0f)
            val determinate = !info.progress.getBoolean("indeterminate", true)
            if (determinate) LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
            else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("$stage${if (determinate) " · ${percent.toInt()}%" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else Text(stage, style = MaterialTheme.typography.bodySmall,
            color = if (info.state == WorkInfo.State.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
