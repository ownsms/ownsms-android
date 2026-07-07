package uz.ownsms.sender.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import uz.ownsms.sender.R

object Notifications {
    const val CHANNEL_ID = "ownsms_sender"
    const val FOREGROUND_ID = 1

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ownsms sender",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "SMS yuborish xizmati holati" }
            mgr.createNotificationChannel(channel)
        }
    }

    fun build(context: Context, text: String): Notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("ownsms")
        .setContentText(text)
        .setSmallIcon(R.drawable.ic_launcher)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build()
}
