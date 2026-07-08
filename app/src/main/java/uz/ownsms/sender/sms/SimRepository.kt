package uz.ownsms.sender.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

data class SimInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val number: String,
)

class SimRepository(private val context: Context) {

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
        PackageManager.PERMISSION_GRANTED

    /**
     * Active SIMs. Empty if the permission is missing or there are none.
     * The phone number is often blank (carrier-dependent) — it's a label only; sending uses the
     * subscriptionId (SIM slot).
     */
    fun list(): List<SimInfo> {
        if (!hasPermission()) return emptyList()
        val sm = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        val subs = try {
            @Suppress("MissingPermission")
            sm.activeSubscriptionInfoList
        } catch (e: SecurityException) {
            null
        } ?: return emptyList()
        return subs.map { info ->
            // getNumber() is deprecated and blocked without READ_PHONE_NUMBERS on API 33+;
            // SubscriptionManager.getPhoneNumber() is the supported path there. Still blank if the
            // carrier didn't provision the number on the SIM.
            val number = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    @Suppress("MissingPermission")
                    sm.getPhoneNumber(info.subscriptionId)
                } else {
                    @Suppress("DEPRECATION", "MissingPermission")
                    info.number
                }
            } catch (e: Exception) {
                ""
            }
            SimInfo(
                subscriptionId = info.subscriptionId,
                slotIndex = info.simSlotIndex,
                displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                number = number ?: "",
            )
        }
    }
}
