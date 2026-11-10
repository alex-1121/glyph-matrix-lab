package com.sajenko.glyphtoys.toys

import android.graphics.Typeface
import android.util.Log
import com.nothing.ketchum.GlyphMatrixNTypeUtils
import com.nothinglondon.text.Font
import com.nothinglondon.text.Text
import com.nothinglondon.text.TextDirection
import com.nothinglondon.text.TextWindow
import com.nothinglondon.text.ViewPort

/**
 * "Large" scrolling text drawn with Nothing's NType82 system font through the
 * undocumented text API in the unified Glyph SDK.
 *
 * Pull-based: each frame is rendered on demand at the current [ScrollCycle] offset, the
 * same way `GlyphMatrixNTypeUtils.animateText` renders each frame on its own thread. Font
 * size, letter spacing and the window setup copy the SDK's Phone (4a) Pro path
 * (`animateText` defaults `16f, 0f`, 13x13 `ViewPort`, `TextWindow` max value 2047).
 *
 * Use [createOrNull] so callers can fall back to another [TextFrameScroller].
 */
class NTypeTextScroller private constructor(
    private val text: Text,
    private val window: TextWindow,
) : TextFrameScroller {

    private val cycle = ScrollCycle(contentWidth = text.measuredWidth)

    override val hasFinishedCycle: Boolean get() = cycle.hasFinishedCycle

    override fun nextFrame(): PixelGrid {
        val frame = currentFrame()
        cycle.advance()
        return frame
    }

    override fun currentFrame(): PixelGrid {
        window.drawText(text.copy(position = cycle.offset))
        return gridFromViewPortData(window.getViewPortData())
    }

    override fun reset() = cycle.reset()

    companion object {
        private const val TAG = "NTypeTextScroller"
        private const val FONT_NAME = "NType82-Regular"
        private const val FONT_SIZE = 16f
        private const val LETTER_SPACING = 0f
        private const val MAX_PIXEL_VALUE = 2047

        /** Returns a scroller, or `null` if NType can't render [text] on this device. */
        fun createOrNull(text: String): TextFrameScroller? = try {
            if (text.isBlank() || !GlyphMatrixNTypeUtils.isNTypeSupport(text)) {
                Log.w(TAG, "NType does not support text; falling back")
                null
            } else {
                val size = PixelGrid().size
                val defaults = Font.getDefaultFont(size).options
                val font = Font(
                    FONT_NAME,
                    Typeface.create(FONT_NAME, Typeface.NORMAL),
                    FONT_SIZE,
                    defaults.copy(letterSpacing = LETTER_SPACING),
                )
                val window = TextWindow(size, size, MAX_PIXEL_VALUE, ViewPort(0, 0, size, size))
                NTypeTextScroller(Text(text, TextDirection.LEFT_TO_RIGHT, font, 0), window)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "NType scroller unavailable; falling back", e)
            null
        }

        /**
         * Hardware scale of `setMatrixFrame(int[])`: a white bitmap pixel at object
         * brightness 255 becomes 4095. NType text tops out at [MAX_PIXEL_VALUE] (2047),
         * i.e. about half brightness, and must keep that ratio to look the same.
         */
        private const val MATRIX_FULL_SCALE = 4095

        /**
         * Converts a row-major SDK frame (values 0..[MAX_PIXEL_VALUE], anti-aliased) into a
         * [PixelGrid] on the same absolute scale, so 2047 maps to about half of
         * [PixelGrid.MAX_BRIGHTNESS]. Any non-zero value stays lit.
         */
        fun gridFromViewPortData(data: IntArray, size: Int = PixelGrid().size): PixelGrid {
            val grid = PixelGrid(size)
            for (i in 0 until minOf(data.size, size * size)) {
                val v = data[i].coerceIn(0, MATRIX_FULL_SCALE)
                if (v > 0) {
                    val level = (v * PixelGrid.MAX_BRIGHTNESS + MATRIX_FULL_SCALE / 2) / MATRIX_FULL_SCALE
                    grid.setBrightness(i % size, i / size, level.coerceAtLeast(1))
                }
            }
            return grid
        }
    }
}
