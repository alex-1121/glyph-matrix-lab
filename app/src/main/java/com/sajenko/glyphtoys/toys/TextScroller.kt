package com.sajenko.glyphtoys.toys

/**
 * "Compact" scrolling text: the 5-row [GlyphFont], on/off pixels only.
 *
 * The text is rendered into a wide bitmap that can be wider than 13 columns.
 * Each call to [nextFrame] returns the current 13x13 window into that bitmap and
 * advances the shared [ScrollCycle] by one pixel column: the text enters from the
 * right edge, leaves on the left, a blank frame is shown, then it wraps.
 *
 * The text is vertically centred on the 13-row display.
 */
class TextScroller(text: String) : TextFrameScroller {

    private val bitmap: Array<BooleanArray> = GlyphFont.buildTextBitmap(text)
    private val bitmapWidth: Int = if (bitmap.isNotEmpty()) bitmap[0].size else 0
    private val cycle = ScrollCycle(contentWidth = bitmapWidth, displaySize = DISPLAY_SIZE)

    // Vertical offset: centre the 5-row font in the 13-row display
    // Top padding = (13 - 5) / 2 = 4
    private val rowOffset: Int = (DISPLAY_SIZE - GlyphFont.CHAR_HEIGHT) / 2

    override val hasFinishedCycle: Boolean
        get() = cycle.hasFinishedCycle

    override fun nextFrame(): PixelGrid {
        val frame = currentFrame()
        cycle.advance()
        return frame
    }

    override fun currentFrame(): PixelGrid {
        val grid = PixelGrid()
        val offset = cycle.offset
        for (row in 0 until GlyphFont.CHAR_HEIGHT) {
            val displayRow = row + rowOffset
            for (displayCol in 0 until DISPLAY_SIZE) {
                val bitmapCol = displayCol + offset
                if (bitmapCol in 0 until bitmapWidth && bitmap[row][bitmapCol]) {
                    grid.set(displayCol, displayRow)
                }
            }
        }
        return grid
    }

    override fun reset() = cycle.reset()

    companion object {
        const val DISPLAY_SIZE = 13
    }
}
