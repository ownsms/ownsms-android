package uz.ownsms.sender.sms

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager

/** Sends SMS through a chosen SIM via [SmsManager], wiring sent/delivered result PendingIntents. */
class SmsSender(private val context: Context) {

    /**
     * Sends [text] to [to] from the given SIM (subscriptionId) or the system default SIM.
     * Multipart-safe. Results arrive at [SmsResultReceiver] (per part).
     */
    @Suppress("DEPRECATION")
    fun send(jobId: Long, to: String, text: String, subscriptionId: Int?) {
        val sms = if (subscriptionId != null && subscriptionId >= 0) {
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        } else {
            SmsManager.getDefault()
        }

        val parts = sms.divideMessage(text)
        val sentIntents = ArrayList<PendingIntent>(parts.size)
        val deliveredIntents = ArrayList<PendingIntent>(parts.size)
        for (i in parts.indices) {
            sentIntents.add(resultPi(jobId, i, SmsResultReceiver.ACTION_SENT))
            deliveredIntents.add(resultPi(jobId, i, SmsResultReceiver.ACTION_DELIVERED))
        }
        sms.sendMultipartTextMessage(to, null, parts, sentIntents, deliveredIntents)
        writeToSentBox(to, text)
    }

    /**
     * When we're the default SMS app, mirror API-sent messages into the provider's Sent box so they
     * show up in the user's normal messaging app. No-op / harmless failure when not default.
     */
    private fun writeToSentBox(to: String, text: String) {
        if (context.packageName != Telephony.Sms.getDefaultSmsPackage(context)) return
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, to)
            put(Telephony.Sms.BODY, text)
            put(Telephony.Sms.DATE, now)
            // MIUI's Messages app shows DATE_SENT; without it the row appears dated 1970 ("eski").
            put(Telephony.Sms.DATE_SENT, now)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
            // Group under the recipient's conversation so it shows correctly in the messaging app.
            runCatching { Telephony.Threads.getOrCreateThreadId(context, to) }.getOrNull()
                ?.let { put(Telephony.Sms.THREAD_ID, it) }
        }
        try {
            context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
        } catch (e: Exception) {
            // best-effort provider mirror
        }
    }

    private fun resultPi(jobId: Long, part: Int, action: String): PendingIntent {
        // Explicit intent (targets the receiver by class). Our manifest receiver has no
        // <intent-filter>, so it only receives EXPLICIT broadcasts — an action-only intent
        // with setPackage() was never delivered, so the sent/delivered callback never fired
        // and jobs stayed "sending" while the server lease timed out to "failed".
        // The action still rides along so the receiver can tell sent from delivered.
        val intent = Intent(context, SmsResultReceiver::class.java).apply {
            this.action = action
            putExtra(SmsResultReceiver.EXTRA_JOB_ID, jobId)
            putExtra(SmsResultReceiver.EXTRA_PART, part)
        }
        val requestCode = (jobId.toInt() shl 8) or (part and 0xFF)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
