package com.sajenko.glyphtoys.toys

/**
 * Pure interface for frame output above the Android bitmap and SDK layer.
 */
interface FrameSink {
    fun display(grid: PixelGrid)
}
