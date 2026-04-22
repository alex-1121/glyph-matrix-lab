package com.sajenko.glyphtoys.models

import com.sajenko.glyphtoys.toys.PixelGrid

/**
 * Editor-only grid with a phone-shaped mask constraint.
 * The mask is not persisted or sent to the Glyph hardware.
 */
class MaskedPixelGrid private constructor(
    val size: Int = PixelGridSize,
    private val mask: BooleanArray,
    private val state: PixelGrid,
) {
    fun isEditable(x: Int, y: Int): Boolean {
        return x in 0 until size && y in 0 until size && mask[(y * size) + x]
    }

    fun isLit(x: Int, y: Int): Boolean {
        return isEditable(x, y) && state.get(x, y)
    }

    fun toggle(x: Int, y: Int) {
        if (isEditable(x, y)) {
            state.set(x, y, !state.get(x, y))
        }
    }

    fun set(x: Int, y: Int, lit: Boolean) {
        if (isEditable(x, y)) {
            state.set(x, y, lit)
        }
    }

    fun clear() {
        state.clear()
    }

    fun toPixelGrid(): PixelGrid {
        val output = PixelGrid(size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (isEditable(x, y) && state.get(x, y)) {
                    output.set(x, y)
                }
            }
        }
        return output
    }

    companion object {
        const val PixelGridSize = 13

        private val PhoneMaskRows = arrayOf(
            "xxxx.....xxxx",
            "xx.........xx",
            "x...........x",
            "x...........x",
            ".............",
            ".............",
            ".............",
            ".............",
            ".............",
            "x...........x",
            "x...........x",
            "xx.........xx",
            "xxxx.....xxxx",
        )

        fun isPhoneDisplayPixel(x: Int, y: Int): Boolean {
            if (x !in 0 until PixelGridSize || y !in 0 until PixelGridSize) {
                return false
            }
            return PhoneMaskRows[y][x] != 'x'
        }

        fun createWithPhoneMask(): MaskedPixelGrid {
            return MaskedPixelGrid(
                size = PixelGridSize,
                mask = buildPhoneMask(),
                state = PixelGrid(PixelGridSize),
            )
        }

        fun fromPixelGrid(grid: PixelGrid): MaskedPixelGrid {
            val maskedGrid = createWithPhoneMask()
            for (y in 0 until PixelGridSize) {
                for (x in 0 until PixelGridSize) {
                    if (grid.get(x, y)) {
                        maskedGrid.set(x, y, lit = true)
                    }
                }
            }
            return maskedGrid
        }

        private fun buildPhoneMask(): BooleanArray {
            val mask = BooleanArray(PixelGridSize * PixelGridSize)
            for (y in 0 until PixelGridSize) {
                for (x in 0 until PixelGridSize) {
                    mask[(y * PixelGridSize) + x] = isPhoneDisplayPixel(x, y)
                }
            }
            return mask
        }
    }
}
