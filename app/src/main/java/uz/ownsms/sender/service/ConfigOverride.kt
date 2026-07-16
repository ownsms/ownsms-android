package uz.ownsms.sender.service

import uz.ownsms.sender.data.remote.DeviceConfig
import uz.ownsms.sender.data.remote.SimConfig

/** Local send-rate overrides. A null field keeps the server's value for that knob. */
data class Overrides(
    val pauseSeconds: Int? = null,
    val ratePerMin: Int? = null,
    val ratePerHour: Int? = null,
    val ratePerDay: Int? = null,
    val dailyQuota: Int? = null,
) {
    val isEmpty: Boolean get() = this == Overrides()
}

/**
 * Overlay [o] on the server's per-SIM config. An overridden pause collapses the jitter range to a
 * fixed value (min == max), so [RateGate.jitterDelayMs] returns exactly that pause.
 */
fun effectiveSim(server: SimConfig, o: Overrides): SimConfig = server.copy(
    rate_per_min = o.ratePerMin ?: server.rate_per_min,
    rate_per_hour = o.ratePerHour ?: server.rate_per_hour,
    rate_per_day = o.ratePerDay ?: server.rate_per_day,
    daily_quota = o.dailyQuota ?: server.daily_quota,
    jitter_min = o.pauseSeconds ?: server.jitter_min,
    jitter_max = o.pauseSeconds ?: server.jitter_max,
)

fun DeviceConfig.withOverrides(o: Overrides): DeviceConfig = if (o.isEmpty) this else copy(sims = sims.map { effectiveSim(it, o) })
