package com.sajenko.glyphtoys.toys

/**
 * Pixel font for scrolling text on the 13x13 Glyph Matrix.
 *
 * Each character is represented as a list of row-strings using '#' for a lit pixel.
 * Characters are 5 rows tall. Width varies per character.
 * A 1-column gap is automatically inserted between characters.
 */
object GlyphFont {

    const val CHAR_HEIGHT = 5
    private const val CHAR_SPACING = 1 // blank columns between chars

    /**
     * Returns a 2D boolean array [row][col] where row ∈ [0, CHAR_HEIGHT) and col ∈ [0, totalWidth).
     * This is what TextScroller uses to produce 13x13 PixelGrid windows.
     */
    fun buildTextBitmap(text: String): Array<BooleanArray> {
        val upper = text.uppercase()
        if (upper.isEmpty()) {
            // Return blank strip 13 wide (one full display width of blank)
            return Array(CHAR_HEIGHT) { BooleanArray(13) }
        }

        val charBitmaps = upper.map { fontFor(it) }
        val totalWidth = charBitmaps.sumOf { it[0].length } + (charBitmaps.size - 1) * CHAR_SPACING

        val bitmap = Array(CHAR_HEIGHT) { BooleanArray(totalWidth) }

        var xCursor = 0
        for ((charIndex, rows) in charBitmaps.withIndex()) {
            val charWidth = rows[0].length
            for (row in rows.indices) {
                val pattern = rows[row]
                for (col in pattern.indices) {
                    bitmap[row][xCursor + col] = (pattern[col] == '#')
                }
            }
            xCursor += charWidth
            if (charIndex < charBitmaps.size - 1) {
                xCursor += CHAR_SPACING
            }
        }
        return bitmap
    }

    private fun fontFor(char: Char): Array<String> {
        return FONT[char] ?: FONT[' ']!!
    }

    // ---------------------------------------------------------------------------
    // Font definitions — 5 rows tall, variable width, '#' = lit pixel
    // ---------------------------------------------------------------------------
    private val FONT: Map<Char, Array<String>> = mapOf(
        ' ' to arrayOf(
            "  ",
            "  ",
            "  ",
            "  ",
            "  ",
        ),
        'A' to arrayOf(
            ".#.",
            "#.#",
            "###",
            "#.#",
            "#.#",
        ),
        'B' to arrayOf(
            "##.",
            "#.#",
            "##.",
            "#.#",
            "##.",
        ),
        'C' to arrayOf(
            ".##",
            "#..",
            "#..",
            "#..",
            ".##",
        ),
        'D' to arrayOf(
            "##.",
            "#.#",
            "#.#",
            "#.#",
            "##.",
        ),
        'E' to arrayOf(
            "###",
            "#..",
            "##.",
            "#..",
            "###",
        ),
        'F' to arrayOf(
            "###",
            "#..",
            "##.",
            "#..",
            "#..",
        ),
        'G' to arrayOf(
            ".##",
            "#..",
            "#.#",
            "#.#",
            ".##",
        ),
        'H' to arrayOf(
            "#.#",
            "#.#",
            "###",
            "#.#",
            "#.#",
        ),
        'I' to arrayOf(
            "#",
            "#",
            "#",
            "#",
            "#",
        ),
        'J' to arrayOf(
            "..#",
            "..#",
            "..#",
            "#.#",
            ".#.",
        ),
        'K' to arrayOf(
            "#.#",
            "#.#",
            "##.",
            "#.#",
            "#.#",
        ),
        'L' to arrayOf(
            "#..",
            "#..",
            "#..",
            "#..",
            "###",
        ),
        'M' to arrayOf(
            "#...#",
            "##.##",
            "#.#.#",
            "#...#",
            "#...#",
        ),
        'N' to arrayOf(
            "#..#",
            "##.#",
            "#.##",
            "#..#",
            "#..#",
        ),
        'O' to arrayOf(
            ".#.",
            "#.#",
            "#.#",
            "#.#",
            ".#.",
        ),
        'P' to arrayOf(
            "##.",
            "#.#",
            "##.",
            "#..",
            "#..",
        ),
        'Q' to arrayOf(
            ".#.",
            "#.#",
            "#.#",
            "#.#",
            ".##",
        ),
        'R' to arrayOf(
            "##.",
            "#.#",
            "##.",
            "#.#",
            "#.#",
        ),
        'S' to arrayOf(
            ".##",
            "#..",
            ".#.",
            "..#",
            "##.",
        ),
        'T' to arrayOf(
            "###",
            ".#.",
            ".#.",
            ".#.",
            ".#.",
        ),
        'U' to arrayOf(
            "#.#",
            "#.#",
            "#.#",
            "#.#",
            "###",
        ),
        'V' to arrayOf(
            "#.#",
            "#.#",
            "#.#",
            "#.#",
            ".#.",
        ),
        'W' to arrayOf(
            "#...#",
            "#...#",
            "#.#.#",
            "##.##",
            "#...#",
        ),
        'X' to arrayOf(
            "#.#",
            "#.#",
            ".#.",
            "#.#",
            "#.#",
        ),
        'Y' to arrayOf(
            "#.#",
            "#.#",
            ".#.",
            ".#.",
            ".#.",
        ),
        'Z' to arrayOf(
            "###",
            "..#",
            ".#.",
            "#..",
            "###",
        ),
        '0' to arrayOf(
            ".#.",
            "#.#",
            "#.#",
            "#.#",
            ".#.",
        ),
        '1' to arrayOf(
            ".#.",
            "##.",
            ".#.",
            ".#.",
            "###",
        ),
        '2' to arrayOf(
            "##.",
            "..#",
            ".#.",
            "#..",
            "###",
        ),
        '3' to arrayOf(
            "##.",
            "..#",
            ".#.",
            "..#",
            "##.",
        ),
        '4' to arrayOf(
            "#.#",
            "#.#",
            "###",
            "..#",
            "..#",
        ),
        '5' to arrayOf(
            "###",
            "#..",
            "##.",
            "..#",
            "##.",
        ),
        '6' to arrayOf(
            ".#.",
            "#..",
            "##.",
            "#.#",
            ".#.",
        ),
        '7' to arrayOf(
            "###",
            "..#",
            ".#.",
            ".#.",
            ".#.",
        ),
        '8' to arrayOf(
            ".#.",
            "#.#",
            ".#.",
            "#.#",
            ".#.",
        ),
        '9' to arrayOf(
            ".#.",
            "#.#",
            ".##",
            "..#",
            ".#.",
        ),
        '!' to arrayOf(
            "#",
            "#",
            "#",
            ".",
            "#",
        ),
        '?' to arrayOf(
            ".#.",
            "#.#",
            ".#.",
            "...",
            ".#.",
        ),
        '.' to arrayOf(
            ".",
            ".",
            ".",
            ".",
            "#",
        ),
        ',' to arrayOf(
            ".",
            ".",
            ".",
            "#",
            "#",
        ),
        '-' to arrayOf(
            ".",
            ".",
            "#",
            ".",
            ".",
        ),
        ':' to arrayOf(
            ".",
            "#",
            ".",
            "#",
            ".",
        ),
    )
}
