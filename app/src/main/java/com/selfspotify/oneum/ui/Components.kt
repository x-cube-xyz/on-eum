package com.selfspotify.oneum.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import coil.compose.AsyncImage
import com.selfspotify.oneum.data.Track
import java.util.Locale

@Composable
fun pageHorizontalPadding(): Dp {
    val width = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    return if (width < 360.dp) 16.dp else 24.dp
}

@Composable
fun Wordmark() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        val color = MaterialTheme.colorScheme.primary
        Canvas(Modifier.size(25.dp)) {
            listOf(.35f, .65f, 1f, .7f, .4f).forEachIndexed { index, height ->
                val x = size.width * (index + .5f) / 5
                drawLine(color, Offset(x, size.height * (1 - height) / 2),
                    Offset(x, size.height * (1 + height) / 2), strokeWidth = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
        Text("on:eum", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
    }
}

@Composable
fun PageHeader(eyebrow: String, title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(eyebrow, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            action?.invoke()
        }
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun Artwork(url: String?, title: String, modifier: Modifier = Modifier, corner: Dp = 12.dp) {
    Box(modifier.clip(RoundedCornerShape(corner)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f), modifier = Modifier.fillMaxSize(.4f))
        if (url != null) AsyncImage(model = url, contentDescription = "$title 앨범 이미지", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, description: String, button: String? = null, onClick: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(68.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(7.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        button?.let {
            Spacer(Modifier.height(14.dp))
            TextButton(onClick = onClick) { Text(it); Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp)) }
        }
    }
}

@Composable
fun TrackRow(track: Track, playing: Boolean, onPlay: () -> Unit, onFavorite: () -> Unit,
             onVideo: () -> Unit, onExport: (Boolean) -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onPlay).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(track.artworkUrl, track.title, Modifier.size(54.dp))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (track.favorite) Icon(Icons.Outlined.Favorite, "좋아요", Modifier.size(11.dp), tint = MaterialTheme.colorScheme.primary)
                Text(formatTime(track.durationSeconds.toLong() * 1000), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (track.videoPath != null) Text("MV · ${track.videoHeight}p", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreHoriz, "${track.title} 메뉴") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(if (track.favorite) "좋아요 취소" else "좋아요에 추가") },
                    onClick = { menu = false; onFavorite() }, leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, null) })
                if (track.videoPath != null) DropdownMenuItem(text = { Text("뮤비 재생") }, onClick = { menu = false; onVideo() },
                    leadingIcon = { Icon(Icons.Outlined.SmartDisplay, null) })
                DropdownMenuItem(text = { Text("음원 파일 내보내기") }, onClick = { menu = false; onExport(false) },
                    leadingIcon = { Icon(Icons.Outlined.FileUpload, null) })
                if (track.videoPath != null) DropdownMenuItem(text = { Text("뮤비 파일 내보내기") }, onClick = { menu = false; onExport(true) },
                    leadingIcon = { Icon(Icons.Outlined.FileUpload, null) })
                DropdownMenuItem(text = { Text("기기에서 삭제", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onDelete() },
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) })
            }
        }
    }
}

fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return if (seconds >= 3600) String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    else String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
