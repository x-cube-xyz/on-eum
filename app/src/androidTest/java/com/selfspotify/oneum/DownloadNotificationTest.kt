package com.selfspotify.oneum

import android.app.Notification
import androidx.test.platform.app.InstrumentationRegistry
import com.selfspotify.oneum.download.DownloadNotifications
import com.selfspotify.oneum.download.DownloadProgress
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class DownloadNotificationTest {
    @Test fun percentageStageAndProgressBarUseTheSameWorkNotification() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notifications = DownloadNotifications(context, UUID.randomUUID())
        val initial = notifications.foreground(DownloadProgress("음악", "음원 다운로드 중", 0f))
        val updated = notifications.foreground(DownloadProgress("음악", "1080p 뮤비 다운로드 중", 47.2f))
        val extras = updated.notification.extras
        assertEquals(initial.notificationId, updated.notificationId)
        assertEquals("1080p 뮤비 다운로드 중 · 47%", extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(47, extras.getInt(Notification.EXTRA_PROGRESS))
        assertEquals(100, extras.getInt(Notification.EXTRA_PROGRESS_MAX))
        assertFalse(extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
        val processing = notifications.foreground(DownloadProgress("음악", "음원 변환 및 저장 중"))
        assertTrue(processing.notification.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
        assertTrue(updated.notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
        assertEquals("취소", updated.notification.actions.single().title.toString())
    }
}
