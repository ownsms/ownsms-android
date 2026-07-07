package uz.ownsms.sender.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Best-effort deep-links to OEM "autostart" / "protected apps" screens. These cannot be enabled
 * programmatically — only the user can — so we just take them to the right place.
 */
object OemAutostart {

    private val INTENTS = listOf(
        // Xiaomi / MIUI
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        // Huawei
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        // Oppo / ColorOS
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        // Vivo / iQOO
        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
        ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
        // Samsung device care
        ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
    )

    /** Manufacturers known to aggressively kill background apps. */
    fun isLikelyAggressive(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        return listOf(
            "xiaomi", "redmi", "poco", "huawei", "honor", "oppo", "realme",
            "vivo", "iqoo", "oneplus", "samsung", "letv", "asus",
        ).any { m.contains(it) }
    }

    /** Opens the OEM autostart screen if known, otherwise the app details settings. */
    fun open(context: Context): Boolean {
        for (cn in INTENTS) {
            val intent = Intent().setComponent(cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                return runCatching { context.startActivity(intent) }.isSuccess
            }
        }
        return runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.isSuccess
    }
}
