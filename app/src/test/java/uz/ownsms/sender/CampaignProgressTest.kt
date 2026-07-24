package uz.ownsms.sender

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.ownsms.sender.data.remote.CampaignProgress
import uz.ownsms.sender.ui.screens.campaignProgressFraction

class CampaignProgressTest {

    @Test
    fun allSentFillsBar() {
        val p = CampaignProgress(sent = 100)
        assertEquals(1f, campaignProgressFraction(p, 100, terminal = false), 0.0001f)
    }

    @Test
    fun completedWithFailuresReachesFull() {
        val p = CampaignProgress(sent = 98, failed = 2)
        assertEquals(1f, campaignProgressFraction(p, 100, terminal = true), 0.0001f)
    }

    @Test
    fun halfwayNonTerminal() {
        val p = CampaignProgress(sent = 50)
        assertEquals(0.5f, campaignProgressFraction(p, 100, terminal = false), 0.0001f)
    }
}
