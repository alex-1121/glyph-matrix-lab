package com.sajenko.glyphtoys.toys

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NTypeTextScrollerTest {
    @Test
    fun `gridFromViewPortData maps row-major SDK values onto the 4095 hardware scale`() {
        val data = IntArray(169)
        data[0] = 2047
        data[14] = 1152 // x=1, y=1, mid-level anti-aliased edge
        data[168] = 288

        val grid = NTypeTextScroller.gridFromViewPortData(data)

        assertEquals(127, grid.brightness(0, 0))
        assertEquals(72, grid.brightness(1, 1))
        assertEquals(18, grid.brightness(12, 12))
        assertFalse(grid.get(1, 0))
        assertEquals(3, (0 until 13).sumOf { y -> (0 until 13).count { x -> grid.get(x, y) } })
    }

    @Test
    fun `gridFromViewPortData keeps faint pixels lit`() {
        val grid = NTypeTextScroller.gridFromViewPortData(intArrayOf(0, 1))

        assertTrue(grid.get(1, 0))
        assertEquals(1, grid.brightness(1, 0))
        assertFalse(grid.get(0, 0))
    }
}
