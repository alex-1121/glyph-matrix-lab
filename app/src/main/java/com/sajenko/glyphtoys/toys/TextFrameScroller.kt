package com.sajenko.glyphtoys.toys

/**
 * A horizontally scrolling text source that is pulled one 13x13 frame at a time.
 *
 * Contract:
 * - Starts with the text fully off the right edge (the first frame is blank).
 * - [nextFrame] returns the current frame, then advances the scroll by 1 px.
 * - After the text has fully left the left edge, the scroller wraps back to the start
 *   and sets [hasFinishedCycle] to `true`.
 * - [reset] returns to the start and clears [hasFinishedCycle].
 *
 * The caller decides the tick rate (see [ScrollSpeed.tickDelayMs]).
 */
interface TextFrameScroller {
    fun nextFrame(): PixelGrid
    fun currentFrame(): PixelGrid
    val hasFinishedCycle: Boolean
    fun reset()
}
