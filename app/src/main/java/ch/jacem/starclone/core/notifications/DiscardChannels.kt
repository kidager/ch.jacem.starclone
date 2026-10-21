package ch.jacem.starclone.core.notifications

import android.app.NotificationManager
import android.content.Context

class DiscardChannels {
    companion object {
        fun discard(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.deleteNotificationChannel("ch.jacem.starclone.UPLOAD_CHANNEL")
            notificationManager.deleteNotificationChannel("ch.jacem.starclone.DOWNLOAD_CHANNEL")

        }
    }
}