package uz.ownsms.sender.reliability

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

data class Check(val key: String, val label: String, val ok: Boolean)

/** Evaluates the on-device requirements that make the sender reliable. */
class ReliabilityChecker(private val context: Context) {

    fun checks(): List<Check> {
        val list = mutableListOf(
            Check("sms", "SMS yuborish ruxsati", granted(Manifest.permission.SEND_SMS)),
            Check("phone", "Telefon/SIM holati ruxsati", granted(Manifest.permission.READ_PHONE_STATE)),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Check("notif", "Bildirishnoma ruxsati", granted(Manifest.permission.POST_NOTIFICATIONS)))
        }
        list.add(Check("battery", "Batareya optimizatsiyasi o'chirilgan", batteryIgnored()))
        return list
    }

    /** All hard requirements satisfied. (OEM autostart can't be detected reliably, so it's a hint, not a gate.) */
    fun isReady(): Boolean = checks().all { it.ok }

    private fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun batteryIgnored(): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
