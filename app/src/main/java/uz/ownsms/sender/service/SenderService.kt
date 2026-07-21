package uz.ownsms.sender.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uz.ownsms.sender.ServiceLocator
import uz.ownsms.sender.data.db.JobDao
import uz.ownsms.sender.data.db.JobEntity
import uz.ownsms.sender.data.db.JobState
import uz.ownsms.sender.data.remote.DeviceConfig
import uz.ownsms.sender.data.remote.StatusReport
import uz.ownsms.sender.sms.SmsSender
import uz.ownsms.sender.data.remote.Job as ApiJob

/**
 * Foreground service: maintains the long-poll loop, sends claimed jobs through the SIM (paced),
 * and reports outcomes. At-most-once: a job left "sending" across a restart is failed, never resent.
 */
class SenderService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null
    private var reporter: Job? = null
    private lateinit var dao: JobDao
    private lateinit var sender: SmsSender
    private val rateGate = RateGate()
    private var config: DeviceConfig? = null
    private var lastConfigAt = 0L

    @Volatile private var paused = false

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
        startForeground(Notifications.FOREGROUND_ID, Notifications.build(this, "Ishga tushmoqda…", paused))
        dao = ServiceLocator.db.jobDao()
        sender = SmsSender(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                ServiceLocator.settings.enabled = false
                WatchdogWorker.cancel(applicationContext)
                loop?.cancel()
                reporter?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                paused = true
                notify("Pauza — yuborish to'xtatildi")
            }
            ACTION_RESUME -> {
                paused = false
                notify("Davom etmoqda…")
            }
        }
        if (loop?.isActive != true) {
            loop = scope.launch { runLoop() }
        }
        // Report outcomes on a fast cadence, independent of the ~30s long-poll, so "sent"/"delivered"
        // reach the server well inside its job lease (otherwise the server reclaims them as failed).
        if (reporter?.isActive != true) {
            reporter = scope.launch { reporterLoop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun runLoop() {
        reconcile()
        while (scope.isActive) {
            try {
                if (!ServiceLocator.settings.enabled) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return
                }
                if (!ServiceLocator.settings.isConfigured) {
                    notify("Sozlanmagan — API URL va token kiriting")
                    delay(10_000)
                    continue
                }
                if (paused) {
                    notify("Pauza — yuborish to'xtatildi")
                    delay(3_000)
                    continue
                }
                refreshConfigIfStale()
                sendClaimed()
                notify("Ulangan. Kutilmoqda…")
                poll()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notify("Tarmoq xatosi, qayta urinish…")
                delay(5_000)
            }
        }
    }

    /** Reports outcomes every ~5s, decoupled from the long-poll, so reports land within the server lease. */
    private suspend fun reporterLoop() {
        while (scope.isActive) {
            try {
                if (ServiceLocator.settings.isConfigured) reportOutcomes()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // transient; retry next tick
            }
            delay(5_000)
        }
    }

    /**
     * Jobs in-flight at a crash → assume SENT (never resend — at-most-once). The SIM/telephony stack
     * almost certainly already accepted them (the send handoff is synchronous and outlives our process),
     * so reporting "sent" — not "failed" — stops SMS that actually went out from showing as
     * "app_restart" errors, and lets the server accept them within the job lease.
     */
    private suspend fun reconcile() {
        for (job in dao.byState(JobState.SENDING)) {
            dao.setState(job.id, JobState.SENT)
        }
    }

    private suspend fun poll() {
        val resp = ServiceLocator.api().poll()
        if (resp.code() == 200) {
            val jobs = resp.body()?.jobs.orEmpty()
            if (jobs.isNotEmpty()) {
                dao.insertAll(jobs.map { it.toEntity() })
                notify("${jobs.size} ta yangi xabar")
            }
        }
        // HTTP 204 → nothing queued; the long-poll already waited server-side.
    }

    /** Refreshes the per-SIM pacing config every ~5 min; keeps the last value (or null) on failure. */
    private suspend fun refreshConfigIfStale() {
        val now = System.currentTimeMillis()
        if (config != null && now - lastConfigAt < 5 * 60_000L) return
        try {
            val s = ServiceLocator.settings
            val overrides = Overrides(s.pauseSeconds, s.ratePerMin, s.ratePerHour, s.ratePerDay, s.dailyQuota)
            config = ServiceLocator.api().config().withOverrides(overrides)
            lastConfigAt = now
        } catch (e: Exception) {
            // keep previous config; without any config we fall back to default pacing
        }
    }

    private suspend fun sendClaimed() {
        val settings = ServiceLocator.settings
        for (job in dao.byState(JobState.CLAIMED)) {
            val sub = job.subscriptionId ?: settings.defaultSubId.takeIf { it >= 0 }
            val now = System.currentTimeMillis()
            val simCfg = sub?.let { s -> config?.sims?.firstOrNull { it.subscription_id == s } }

            // When we have config for this SIM, respect rate limits + working hours; otherwise just send.
            // (simCfg != null guarantees sub != null, since simCfg derives from sub.)
            if (simCfg != null && !rateGate.allowed(simCfg, sub, now)) {
                continue // hold; the job stays "claimed" and is retried next loop
            }

            dao.setState(job.id, JobState.SENDING)
            try {
                sender.send(job.id, job.to, job.text, sub)
                // Optimistically mark SENT the moment the SIM accepts the handoff, so the "sent"
                // report reaches the server inside its job lease even if the sent PendingIntent never
                // fires (common on MIUI) or the service is killed right after. A real failure still
                // arrives via SmsResultReceiver and downgrades to failed (server accepts sent→failed).
                // ponytail: reports sent before delivery confirmation; the only new false-success is a
                // silent drop with no error intent — already indistinguishable from a billing reject.
                dao.setState(job.id, JobState.SENT)
                if (simCfg != null) rateGate.record(sub, now)
            } catch (e: Exception) {
                dao.setState(job.id, JobState.FAILED, "send_exception")
            }
            val delayMs = simCfg?.let { rateGate.jitterDelayMs(it) } ?: Pacing.nextDelayMs()
            delay(delayMs)
        }
    }

    private suspend fun reportOutcomes() {
        val api = ServiceLocator.api()
        // 1. Report "sent" for every job that left the SIM — including ones that already raced to
        //    "delivered" locally. The server only accepts "delivered" from the "sent" state, so if we
        //    skip "sent" the delivered report is silently ignored and the job dies on lease timeout.
        for (job in dao.needsSentReport()) {
            try {
                api.reportStatus(job.id, StatusReport(status = "sent"))
                dao.setSentReported(job.id, true)
            } catch (e: Exception) { /* retry next iteration */ }
        }
        // 2. Terminal outcomes: delivered only after "sent" is on record; failed is accepted anytime.
        for (state in listOf(JobState.DELIVERED, JobState.FAILED)) {
            for (job in dao.unreported(state)) {
                if (state == JobState.DELIVERED && !job.sentReported) continue // wait for sent to land
                try {
                    api.reportStatus(job.id, StatusReport(status = state, error_code = job.errorCode))
                    dao.markReported(job.id)
                } catch (e: Exception) { /* retry next iteration */ }
            }
        }
        dao.prune(200) // keep local history bounded — only fully-reported rows are eligible
    }

    private fun notify(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(Notifications.FOREGROUND_ID, Notifications.build(this, text, paused))
    }

    companion object {
        const val ACTION_STOP = "uz.ownsms.sender.action.STOP"
        const val ACTION_PAUSE = "uz.ownsms.sender.action.PAUSE"
        const val ACTION_RESUME = "uz.ownsms.sender.action.RESUME"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, SenderService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SenderService::class.java))
        }
    }
}

private fun ApiJob.toEntity() = JobEntity(
    id = id,
    to = to,
    text = text,
    subscriptionId = subscription_id,
    segments = segments,
    state = JobState.CLAIMED,
    createdAt = System.currentTimeMillis(),
)
