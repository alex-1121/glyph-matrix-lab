package com.sajenko.glyphtoys.toys

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelGridTest {
    @Test
    fun `set lit stores full brightness and unlit stores zero`() {
        val grid = PixelGrid()

        grid.set(1, 2)
        grid.set(3, 4, lit = false)

        assertEquals(PixelGrid.MAX_BRIGHTNESS, grid.brightness(1, 2))
        assertTrue(grid.get(1, 2))
        assertEquals(0, grid.brightness(3, 4))
        assertFalse(grid.get(3, 4))
    }

    @Test
    fun `partial brightness counts as lit and is clamped`() {
        val grid = PixelGrid()

        grid.setBrightness(0, 0, 40)
        grid.setBrightness(1, 0, 999)
        grid.setBrightness(2, 0, -3)

        assertTrue(grid.get(0, 0))
        assertEquals(40, grid.brightness(0, 0))
        assertEquals(PixelGrid.MAX_BRIGHTNESS, grid.brightness(1, 0))
        assertEquals(0, grid.brightness(2, 0))
    }

    @Test
    fun `out of bounds reads are off and writes are ignored`() {
        val grid = PixelGrid()

        grid.setBrightness(13, 0, 100)

        assertEquals(0, grid.brightness(13, 0))
        assertEquals(0, grid.brightness(-1, -1))
    }

    @Test
    fun `copy keeps brightness levels`() {
        val grid = PixelGrid()
        grid.setBrightness(5, 5, 77)

        val copy = grid.copy()
        grid.clear()

        assertEquals(77, copy.brightness(5, 5))
        assertEquals(0, grid.brightness(5, 5))
    }
}
