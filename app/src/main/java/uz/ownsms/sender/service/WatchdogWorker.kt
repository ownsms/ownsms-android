package uz.ownsms.sender.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import uz.ownsms.sender.ServiceLocator
import java.util.concurrent.TimeUnit

/**
 * Periodic safety net: if sending is enabled but the OS killed the foreground service, restart it.
 * Note: on Android 12+ starting a foreground service from the background can be blocked
 * (ForegroundServiceStartNotAllowedException) — we best-effort and swallow that; the BootReceiver
 * and the app's own resume also restart the service.
 */
class WatchdogWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val s = ServiceLocator.settings
        if (s.enabled && s.isConfigured) {
            runCatching { SenderService.start(applicationContext) }
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "ownsms_watchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
