package com.sajenko.glyphtoys.toys

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompositeToyControllerTest {
    private val fakeSink = FakeFrameSink()
    private val fakeState = FakeSystemStateProvider()
    private val fakeTime = FakeTimeProvider(currentHour = 12, currentMinute = 30)
    private val fakeCustomGlyphProvider = FakeCustomGlyphProvider()
    private val reportedFrames = mutableListOf<Pair<DisplayMode, PixelGrid>>()
    private lateinit var controller: CompositeToyController

    @Before
    fun setup() {
        fakeCustomGlyphProvider.grid = null
        reportedFrames.clear()
        controller = CompositeToyController(
            frameSink = fakeSink,
            stateProvider = fakeState,
            timeProvider = fakeTime,
            customGlyphProvider = fakeCustomGlyphProvider,
            liveFrameReporter = { grid, mode -> reportedFrames.add(mode to grid.copy()) },
        )
    }

    @Test
    fun `call takes precedence over media`() {
        fakeState.isCallActive = true
        fakeState.isMediaPlaying = true

        assertEquals(DisplayMode.CALL, controller.currentMode())
    }

    @Test
    fun `media without call shows EQUALIZER`() {
        fakeState.isCallActive = false
        fakeState.isMediaPlaying = true

        assertEquals(DisplayMode.EQUALIZER, controller.currentMode())
    }

    @Test
    fun `idle shows CLOCK`() {
        fakeState.isCallActive = false
        fakeState.isMediaPlaying = false

        assertEquals(DisplayMode.CLOCK, controller.currentMode())
    }

    @Test
    fun `idle with custom image shows CUSTOM_IDLE`() {
        fakeCustomGlyphProvider.grid = PixelGrid().apply { set(6, 6) }

        assertEquals(DisplayMode.CUSTOM_IDLE, controller.currentMode())

        val result = controller.render(force = true)

        assertEquals(RenderResult.Rendered, result)
        assertTrue(fakeSink.lastFrame()!!.get(6, 6))
        assertEquals(DisplayMode.CUSTOM_IDLE, reportedFrames.single().first)
    }

    @Test
    fun `call and media take precedence over custom idle image`() {
        fakeCustomGlyphProvider.grid = PixelGrid().apply { set(6, 6) }
        fakeState.isMediaPlaying = true

        assertEquals(DisplayMode.EQUALIZER, controller.currentMode())

        fakeState.isCallActive = true

        assertEquals(DisplayMode.CALL, controller.currentMode())
    }

    @Test
    fun `force render returns Rendered`() {
        val result = controller.render(force = true)

        assertEquals(RenderResult.Rendered, result)
        assertEquals(1, fakeSink.frameCount())

        val result2 = controller.render(force = true)

        assertEquals(RenderResult.Rendered, result2)
        assertEquals(2, fakeSink.frameCount())
    }

    @Test
    fun `same mode same minute returns Skipped`() {
        controller.render(force = true)

        assertEquals(1, fakeSink.frameCount())

        val result = controller.render(force = false)

        assertEquals(RenderResult.Skipped, result)
        assertEquals(1, fakeSink.frameCount())
    }

    @Test
    fun `clock mode re-renders on minute change`() {
        fakeTime.setTime(12, 30)
        controller.render(force = true)

        assertEquals(1, fakeSink.frameCount())

        fakeTime.setTime(12, 31)
        controller.render(force = false)

        assertEquals(2, fakeSink.frameCount())
    }

    @Test
    fun `mode change triggers render`() {
        controller.render(force = true)

        assertEquals(1, fakeSink.frameCount())

        fakeState.isMediaPlaying = true
        controller.equalizerAvailable = false
        controller.render(force = false)

        assertEquals(2, fakeSink.frameCount())
    }

    @Test
    fun `EQUALIZER mode without equalizer shows fallback bars`() {
        fakeState.isMediaPlaying = true
        controller.equalizerAvailable = false

        controller.render(force = true)

        val grid = fakeSink.lastFrame()!!
        EqualizerProcessor.BAR_COLUMNS.forEach { col ->
            for (row in 7 until 13) {
                assertTrue("Col $col row $row should be lit", grid.get(col, row))
            }
        }
    }

    @Test
    fun `EQUALIZER mode with equalizer returns NeedsEqualizerRender`() {
        fakeState.isMediaPlaying = true
        controller.equalizerAvailable = true

        val result = controller.render(force = true)

        assertEquals(RenderResult.NeedsEqualizerRender, result)
        assertEquals(0, fakeSink.frameCount())
    }

    @Test
    fun `same CALL frame rendered twice returns Skipped`() {
        fakeState.isCallActive = true

        controller.render(force = true, callFrameIndex = 0)

        val result = controller.render(force = false, callFrameIndex = 0)

        assertEquals(RenderResult.Skipped, result)
        assertEquals(1, fakeSink.frameCount())
    }

    @Test
    fun `changing CALL frame renders again`() {
        fakeState.isCallActive = true

        controller.render(force = true, callFrameIndex = 0)

        val result = controller.render(force = false, callFrameIndex = 1)

        assertEquals(RenderResult.Rendered, result)
        assertEquals(2, fakeSink.frameCount())
    }

    @Test
    fun `resetCallAnimation allows frame 0 to render again`() {
        fakeState.isCallActive = true

        controller.render(force = true, callFrameIndex = 0)
        controller.resetCallAnimation()

        val result = controller.render(force = false, callFrameIndex = 0)

        assertEquals(RenderResult.Rendered, result)
        assertEquals(2, fakeSink.frameCount())
    }

    @Test
    fun `smoothing applies decay to previous heights`() {
        val heights1 = intArrayOf(10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10)
        controller.renderEqualizer(heights1, smooth = false)

        val heights2 = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        controller.renderEqualizer(heights2, smooth = true)

        val grid = fakeSink.lastFrame()!!
        var litCount = 0
        for (row in 0 until 13) {
            if (grid.get(0, row)) {
                litCount++
            }
        }
        assertTrue("Expected ~6 lit pixels, got $litCount", litCount in 5..7)
    }

    @Test
    fun `resetSmoothing clears decay state`() {
        val heights = intArrayOf(10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10)
        controller.renderEqualizer(heights, smooth = false)

        controller.resetSmoothing()

        val zero = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        controller.renderEqualizer(zero, smooth = true)

        val grid = fakeSink.lastFrame()!!
        EqualizerProcessor.BAR_COLUMNS.forEach { col ->
            for (row in 0 until 13) {
                assertFalse("Col $col row $row should be off", grid.get(col, row))
            }
        }
    }

    private class FakeCustomGlyphProvider : CustomGlyphProvider {
        var grid: PixelGrid? = null

        override fun idleImageGrid(): PixelGrid? = grid
    }
}
