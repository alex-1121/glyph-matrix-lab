package com.sajenko.glyphtoys.toys

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollCycleTest {
    @Test
    fun `starts with content fully off the right edge`() {
        val cycle = ScrollCycle(contentWidth = 20)

        assertEquals(-13, cycle.offset)
        assertFalse(cycle.hasFinishedCycle)
    }

    @Test
    fun `advance moves one pixel per call`() {
        val cycle = ScrollCycle(contentWidth = 20)

        cycle.advance()
        cycle.advance()

        assertEquals(-11, cycle.offset)
    }

    @Test
    fun `wraps to start once content has left the left edge`() {
        val cycle = ScrollCycle(contentWidth = 20)

        repeat(13 + 19) { cycle.advance() }
        assertEquals(19, cycle.offset)
        assertFalse(cycle.hasFinishedCycle)

        cycle.advance()
        assertEquals(-13, cycle.offset)
        assertTrue(cycle.hasFinishedCycle)
    }

    @Test
    fun `one full cycle is display plus content width frames`() {
        val cycle = ScrollCycle(contentWidth = 20)
        var frames = 0

        while (!cycle.hasFinishedCycle) {
            cycle.advance()
            frames++
        }

        assertEquals(33, frames)
    }

    @Test
    fun `reset returns to start and clears finished flag`() {
        val cycle = ScrollCycle(contentWidth = 5)
        repeat(30) { cycle.advance() }

        cycle.reset()

        assertEquals(-13, cycle.offset)
        assertFalse(cycle.hasFinishedCycle)
    }
}
