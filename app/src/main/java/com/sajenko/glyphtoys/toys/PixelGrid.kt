package com.sajenko.glyphtoys.toys

/**
 * Pure domain model for 13x13 pixel frame data.
 *
 * Each cell stores a brightness from 0 (off) to [MAX_BRIGHTNESS] (fully on). The on/off API
 * ([set] with `lit`, [get]) covers most frames; [setBrightness]/[brightness] carry partial
 * levels, e.g. anti-aliased edges from NType text.
 */
class PixelGrid(val size: Int = 13) {
    private val pixels = IntArray(size * size)

    fun set(x: Int, y: Int, lit: Boolean = true) {
        setBrightness(x, y, if (lit) MAX_BRIGHTNESS else 0)
    }

    /** `true` if the pixel is on at any brightness. */
    fun get(x: Int, y: Int): Boolean = brightness(x, y) > 0

    fun setBrightness(x: Int, y: Int, value: Int) {
        if (x in 0 until size && y in 0 until size) {
            pixels[(y * size) + x] = value.coerceIn(0, MAX_BRIGHTNESS)
        }
    }

    fun brightness(x: Int, y: Int): Int {
        return if (x in 0 until size && y in 0 until size) {
            pixels[(y * size) + x]
        } else {
            0
        }
    }

    fun clear() {
        pixels.fill(0)
    }

    fun copy(): PixelGrid {
        val copy = PixelGrid(size)
        pixels.copyInto(copy.pixels)
        return copy
    }

    companion object {
        const val MAX_BRIGHTNESS = 255
    }
}
