package uz.ownsms.sender.sms

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony

/**
 * Helpers for becoming the default SMS app — which exempts the app from the OS "sending too many
 * SMS" rate-limit dialog, the reliable way to send bulk campaigns without interruption.
 */
object DefaultSmsApp {

    fun isDefault(context: Context): Boolean = context.packageName == Telephony.Sms.getDefaultSmsPackage(context)

    /**
     * Intent that asks the user to make this app the default SMS app. On Android 10+ this is the
     * RoleManager ROLE_SMS request; below 10 it's the legacy change-default dialog. Returns null if
     * already default or the role is unavailable.
     */
    fun requestIntent(context: Context): Intent? {
        if (isDefault(context)) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java) ?: return null
            if (rm.isRoleAvailable(RoleManager.ROLE_SMS) && !rm.isRoleHeld(RoleManager.ROLE_SMS)) {
                rm.createRequestRoleIntent(RoleManager.ROLE_SMS)
            } else {
                null
            }
        } else {
            @Suppress("DEPRECATION")
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
        }
    }
}
