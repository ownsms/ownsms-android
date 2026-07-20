package uz.ownsms.sender.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import uz.ownsms.sender.R

object Notifications {
    const val CHANNEL_ID = "ownsms_sender"
    const val INCOMING_CHANNEL_ID = "ownsms_incoming"
    const val FOREGROUND_ID = 1
    private var incomingId = 1000

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
        if (mgr.getNotificationChannel(INCOMING_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                INCOMING_CHANNEL_ID,
                "Kiruvchi SMS",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Kiruvchi SMS bildirishnomalari" }
            mgr.createNotificationChannel(channel)
        }
    }

    /** Posts a heads-up notification for an incoming SMS — the default SMS app is responsible for it. */
    fun notifyIncoming(context: Context, sender: String?, body: String) {
        ensureChannel(context)
        val n = NotificationCompat.Builder(context, INCOMING_CHANNEL_ID)
            .setContentTitle(sender ?: "SMS")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.ic_launcher)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(incomingId++, n)
    }

    private fun action(context: Context, act: String): PendingIntent {
        val intent = Intent(context, SenderService::class.java).setAction(act)
        return PendingIntent.getService(context, act.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
    }

    fun build(context: Context, text: String, paused: Boolean = false): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ownsms")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        val icon = R.drawable.ic_launcher
        if (paused) {
            builder.addAction(icon, "Davom etish", action(context, SenderService.ACTION_RESUME))
        } else {
            builder.addAction(icon, "Pauza", action(context, SenderService.ACTION_PAUSE))
        }
        builder.addAction(icon, "To'xtatish", action(context, SenderService.ACTION_STOP))
        return builder.build()
    }
}
