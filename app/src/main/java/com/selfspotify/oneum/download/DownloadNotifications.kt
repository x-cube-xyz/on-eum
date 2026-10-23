package com.selfspotify.oneum.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import com.selfspotify.oneum.MainActivity
import com.selfspotify.oneum.R
import java.util.UUID

data class DownloadProgress(val title: String, val stage: String, val percent: Float? = null) {
    val boundedPercent: Float? get() = percent?.takeIf { it.isFinite() && it >= 0f }?.coerceAtMost(100f)
    val caption: String get() = boundedPercent?.let { "$stage · ${it.toInt()}%" } ?: stage
}

class DownloadNotifications(private val context: Context, private val workId: UUID) {
    init {
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, "다운로드", NotificationManager.IMPORTANCE_LOW))
    }

    fun foreground(progress: DownloadProgress): ForegroundInfo {
        val intent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(progress.title).setSubText("온음 · 다운로드")
            .setContentText(progress.caption).setStyle(NotificationCompat.BigTextStyle().bigText(progress.caption))
            .setContentIntent(intent).setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setProgress(100, progress.boundedPercent?.toInt() ?: 0, progress.boundedPercent == null)
            .addAction(0, "취소", WorkManager.getInstance(context).createCancelPendingIntent(workId)).build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ForegroundInfo(workId.hashCode(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(workId.hashCode(), notification)
    }

    companion object { const val CHANNEL = "downloads" }
}
