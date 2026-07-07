package uz.ownsms.sender

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.ownsms.sender.data.remote.SimConfig
import uz.ownsms.sender.service.RateGate
import java.util.Calendar

class RateGateTest {

    private fun cfg(
        perMin: Int = 100,
        perHour: Int = 1000,
        perDay: Int = 10000,
        whStart: String? = null,
        whEnd: String? = null,
        dailyQuota: Int? = null,
    ) = SimConfig(
        subscription_id = 1,
        rate_per_min = perMin, rate_per_hour = perHour, rate_per_day = perDay,
        jitter_min = 1, jitter_max = 2,
        work_hours_start = whStart, work_hours_end = whEnd, daily_quota = dailyQuota,
    )

    @Test
    fun perMinuteLimitEnforced() {
        val g = RateGate()
        val now = 1_000_000_000_000L
        val c = cfg(perMin = 2)
        assertTrue(g.allowed(c, 1, now))
        g.record(1, now)
        assertTrue(g.allowed(c, 1, now))
        g.record(1, now)
        assertFalse(g.allowed(c, 1, now)) // 3rd within the minute is blocked
        assertTrue(g.allowed(c, 1, now + 61_000L)) // a minute later it's allowed again
    }

    @Test
    fun dailyQuotaEnforced() {
        val g = RateGate()
        val now = 1_000_000_000_000L
        val c = cfg(dailyQuota = 1)
        assertTrue(g.allowed(c, 1, now))
        g.record(1, now)
        assertFalse(g.allowed(c, 1, now + 120_000L)) // quota of 1 reached for the day
    }

    @Test
    fun workingHoursEnforced() {
        val g = RateGate()
        val night = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 0)
        }.timeInMillis
        val day = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }.timeInMillis
        val c = cfg(whStart = "08:00", whEnd = "20:00")
        assertFalse(g.allowed(c, 1, night)) // 03:00 is outside working hours
        assertTrue(g.allowed(c, 1, day)) // 12:00 is inside
    }
}
