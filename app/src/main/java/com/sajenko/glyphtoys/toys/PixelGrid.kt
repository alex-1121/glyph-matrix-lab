package com.sajenko.glyphtoys.toys

/**
 * Pure domain model for 13x13 pixel frame data.
 * Each cell is either lit or off.
 */
class PixelGrid(val size: Int = 13) {
    private val pixels = BooleanArray(size * size)

    fun set(x: Int, y: Int, lit: Boolean = true) {
        if (x in 0 until size && y in 0 until size) {
            pixels[(y * size) + x] = lit
        }
    }

    fun get(x: Int, y: Int): Boolean {
        return if (x in 0 until size && y in 0 until size) {
            pixels[(y * size) + x]
        } else {
            false
        }
    }

    fun clear() {
        pixels.fill(false)
    }

    fun copy(): PixelGrid {
        val copy = PixelGrid(size)
        pixels.copyInto(copy.pixels)
        return copy
    }
}
