package com.sajenko.glyphtoys.serialization

import com.sajenko.glyphtoys.toys.PixelGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphImageSerializerTest {
    @Test
    fun `pixel grid round trips through binary string`() {
        val grid = PixelGrid().apply {
            set(0, 0)
            set(6, 6)
            set(12, 12)
        }

        val binary = GlyphImageSerializer.pixelGridToBinary(grid)
        val parsed = GlyphImageSerializer.binaryToPixelGrid(binary)!!

        assertEquals(GlyphImageSerializer.BinaryLength, binary.length)
        assertTrue(parsed.get(0, 0))
        assertTrue(parsed.get(6, 6))
        assertTrue(parsed.get(12, 12))
        assertFalse(parsed.get(1, 0))
    }

    @Test
    fun `invalid binary strings are rejected`() {
        assertNull(GlyphImageSerializer.binaryToPixelGrid("101"))
        assertNull(GlyphImageSerializer.binaryToPixelGrid("x".repeat(GlyphImageSerializer.BinaryLength)))
    }
}
