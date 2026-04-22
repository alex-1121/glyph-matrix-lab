package com.sajenko.glyphtoys.toys

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameBuildersTest {
    @Test
    fun `buildCallGrid frame 0 lights handset pixels only`() {
        val grid = FrameBuilders.buildCallGrid(frameIndex = 0)

        assertTrue("Handset top should be lit", grid.get(3, 2))
        assertTrue("Handset body should be lit", grid.get(2, 2))
        assertTrue("Handset bottom should be lit", grid.get(6, 10))
        assertFalse("Inner wave should be off in frame 0", grid.get(7, 2))
        assertFalse("Outer wave should be off in frame 0", grid.get(9, 2))
        assertFalse("Corner (12, 12) should be off", grid.get(12, 12))
    }

    @Test
    fun `buildCallGrid frame 1 adds inner wave`() {
        val grid = FrameBuilders.buildCallGrid(frameIndex = 1)

        assertTrue("Inner wave should be lit", grid.get(7, 2))
        assertTrue("Inner wave edge should be lit", grid.get(8, 4))
        assertTrue("Handset should remain lit", grid.get(2, 2))
        assertFalse("Outer wave should still be off", grid.get(9, 2))
        assertFalse("Outer wave edge should still be off", grid.get(9, 4))
    }

    @Test
    fun `buildCallGrid frame 2 shows expanded outer wave`() {
        val grid = FrameBuilders.buildCallGrid(frameIndex = 2)

        assertTrue("Outer wave should be lit", grid.get(8, 1))
        assertTrue("Outer wave edge should be lit", grid.get(9, 4))
        assertTrue("Outer wave top should be lit", grid.get(6, 0))
        assertTrue("Handset should remain lit", grid.get(6, 10))
        assertFalse("Far corner should remain off", grid.get(12, 12))
    }

    @Test
    fun `buildClockGrid preserves known glyph anchors for 12-34`() {
        val grid = FrameBuilders.buildClockGrid(hour = 12, minute = 34)

        assertTrue("Hour 1 should light (4, 1)", grid.get(4, 1))
        assertTrue("Hour 2 should light (7, 1)", grid.get(7, 1))
        assertTrue("Hour 2 should light (10, 2)", grid.get(10, 2))
        assertTrue("Minute 3 should light (2, 7)", grid.get(2, 7))
        assertTrue("Minute 3 should light (5, 10)", grid.get(5, 10))
        assertTrue("Minute 4 should light (7, 7)", grid.get(7, 7))
        assertTrue("Minute 4 should light (8, 9)", grid.get(8, 9))
        assertTrue("Minute 4 should light (10, 11)", grid.get(10, 11))

        assertFalse("Gap between hour digits should stay dark", grid.get(6, 2))
        assertFalse("Gap inside hour 2 should stay dark", grid.get(7, 2))
        assertFalse("Clock background should stay dark", grid.get(0, 6))
        assertFalse("Unused bottom-right corner should stay dark", grid.get(12, 12))
    }

    @Test
    fun `buildEqualizerGrid lights all 13 columns based on heights`() {
        val heights = intArrayOf(1, 3, 5, 7, 9, 11, 13, 11, 9, 7, 5, 3, 1)
        val grid = FrameBuilders.buildEqualizerGrid(heights)

        assertTrue(grid.get(0, 12))
        assertFalse(grid.get(0, 11))

        for (y in 0 until 13) {
            assertTrue(grid.get(6, y))
        }

        assertTrue(grid.get(12, 12))
        assertFalse(grid.get(12, 11))
    }

    @Test
    fun `buildEqualizerGrid respects height parameter`() {
        val heights = intArrayOf(3, 0, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val grid = FrameBuilders.buildEqualizerGrid(heights)

        assertTrue(grid.get(0, 10))
        assertTrue(grid.get(0, 11))
        assertTrue(grid.get(0, 12))
        assertFalse(grid.get(0, 9))

        for (y in 0 until 13) {
            assertFalse(grid.get(1, y))
        }

        for (y in 0 until 13) {
            assertTrue(grid.get(2, y))
        }
    }
}
