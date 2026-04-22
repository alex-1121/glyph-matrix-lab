package com.sajenko.glyphtoys.toys

import kotlin.math.roundToInt

enum class DisplayMode {
    CALL,
    CLOCK,
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
            DisplayMode.CALL -> frameSink.display(FrameBuilders.buildCallGrid(callFrameIndex))
            DisplayMode.CLOCK -> frameSink.display(
                FrameBuilders.buildClockGrid(timeProvider.hour(), minute),
            )
            DisplayMode.EQUALIZER -> {
                if (equalizerAvailable) {
                    return RenderResult.NeedsEqualizerRender
                }
                frameSink.display(FrameBuilders.buildEqualizerGrid(EqualizerProcessor.buildFallbackHeights()))
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

        frameSink.display(FrameBuilders.buildEqualizerGrid(displayHeights))
        lastRenderedMode = DisplayMode.EQUALIZER
        lastRenderedMinute = timeProvider.minute()
        lastRenderedCallFrameIndex = -1
        return true
    }

    fun renderFallbackEqualizer() {
        frameSink.display(FrameBuilders.buildEqualizerGrid(EqualizerProcessor.buildFallbackHeights()))
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
}
