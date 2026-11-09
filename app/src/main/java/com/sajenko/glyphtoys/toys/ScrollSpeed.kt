package com.sajenko.glyphtoys.toys

/**
 * Scrolling text speed in pixels per second. Scrollers move 1 px per tick, so the tick
 * delay is `1000 / pxPerSec` ms. For reference, the SDK's own NType animation on the
 * Phone (4a) Pro runs at 24 px/s.
 */
object ScrollSpeed {
    const val MIN_PX_PER_SEC = 8
    const val MAX_PX_PER_SEC = 40
    const val DEFAULT_PX_PER_SEC = 20

    fun clamp(pxPerSec: Int): Int = pxPerSec.coerceIn(MIN_PX_PER_SEC, MAX_PX_PER_SEC)

    fun tickDelayMs(pxPerSec: Int): Long = 1000L / clamp(pxPerSec)
}
