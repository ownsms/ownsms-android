package uz.ownsms.sender.service

import uz.ownsms.sender.data.remote.SimConfig
import java.util.Calendar
import kotlin.random.Random

/**
 * Per-SIM pacing gate enforcing the synced config: working hours, per-minute/hour/day rate limits,
 * and daily quota. Send timestamps are kept in memory (reset on process restart — acceptable for the
 * MVP; a burst right after a restart is possible but bounded by the carrier itself).
 */
class RateGate {

    private val sends = HashMap<Int, ArrayDeque<Long>>()

    fun record(subId: Int, now: Long) {
        sends.getOrPut(subId) { ArrayDeque() }.addLast(now)
    }

    /** Whether a send from [subId] is allowed right now under [cfg]. */
    fun allowed(cfg: SimConfig, subId: Int, now: Long): Boolean {
        if (!withinWorkHours(cfg, now)) return false
        if (countSince(subId, now - 60_000L, now) >= cfg.rate_per_min) return false
        if (countSince(subId, now - 3_600_000L, now) >= cfg.rate_per_hour) return false
        val dayCount = countSince(subId, now - 86_400_000L, now)
        if (dayCount >= cfg.rate_per_day) return false
        if (cfg.daily_quota != null && dayCount >= cfg.daily_quota) return false
        return true
    }

    fun jitterDelayMs(cfg: SimConfig): Long {
        val min = (cfg.jitter_min.coerceAtLeast(0)) * 1000L
        val max = (cfg.jitter_max.coerceAtLeast(cfg.jitter_min)) * 1000L
        return if (max <= min) min else Random.nextLong(min, max + 1)
    }

    private fun countSince(subId: Int, since: Long, now: Long): Int {
        val dq = sends[subId] ?: return 0
        while (dq.isNotEmpty() && dq.first() < now - 86_400_000L) dq.removeFirst() // prune > 1 day
        return dq.count { it >= since }
    }

    private fun withinWorkHours(cfg: SimConfig, now: Long): Boolean {
        val start = cfg.work_hours_start ?: return true
        val end = cfg.work_hours_end ?: return true
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val s = toMinutes(start)
        val e = toMinutes(end)
        return if (s <= e) minutes in s..e else minutes >= s || minutes <= e
    }

    private fun toMinutes(t: String): Int {
        val parts = t.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }
}
