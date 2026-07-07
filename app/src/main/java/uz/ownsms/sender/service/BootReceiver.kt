package uz.ownsms.sender.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uz.ownsms.sender.ServiceLocator

/** Restarts the sender after reboot, but only if the user had it enabled and configured. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val s = ServiceLocator.settings
        if (s.enabled && s.isConfigured) {
            SenderService.start(context)
        }
    }
}
