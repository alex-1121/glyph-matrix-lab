package com.sajenko.glyphtoys.models

import com.sajenko.glyphtoys.toys.PixelGrid
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskedPixelGridTest {
    @Test
    fun `disabled mask cells cannot be toggled`() {
        val grid = MaskedPixelGrid.createWithPhoneMask()

        grid.toggle(0, 0)
        grid.toggle(6, 6)

        assertFalse(grid.isEditable(0, 0))
        assertFalse(grid.isLit(0, 0))
        assertTrue(grid.isEditable(6, 6))
        assertTrue(grid.isLit(6, 6))
    }

    @Test
    fun `fromPixelGrid keeps only editable lit pixels`() {
        val source = PixelGrid().apply {
            set(0, 0)
            set(6, 6)
        }

        val masked = MaskedPixelGrid.fromPixelGrid(source)
        val output = masked.toPixelGrid()

        assertFalse(output.get(0, 0))
        assertTrue(output.get(6, 6))
    }

    @Test
    fun `phone display mask exposes rounded matrix shape`() {
        assertFalse(MaskedPixelGrid.isPhoneDisplayPixel(0, 0))
        assertFalse(MaskedPixelGrid.isPhoneDisplayPixel(12, 12))
        assertTrue(MaskedPixelGrid.isPhoneDisplayPixel(4, 0))
        assertTrue(MaskedPixelGrid.isPhoneDisplayPixel(6, 6))
        assertFalse(MaskedPixelGrid.isPhoneDisplayPixel(-1, 0))
        assertFalse(MaskedPixelGrid.isPhoneDisplayPixel(13, 0))
    }
}
