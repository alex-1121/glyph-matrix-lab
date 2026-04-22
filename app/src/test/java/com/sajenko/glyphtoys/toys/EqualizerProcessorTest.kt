package com.sajenko.glyphtoys.toys

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerProcessorTest {
    @Test
    fun `constants match production values`() {
        assertEquals(-36f, EqualizerProcessor.MIN_DB)
        assertEquals(0.85f, EqualizerProcessor.PEAK_WEIGHT)
        assertEquals(0.55f, EqualizerProcessor.DECAY)
        assertEquals(6, EqualizerProcessor.FALLBACK_BAR_HEIGHT)
    }

    @Test
    fun `empty waveform returns zero heights`() {
        val heights = EqualizerProcessor.computeWaveformHeights(ByteArray(0))
        assertArrayEquals(EqualizerProcessor.ZERO_HEIGHTS, heights)
    }

    @Test
    fun `silent waveform returns zero heights`() {
        val silent = ByteArray(256) { 128.toByte() }
        val heights = EqualizerProcessor.computeWaveformHeights(silent)
        assertTrue(heights.all { it == 0 })
    }

    @Test
    fun `non-silent waveform produces 13 distinct heights`() {
        val waveform = ByteArray(256) { index -> (128 + (index / 4)).coerceAtMost(255).toByte() }
        val heights = EqualizerProcessor.computeWaveformHeights(waveform)

        assertEquals(13, heights.size)
        assertTrue("Last bar should be taller than first", heights.last() > heights.first())
    }

    @Test
    fun `decay preserves higher previous values`() {
        val previous = floatArrayOf(10f, 5f, 8f, 1f, 12f, 0f, 7f, 2f, 4f, 9f, 3f, 6f, 11f)
        val current = intArrayOf(0, 6, 4, 0, 1, 5, 8, 3, 4, 0, 10, 2, 1)

        val result = EqualizerProcessor.applyDecay(previous, current)

        assertArrayEquals(
            floatArrayOf(5.5f, 6f, 4.4f, 0.55f, 6.6f, 5f, 8f, 3f, 4f, 4.95f, 10f, 3.3f, 6.05f),
            result,
            0.01f,
        )
    }
}
