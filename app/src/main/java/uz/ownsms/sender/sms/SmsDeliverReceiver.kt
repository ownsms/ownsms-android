package uz.ownsms.sender.sms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Required for the app to be a valid default SMS app. When we ARE the default app, incoming SMS are
 * delivered here (SMS_DELIVER) and nowhere else — so we must persist them to the Telephony provider,
 * otherwise the user loses their texts. Once written to the provider, any reader app (e.g. Google
 * Messages) still shows them, so the normal messaging experience keeps working. Being default is
 * what exempts the app from the OS "sending too many SMS" limit during bulk campaigns.
 */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // Multipart SMS arrive as several parts from the same sender — concatenate the body.
        val sender = messages[0].displayOriginatingAddress ?: messages[0].originatingAddress
        val body = messages.joinToString("") { it.displayMessageBody ?: it.messageBody ?: "" }
        val ts = messages[0].timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()

        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, sender)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, ts)
            put(Telephony.Sms.DATE_SENT, ts)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
        }
        try {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        } catch (e: Exception) {
            // Provider write can fail if we're not actually the default app — nothing else to do.
        }
        // As the default app we own the incoming notification, so the user still gets alerted.
        uz.ownsms.sender.service.Notifications.notifyIncoming(context, sender, body)
    }
}
