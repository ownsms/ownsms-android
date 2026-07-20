package uz.ownsms.sender.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Required component for default-SMS-app eligibility (RESPOND_VIA_MESSAGE — "reply with message"
 * from the call screen). The gateway has no quick-reply UX. ponytail: intentional no-op stub;
 * satisfies the role requirement so ROLE_SMS can be granted.
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
