package com.sajenko.glyphtoys.toys

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollSpeedTest {
    @Test
    fun `clamp keeps values inside 2 to 40 px per second`() {
        assertEquals(2, ScrollSpeed.clamp(0))
        assertEquals(2, ScrollSpeed.clamp(-5))
        assertEquals(2, ScrollSpeed.clamp(1))
        assertEquals(20, ScrollSpeed.clamp(20))
        assertEquals(40, ScrollSpeed.clamp(500))
    }

    @Test
    fun `tick delay is one pixel per tick`() {
        assertEquals(50L, ScrollSpeed.tickDelayMs(20))
        assertEquals(125L, ScrollSpeed.tickDelayMs(8))
        assertEquals(500L, ScrollSpeed.tickDelayMs(2))
        assertEquals(25L, ScrollSpeed.tickDelayMs(40))
    }

    @Test
    fun `tick delay uses clamped speed`() {
        assertEquals(500L, ScrollSpeed.tickDelayMs(0))
        assertEquals(25L, ScrollSpeed.tickDelayMs(1000))
    }
}
