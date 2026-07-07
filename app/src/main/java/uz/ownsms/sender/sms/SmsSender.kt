package uz.ownsms.sender.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
