package com.sajenko.glyphtoys.toys

class FakeFrameSink : FrameSink {
    val frames = mutableListOf<PixelGrid>()

    override fun display(grid: PixelGrid) {
        frames.add(grid.copy())
    }

    fun lastFrame(): PixelGrid? = frames.lastOrNull()

    fun frameCount(): Int = frames.size

    fun clear() {
        frames.clear()
    }
}
