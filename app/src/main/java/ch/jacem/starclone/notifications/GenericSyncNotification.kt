package ch.jacem.starclone.notifications

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ch.jacem.starclone.Activities.MainActivity
import ch.jacem.starclone.R

class GenericSyncNotification(var mContext: Context) {

    fun updateGenericNotification(
        title: String,
        content: String,
        icon: Int,
        bigTextArray: ArrayList<String>,
        percent: Int,
        cls: Class<*>?,
        cancelIntent: PendingIntent?,
        channelID: String,
        workerId: String? = null,
    ): NotificationCompat.Builder {
        val bigText = StringBuilder()
        for (i in bigTextArray.indices) {
            bigText.append(bigTextArray[i])
            if (i < 4) {
                bigText.append("\n")
            }
        }
        val foregroundIntent = Intent(mContext, MainActivity::class.java).apply {
            action = MainActivity.MAIN_ACTIVITY_START_PROCESSES
            if (workerId != null) {
                putExtra(MainActivity.EXTRA_HIGHLIGHT_WORKER_ID, workerId)
            }
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            mContext,
            workerId?.hashCode() ?: 0,
            foregroundIntent,
            FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE,
        )


        val notificationBuilder = NotificationCompat.Builder(
            mContext,
            channelID
        )
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText.toString()))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, false)

        if(cancelIntent != null) {
            notificationBuilder.addAction(
                R.drawable.ic_cancel_download,
                mContext.getString(R.string.cancel),
                cancelIntent
            )
        }
        return notificationBuilder


    }

    fun setNotificationChannel(channelID: String, channelName: String, description: String, groupID: String, groupDescription: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val channel = NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_LOW)
            channel.description = description
            // Register the channel with the system
            val notificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannelGroup(NotificationChannelGroup(groupID, groupDescription))
            channel.group = groupID
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        fun  getFlags(): Int {
            return FLAG_IMMUTABLE
        }
    }
}
