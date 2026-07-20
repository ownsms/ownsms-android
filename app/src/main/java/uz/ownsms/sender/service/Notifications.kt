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
        if (paused) {
            builder.addAction(0, "Davom etish", action(context, SenderService.ACTION_RESUME))
        } else {
            builder.addAction(0, "Pauza", action(context, SenderService.ACTION_PAUSE))
        }
        builder.addAction(0, "To'xtatish", action(context, SenderService.ACTION_STOP))
        return builder.build()
    }
}
