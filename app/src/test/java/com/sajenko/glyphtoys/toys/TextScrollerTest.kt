package com.sajenko.glyphtoys.toys

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextScrollerTest {
    private val text = "HI"
    private val bitmap = GlyphFont.buildTextBitmap(text)
    private val width = bitmap[0].size
    private val rowOffset = (13 - GlyphFont.CHAR_HEIGHT) / 2

    @Test
    fun `first frame is blank`() {
        assertTrue(TextScroller(text).nextFrame().isBlank())
    }

    @Test
    fun `text enters from the right edge`() {
        val scroller = TextScroller(text)
        scroller.nextFrame()
        val second = scroller.nextFrame()

        for (row in 0 until GlyphFont.CHAR_HEIGHT) {
            assertEquals("row $row", bitmap[row][0], second.get(12, row + rowOffset))
        }
        for (col in 0 until 12) {
            for (row in 0 until 13) assertFalse("col $col row $row", second.get(col, row))
        }
    }

    @Test
    fun `one cycle is display width plus text width plus one blank frame`() {
        val scroller = TextScroller(text)
        var frames = 0
        var last = PixelGrid()
        while (!scroller.hasFinishedCycle) {
            last = scroller.nextFrame()
            frames++
            check(frames < 1000)
        }

        assertEquals(13 + width + 1, frames)
        assertTrue("last frame of the cycle is fully exited", last.isBlank())
    }

    @Test
    fun `reset clears finished flag and restarts blank`() {
        val scroller = TextScroller(text)
        repeat(13 + width + 1) { scroller.nextFrame() }
        assertTrue(scroller.hasFinishedCycle)

        scroller.reset()

        assertFalse(scroller.hasFinishedCycle)
        assertTrue(scroller.currentFrame().isBlank())
    }

    private fun PixelGrid.isBlank(): Boolean =
        (0 until size).all { x -> (0 until size).none { y -> get(x, y) } }
}
