package uz.ownsms.sender

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG contrast guard for the theme-aware 'sent' status color (Color.kt / Theme.kt).
 * Pure JVM — no Android runtime — so it re-declares the color/surface RGB values locally
 * and a local WCAG 2.1 relative-luminance + contrast-ratio implementation.
 */
class StatusColorContrastTest {

    // 0xRRGGBB, mirroring the constants in ui/theme/Color.kt.
    private val statusSent = 0x2952E3 // light-surface indigo
    private val statusSentDark = 0x6E86FF // UltramarineLight — dark-surface variant
    private val lightSurface = 0xFFFFFF // colorScheme.surface (light) = Color.White
    private val darkSurface = 0x161A21 // InkSurface = colorScheme.surface (dark)

    private fun channelLuminance(c8: Int): Double {
        val cs = c8 / 255.0
        return if (cs <= 0.03928) cs / 12.92 else Math.pow((cs + 0.055) / 1.055, 2.4)
    }

    private fun relativeLuminance(rgb: Int): Double {
        val r = channelLuminance((rgb shr 16) and 0xFF)
        val g = channelLuminance((rgb shr 8) and 0xFF)
        val b = channelLuminance(rgb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrast(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }

    @Test
    fun darkSentPassesNormalAndLargeText() {
        // The chip label is 12sp normal text (needs >= 4.5:1); also covers the >= 3.0:1 large threshold.
        val ratio = contrast(statusSentDark, darkSurface)
        assertTrue("dark sent contrast $ratio < 4.5", ratio >= 4.5)
    }

    @Test
    fun lightSentStillPasses() {
        val ratio = contrast(statusSent, lightSurface)
        assertTrue("light sent contrast $ratio < 4.5", ratio >= 4.5)
    }

    @Test
    fun fixedSentWasFailingOnDark() {
        // Regression witness: the old theme-fixed indigo on the dark surface fails WCAG.
        assertTrue(contrast(statusSent, darkSurface) < 3.0)
    }
}
