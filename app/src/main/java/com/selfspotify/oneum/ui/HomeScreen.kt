package com.selfspotify.oneum.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import com.selfspotify.oneum.MainViewModel
import com.selfspotify.oneum.PlaybackState
import com.selfspotify.oneum.data.Track

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(tracks: List<Track>, playback: PlaybackState, model: MainViewModel,
               onDownload: () -> Unit, onLibrary: (Boolean) -> Unit, onPlayer: () -> Unit,
               onExport: (Track, Boolean) -> Unit, onSearch: () -> Unit, onDelete: (Track) -> Unit) {
    val horizontalPadding = pageHorizontalPadding()
    LazyColumn(modifier = Modifier.testTag("home-content"), contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, top = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.align(Alignment.CenterVertically)) { Wordmark() }
                Row(Modifier.align(Alignment.CenterVertically).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Text("LOCAL PLAYER", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("음악은 가까이,\n취향은 온전히.", style = MaterialTheme.typography.headlineLarge)
                Text("좋아하는 음악을 모아, 어디서든 들어보세요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { CollectionHero(onDownload) }
        item {
            OutlinedButton(onClick = onSearch, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("유튜브에서 음악 찾기")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CollectionCard("모든 음악", tracks.size, Icons.Outlined.LibraryMusic, Modifier.weight(1f)) { onLibrary(false) }
                CollectionCard("좋아요", tracks.count { it.favorite }, Icons.Outlined.FavoriteBorder, Modifier.weight(1f)) { onLibrary(true) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("최근 추가한 음악", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { onLibrary(false) }, contentPadding = PaddingValues(0.dp)) {
                    Text("모두 보기", style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp))
                }
            }
        }
        if (tracks.isEmpty()) item {
            Box(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))) {
                EmptyState(Icons.AutoMirrored.Outlined.QueueMusic, "첫 번째 음악을 기다리고 있어요", "유튜브 링크로 나만의 보관함을 채워보세요.", "음악 가져오기", onDownload)
            }
        } else items(tracks.take(5), key = { it.id }) { track ->
            TrackRow(track, playback.trackId == track.id,
                onPlay = { model.play(track); onPlayer() }, onFavorite = { model.favorite(track) },
                onVideo = { model.play(track, true); onPlayer() }, onExport = { onExport(track, it) }, onDelete = { onDelete(track) })
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.OfflinePin, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text("내 기기에 저장하고, 내 리듬대로.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CollectionHero(onDownload: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min = 228.dp).clip(RoundedCornerShape(24.dp)).background(Lime)) {
        if (maxWidth >= 320.dp && fontScale <= 1.15f) Canvas(Modifier.size(225.dp).align(Alignment.CenterEnd).offset(x = 72.dp, y = 18.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2
            drawCircle(Ink, radius, center)
            repeat(8) { index -> drawCircle(Color(0xFF343D2C), radius * (.42f + index * .065f), center, style = Stroke(1.dp.toPx())) }
            drawCircle(Color(0xFFD9E8BE), radius * .28f, center)
            drawCircle(Ink, radius * .055f, center)
            drawLine(Color(0xFF657451), Offset(center.x - radius * .48f, center.y - radius * .82f),
                Offset(center.x - radius * .17f, center.y - radius * .32f), strokeWidth = 2.dp.toPx())
        }
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("YOUR OWN COLLECTION", color = Ink.copy(alpha = .65f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("좋아하는 순간을\n소장하는 방법.", color = Ink, fontSize = 23.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp,
                    modifier = Modifier.testTag("home-hero-title"))
            }
            Button(onClick = onDownload, colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
                modifier = Modifier.testTag("home-download-button"), contentPadding = PaddingValues(horizontal = 17.dp, vertical = 12.dp), shape = CircleShape) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp)); Text("음악 가져오기", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CollectionCard(title: String, count: Int, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(23.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.ArrowOutward, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("${count}곡", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
