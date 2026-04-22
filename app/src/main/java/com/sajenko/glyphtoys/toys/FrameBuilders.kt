package com.sajenko.glyphtoys.toys

object FrameBuilders {
    const val CALL_FRAME_COUNT = 3

    fun buildClockGrid(hour: Int, minute: Int): PixelGrid {
        val grid = PixelGrid()
        drawDigit(grid, hour / 10, 2, 1)
        drawDigit(grid, hour % 10, 7, 1)
        drawDigit(grid, minute / 10, 2, 7)
        drawDigit(grid, minute % 10, 7, 7)
        return grid
    }

    fun buildCallGrid(frameIndex: Int = 0): PixelGrid {
        val rows = CALL_FRAMES[frameIndex.coerceIn(0, CALL_FRAME_COUNT - 1)]
        return buildGrid(rows, litPixel = '#')
    }

    fun buildEqualizerGrid(heights: IntArray): PixelGrid {
        val grid = PixelGrid()
        EqualizerProcessor.BAR_COLUMNS.indices.forEach { bar ->
            val height = heights.getOrElse(bar) { 0 }.coerceIn(0, EqualizerProcessor.SIZE)
            if (height == 0) {
                return@forEach
            }

            val column = EqualizerProcessor.BAR_COLUMNS[bar]
            for (row in (EqualizerProcessor.SIZE - height) until EqualizerProcessor.SIZE) {
                grid.set(column, row)
            }
        }
        return grid
    }

    fun buildDollarIconGrid(): PixelGrid {
        return buildGrid(DOLLAR_EXAMPLE.first(), litPixel = 'o')
    }

    fun buildLabIconGrid(): PixelGrid {
        return buildGrid(MATRIX_LAB_ICON.first(), litPixel = 'o')
    }

    fun buildAppLogoGrid(): PixelGrid {
        return buildGrid(CROSS_EXAMPLE.first(), litPixel = 'o')
    }

    private fun buildGrid(rows: Array<String>, litPixel: Char): PixelGrid {
        val grid = PixelGrid()
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, pixel ->
                if (pixel == litPixel) {
                    grid.set(x, y)
                }
            }
        }
        return grid
    }

    private fun drawDigit(grid: PixelGrid, digit: Int, xOff: Int, yOff: Int) {
        val rows = FONT.getOrNull(digit) ?: return
        rows.forEachIndexed { row, pattern ->
            pattern.forEachIndexed { col, pixel ->
                if (pixel == '#') {
                    grid.set(xOff + col, yOff + row)
                }
            }
        }
    }

    private val MATRIX_LAB_ICON = arrayOf(
        arrayOf(
            "xxxx.....xxxx",
            "xx..ooooo..xx",
            "x..o.....o..x",
            "x...o...o...x",
            "....o...o....",
            "....o...o....",
            "....o...o....",
            "...o.....o...",
            "..o.......o..",
            "xo.........ox",
            "xo.........ox",
            "xxoooooooooxx",
            "xxxx.....xxxx",
        ),
    )

    private val CROSS_EXAMPLE = arrayOf(
        arrayOf(
            "xxxx.....xxxx",
            "xx....o....xx",
            "x.....o.....x",
            "x..ooooooo..x",
            "...o..o..o...",
            "...o..o..o...",
            ".ooooooooooo.",
            "...o..o..o...",
            "...o..o..o...",
            "x..ooooooo..x",
            "x.....o.....x",
            "xx....o....xx",
            "xxxx.....xxxx",

        ),
    )

    private val DOLLAR_EXAMPLE = arrayOf(
        arrayOf(
            "xxxx.....xxxx",
            "xx....o....xx",
            "x...ooooo...x",
            "x..oo.o.o...x",
            "...oo.o.oo...",
            "...oooo......",
            "....ooooo....",
            "......oooo...",
            "......o.oo...",
            "x..oo.o.oo..x",
            "x...ooooo...x",
            "xx....o....xx",
            "xxxx.....xxxx",
        ),
    )

    private val CALL_FRAMES = arrayOf(
        arrayOf(
            ".............",
            ".............",
            "..##.........",
            ".#..#........",
            ".#..#........",
            ".#..#........",
            "..#..#.......",
            "...#..#......",
            "....#..###...",
            ".....#....#..",
            "......#...#..",
            ".......###...",
            ".............",
        ),
        arrayOf(
            ".............",
            ".....##......",
            "..##...#.....",
            ".#..#...#....",
            ".#..#...#....",
            ".#..#........",
            "..#..#.......",
            "...#..#......",
            "....#..###...",
            ".....#....#..",
            "......#...#..",
            ".......###...",
            ".............",
        ),
        arrayOf(
            ".....###.....",
            "........#....",
            "..##.....#...",
            ".#..#....#...",
            ".#..#....#...",
            ".#..#........",
            "..#..#.......",
            "...#..#......",
            "....#..###...",
            ".....#....#..",
            "......#...#..",
            ".......###...",
            ".............",
        ),
    )

    private val FONT = arrayOf(
        arrayOf(
            ".##.",
            "#..#",
            "#..#",
            "#..#",
            ".##.",
        ),
        arrayOf(
            "..#.",
            ".##.",
            "#.#.",
            "..#.",
            "..#.",
        ),
        arrayOf(
            "###.",
            "...#",
            ".##.",
            "#...",
            "####",
        ),
        arrayOf(
            "###.",
            "...#",
            "###.",
            "...#",
            "###.",
        ),
        arrayOf(
            "#..#",
            "#..#",
            ".###",
            "...#",
            "...#",
        ),
        arrayOf(
            "####",
            "#...",
            "###.",
            "...#",
            "###.",
        ),
        arrayOf(
            ".##.",
            "#...",
            "###.",
            "#..#",
            ".##.",
        ),
        arrayOf(
            "###.",
            "...#",
            "...#",
            "...#",
            "...#",
        ),
        arrayOf(
            ".##.",
            "#..#",
            ".##.",
            "#..#",
            ".##.",
        ),
        arrayOf(
            "###.",
            "#..#",
            "####",
            "...#",
            "###.",
        ),
    )
}
