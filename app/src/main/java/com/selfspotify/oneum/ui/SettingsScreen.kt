package com.selfspotify.oneum.ui

import android.content.Intent
import android.provider.Settings
import android.text.format.Formatter
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.selfspotify.oneum.MainViewModel
import com.selfspotify.oneum.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(model: MainViewModel, tracks: List<Track>, updating: Boolean) {
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val dark = isSystemInDarkTheme()
    var licenses by remember { mutableStateOf(false) }
    val usage by produceState(0L, tracks) {
        value = withContext(Dispatchers.IO) { tracks.sumOf { File(it.audioPath).length() + (it.videoPath?.let { path -> File(path).length() } ?: 0L) } }
    }
    LazyColumn(modifier = Modifier.testTag("settings-content"),
        contentPadding = PaddingValues(horizontal = pageHorizontalPadding(), vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { PageHeader("JUST THE WAY YOU LIKE", "설정", "음악을 위한, 작은 취향들.") }
        item {
            SettingsGroup("화면") {
                SettingsRow(if (dark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode, "기기 설정에 맞춘 테마", "현재 ${if (dark) "다크" else "라이트"} 모드")
                Text("다크와 라이트 테마는 기기의 화면 설정에 따라 자동으로 전환됩니다.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingsGroup("로컬 보관함") {
                SettingsRow(Icons.Outlined.Storage, "${tracks.size}곡 · ${Formatter.formatFileSize(context, usage)}", "내 기기에 저장되어 있습니다")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("음원은 MP3, 뮤비는 MP4로 저장합니다. 뮤비는 720p와 1080p를 지원합니다.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("앱을 삭제하면 보관함 파일도 삭제됩니다. 보관할 파일은 음악 메뉴의 '파일 내보내기'로 다른 폴더에 저장해 주세요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingsGroup("다운로드") {
                SettingsRow(Icons.Outlined.SystemUpdateAlt, "다운로드 엔진", "처음 사용하거나 24시간이 지나면 자동 갱신합니다")
                OutlinedButton(onClick = model::updateDownloader, enabled = !updating, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    if (updating) { CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                    Text(if (updating) "업데이트 확인 중…" else "엔진 업데이트 확인")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(Icons.Outlined.Notifications, "다운로드 진행률 알림",
                    if (NotificationManagerCompat.from(context).areNotificationsEnabled()) "알림에 퍼센트와 저장 단계를 표시합니다" else "알림이 꺼져 있습니다. 알림 설정에서 허용해 주세요")
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }, contentPadding = PaddingValues(0.dp)) { Text("알림 설정 열기") }
            }
        }
        item {
            SettingsGroup("온음에 대하여") {
                Wordmark()
                Text("좋아하는 음악을 온전히.\n버전 0.2.1", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { licenses = true }, contentPadding = PaddingValues(0.dp)) { Text("오픈소스 라이선스") }
            }
        }
    }
    if (licenses) AlertDialog(onDismissRequest = { licenses = false }, title = { Text("오픈소스 라이선스") },
        text = { Text("이 앱은 AndroidX / Jetpack Compose / Media3 (Apache 2.0), Coil (Apache 2.0), youtubedl-android (GPL 3.0), yt-dlp (Unlicense 및 포함 구성요소별 라이선스), FFmpeg (빌드 구성에 따른 LGPL / GPL)를 사용합니다. 자세한 고지는 프로젝트의 THIRD_PARTY_NOTICES.md를 확인해 주세요.", modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { licenses = false }) { Text("확인") } },
        dismissButton = { TextButton(onClick = { uri.openUri("https://github.com/yausername/youtubedl-android") }) { Text("다운로더 소스") } })
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Icon(icon, null, Modifier.size(23.dp), tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
