package uz.ownsms.sender.service

import kotlin.random.Random

/**
 * Conservative inter-message pacing to avoid carrier anti-spam blocking. Fixed defaults for the
 * MVP (not user-configurable yet).
 */
object Pacing {
    private const val MIN_MS = 2_000L
    private const val MAX_MS = 5_000L
    fun nextDelayMs(): Long = Random.nextLong(MIN_MS, MAX_MS + 1)
}
