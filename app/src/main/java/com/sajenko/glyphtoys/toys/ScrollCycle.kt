package com.sajenko.glyphtoys.toys

/**
 * Scroll offset state machine shared by [TextFrameScroller] implementations.
 *
 * [offset] is the content column shown in display column 0. It starts at `-displaySize`
 * (content fully off the right edge), grows by 1 per [advance], and wraps back to the
 * start once it reaches [contentWidth] (content fully off the left edge).
 */
class ScrollCycle(
    private val contentWidth: Int,
    private val displaySize: Int = PixelGrid().size,
) {
    var offset: Int = startOffset
        private set

    var hasFinishedCycle: Boolean = false
        private set

    private val startOffset: Int get() = -displaySize

    fun advance() {
        offset++
        if (offset >= contentWidth) {
            offset = startOffset
            hasFinishedCycle = true
        }
    }

    fun reset() {
        offset = startOffset
        hasFinishedCycle = false
    }
}
