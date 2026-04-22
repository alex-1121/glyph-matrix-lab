package com.sajenko.glyphtoys.toys

import kotlin.math.roundToInt

enum class DisplayMode {
    CALL,
    CLOCK,
    CUSTOM_IDLE,
    EQUALIZER,
}

sealed class RenderResult {
    object Rendered : RenderResult()
    object Skipped : RenderResult()
    object NeedsEqualizerRender : RenderResult()
}

class CompositeToyController(
    private val frameSink: FrameSink,
    private val stateProvider: SystemStateProvider,
    private val timeProvider: TimeProvider = SystemTimeProvider,
    private val customGlyphProvider: CustomGlyphProvider = EmptyCustomGlyphProvider,
    private val liveFrameReporter: (PixelGrid, DisplayMode) -> Unit = { _, _ -> },
) {
    private var lastRenderedMode: DisplayMode? = null
    private var lastRenderedMinute = -1
    private var lastRenderedCallFrameIndex = -1
    private val smoothedHeights = FloatArray(EqualizerProcessor.BAR_COLUMNS.size)

    var equalizerAvailable = false

    fun currentMode(): DisplayMode {
        return when {
            stateProvider.isCallActive -> DisplayMode.CALL
            stateProvider.isMediaPlaying -> DisplayMode.EQUALIZER
            customGlyphProvider.idleImageGrid() != null -> DisplayMode.CUSTOM_IDLE
            else -> DisplayMode.CLOCK
        }
    }

    fun render(force: Boolean = false, callFrameIndex: Int = 0): RenderResult {
        val mode = currentMode()
        val minute = timeProvider.minute()
        val shouldRender = force ||
            mode != lastRenderedMode ||
            (mode == DisplayMode.CLOCK && minute != lastRenderedMinute) ||
            (mode == DisplayMode.CALL && callFrameIndex != lastRenderedCallFrameIndex)

        if (!shouldRender) {
            return RenderResult.Skipped
        }

        when (mode) {
            DisplayMode.CALL -> display(mode, FrameBuilders.buildCallGrid(callFrameIndex))
            DisplayMode.CLOCK -> display(
                mode,
                FrameBuilders.buildClockGrid(timeProvider.hour(), minute),
            )
            DisplayMode.CUSTOM_IDLE -> {
                val grid = customGlyphProvider.idleImageGrid()
                if (grid != null) {
                    display(mode, grid)
                } else {
                    display(DisplayMode.CLOCK, FrameBuilders.buildClockGrid(timeProvider.hour(), minute))
                }
            }
            DisplayMode.EQUALIZER -> {
                if (equalizerAvailable) {
                    return RenderResult.NeedsEqualizerRender
                }
                display(mode, FrameBuilders.buildEqualizerGrid(EqualizerProcessor.buildFallbackHeights()))
            }
        }

        lastRenderedMode = mode
        lastRenderedMinute = minute
        lastRenderedCallFrameIndex = if (mode == DisplayMode.CALL) callFrameIndex else -1
        return RenderResult.Rendered
    }

    fun renderEqualizer(rawHeights: IntArray, smooth: Boolean): Boolean {
        val displayHeights = IntArray(EqualizerProcessor.BAR_COLUMNS.size)
        EqualizerProcessor.BAR_COLUMNS.indices.forEach { bar ->
            val rawHeight = rawHeights.getOrElse(bar) { 0 }
                .toFloat()
                .coerceIn(0f, EqualizerProcessor.SIZE.toFloat())
            displayHeights[bar] = if (smooth) {
                smoothedHeights[bar] = maxOf(rawHeight, smoothedHeights[bar] * EqualizerProcessor.DECAY)
                smoothedHeights[bar].roundToInt().coerceIn(0, EqualizerProcessor.SIZE)
            } else {
                smoothedHeights[bar] = rawHeight
                rawHeight.roundToInt().coerceIn(0, EqualizerProcessor.SIZE)
            }
        }

        display(DisplayMode.EQUALIZER, FrameBuilders.buildEqualizerGrid(displayHeights))
        lastRenderedMode = DisplayMode.EQUALIZER
        lastRenderedMinute = timeProvider.minute()
        lastRenderedCallFrameIndex = -1
        return true
    }

    fun renderFallbackEqualizer() {
        display(DisplayMode.EQUALIZER, FrameBuilders.buildEqualizerGrid(EqualizerProcessor.buildFallbackHeights()))
        lastRenderedMode = DisplayMode.EQUALIZER
        lastRenderedMinute = timeProvider.minute()
        lastRenderedCallFrameIndex = -1
    }

    fun resetSmoothing() {
        smoothedHeights.fill(0f)
    }

    fun resetCallAnimation() {
        lastRenderedCallFrameIndex = -1
    }

    fun modeChanged(): Boolean = currentMode() != lastRenderedMode

    fun formatSmoothedHeights(): String {
        return smoothedHeights.joinToString(prefix = "[", postfix = "]") { value ->
            value.roundToInt().toString()
        }
    }

    private fun display(mode: DisplayMode, grid: PixelGrid) {
        frameSink.display(grid)
        liveFrameReporter(grid, mode)
    }
}
