package com.sajenko.glyphtoys.toys

/**
 * Manages scrolling text animation state for the Glyph Matrix.
 *
 * The text is rendered into a wide bitmap (using [GlyphFont]) that can be
 * wider than 13 columns. Each call to [nextFrame] advances the scroll offset
 * by one pixel column and returns a 13x13 [PixelGrid] window into that bitmap.
 *
 * When the end of the text is reached, the scroller wraps back to the start
 * so the text loops continuously.
 *
 * The text is vertically centred on the 13-row display.
 */
class TextScroller(text: String) {

    private val bitmap: Array<BooleanArray> = GlyphFont.buildTextBitmap(text)
    private val bitmapWidth: Int = if (bitmap.isNotEmpty()) bitmap[0].size else 0

    // We scroll the text in from the right edge: start offset at -13 so the display is
    // initially blank and the text scrolls into view.
    private var offset: Int = -(DISPLAY_SIZE)

    // Vertical offset: centre the 5-row font in the 13-row display
    // Top padding = (13 - 5) / 2 = 4
    private val rowOffset: Int = (DISPLAY_SIZE - GlyphFont.CHAR_HEIGHT) / 2

    /**
     * Advance scroll by one pixel and return the current 13x13 window.
     */
    fun nextFrame(): PixelGrid {
        val frame = currentFrame()
        advance()
        return frame
    }

    /**
     * Returns the current 13x13 frame without advancing.
     */
    fun currentFrame(): PixelGrid {
        val grid = PixelGrid()
        for (row in 0 until GlyphFont.CHAR_HEIGHT) {
            val displayRow = row + rowOffset
            for (displayCol in 0 until DISPLAY_SIZE) {
                val bitmapCol = displayCol + offset
                if (bitmapCol >= 0 && bitmapCol < bitmapWidth) {
                    if (bitmap[row][bitmapCol]) {
                        grid.set(displayCol, displayRow)
                    }
                }
            }
        }
        return grid
    }

    var hasFinishedCycle: Boolean = false
        private set

    /**
     * Advance the scroll offset by one pixel column. Wraps after the text has
     * fully scrolled off the left edge (offset == bitmapWidth).
     */
    private fun advance() {
        offset++
        if (offset >= bitmapWidth) {
            // Restart: scroll in from the right again after a short blank pause
            offset = -(DISPLAY_SIZE)
            hasFinishedCycle = true
        }
    }

    /**
     * Reset scroll position to the beginning.
     */
    fun reset() {
        offset = -(DISPLAY_SIZE)
        hasFinishedCycle = false
    }

    companion object {
        const val DISPLAY_SIZE = 13
    }
}
