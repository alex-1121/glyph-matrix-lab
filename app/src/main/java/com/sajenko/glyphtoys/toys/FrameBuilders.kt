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
        val grid = PixelGrid()
        val rows = CALL_FRAMES[frameIndex.coerceIn(0, CALL_FRAME_COUNT - 1)]
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, pixel ->
                if (pixel == '#') {
                    grid.set(x, y)
                }
            }
        }
        return grid
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
