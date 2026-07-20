package uz.ownsms.sender.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Required component for default-SMS-app eligibility (WAP_PUSH_DELIVER / MMS). This app is an SMS
 * gateway, not an MMS client — MMS are not stored or handled. ponytail: intentional no-op; add MMS
 * persistence only if the gateway ever needs to receive MMS.
 */
class MmsWapPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op: MMS is out of scope for the gateway.
    }
}
