package uz.ownsms.sender

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.ownsms.sender.data.remote.SimConfig
import uz.ownsms.sender.service.Overrides
import uz.ownsms.sender.service.effectiveSim
import uz.ownsms.sender.ui.parseRecipients

class BulkAndOverridesTest {

    // --- parseRecipients: blanks dropped, line numbers preserved for bad_row mapping ---

    @Test fun parse_drops_blanks_and_keeps_line_numbers() {
        val r = parseRecipients("901\n\n902\n  903  \n\n")
        assertEquals(listOf(1 to "901", 3 to "902", 4 to "903"), r.map { it.line to it.number })
    }

    @Test fun parse_handles_crlf_and_empty() {
        assertEquals(emptyList<Pair<Int, String>>(), parseRecipients("\n \n").map { it.line to it.number })
        assertEquals(listOf(1 to "901", 2 to "902"), parseRecipients("901\r\n902").map { it.line to it.number })
    }

    /** Duplicate numbers collapse to one job (first occurrence kept) so nobody gets two SMS. */
    @Test fun parse_dedupes_keeping_first_occurrence() {
        val r = parseRecipients("901\n902\n901\n902")
        assertEquals(listOf(1 to "901", 2 to "902"), r.map { it.line to it.number })
    }

    /** Server returns bad_rows[index] against the submitted array; index maps back to the typed line. */
    @Test fun bad_row_index_maps_to_source_line() {
        val parsed = parseRecipients("901\n\n902bad\n903")
        // recipients array = [901, 902bad, 903]; server rejects index 1 → line 3
        assertEquals(3, parsed[1].line)
        assertEquals("902bad", parsed[1].number)
    }

    // --- effectiveSim: blank override keeps server value; set override wins; pause collapses jitter ---

    private fun server() = SimConfig(
        subscription_id = 1, rate_per_min = 15, rate_per_hour = 200, rate_per_day = 500,
        jitter_min = 2, jitter_max = 5, work_hours_start = null, work_hours_end = null, daily_quota = null,
    )

    @Test fun empty_override_returns_server_unchanged() {
        val e = effectiveSim(server(), Overrides())
        assertEquals(15, e.rate_per_min)
        assertEquals(2, e.jitter_min)
        assertNull(e.daily_quota)
    }

    @Test fun set_override_wins() {
        val e = effectiveSim(server(), Overrides(ratePerMin = 5, dailyQuota = 100))
        assertEquals(5, e.rate_per_min)
        assertEquals(100, e.daily_quota)
        assertEquals(200, e.rate_per_hour) // untouched knob keeps server value
    }

    @Test fun pause_override_collapses_jitter_to_fixed() {
        val e = effectiveSim(server(), Overrides(pauseSeconds = 10))
        assertEquals(10, e.jitter_min)
        assertEquals(10, e.jitter_max)
    }
}
