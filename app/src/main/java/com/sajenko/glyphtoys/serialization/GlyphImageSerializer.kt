package com.sajenko.glyphtoys.serialization

import com.sajenko.glyphtoys.toys.PixelGrid

object GlyphImageSerializer {
    const val Size = 13
    const val BinaryLength = Size * Size

    fun pixelGridToBinary(grid: PixelGrid): String {
        val builder = StringBuilder(BinaryLength)
        for (y in 0 until Size) {
            for (x in 0 until Size) {
                builder.append(if (grid.get(x, y)) '1' else '0')
            }
        }
        return builder.toString()
    }

    fun binaryToPixelGrid(binary: String): PixelGrid? {
        if (binary.length != BinaryLength || binary.any { it != '0' && it != '1' }) {
            return null
        }

        val grid = PixelGrid(Size)
        binary.forEachIndexed { index, value ->
            if (value == '1') {
                grid.set(index % Size, index / Size)
            }
        }
        return grid
    }
}
