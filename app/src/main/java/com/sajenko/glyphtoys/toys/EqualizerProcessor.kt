package com.sajenko.glyphtoys.toys

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

object EqualizerProcessor {
    const val SIZE = 13
    const val MIN_DB = -36f
    const val MIN_WAVEFORM_AMPLITUDE = 1f
    const val MAX_WAVEFORM_AMPLITUDE = 128f
    const val PEAK_WEIGHT = 0.85f
    const val DECAY = 0.55f
    const val FALLBACK_BAR_HEIGHT = 6

    val BAR_COLUMNS = (0..12).toList().toIntArray()
    val ZERO_HEIGHTS = IntArray(BAR_COLUMNS.size)

    fun computeWaveformHeights(waveform: ByteArray): IntArray {
        if (waveform.isEmpty()) {
            return ZERO_HEIGHTS
        }

        return IntArray(BAR_COLUMNS.size) { bar ->
            val start = (bar * waveform.size) / BAR_COLUMNS.size
            val end = (((bar + 1) * waveform.size) / BAR_COLUMNS.size).coerceAtLeast(start + 1)
            var peakAmplitude = 0f
            var squaredAmplitudeSum = 0f

            for (index in start until end) {
                val centeredSample = ((waveform[index].toInt() and 0xFF) - 128).toFloat()
                val amplitude = abs(centeredSample)
                peakAmplitude = maxOf(peakAmplitude, amplitude)
                squaredAmplitudeSum += centeredSample * centeredSample
            }

            val sampleCount = (end - start).coerceAtLeast(1)
            val rmsAmplitude = sqrt(squaredAmplitudeSum / sampleCount)
            amplitudeToHeight(maxOf(rmsAmplitude, peakAmplitude * PEAK_WEIGHT))
        }
    }

    fun amplitudeToHeight(amplitude: Float): Int {
        if (amplitude <= MIN_WAVEFORM_AMPLITUDE) {
            return 0
        }

        val clampedAmplitude = amplitude.coerceIn(MIN_WAVEFORM_AMPLITUDE, MAX_WAVEFORM_AMPLITUDE)
        val db = 20f * log10(clampedAmplitude / MAX_WAVEFORM_AMPLITUDE)
        val normalized = ((db - MIN_DB) / -MIN_DB).coerceIn(0f, 1f)
        val scaled = normalized * SIZE
        return scaled.roundToInt().coerceIn(0, SIZE)
    }

    fun applyDecay(previous: FloatArray, current: IntArray, decay: Float = DECAY): FloatArray {
        require(previous.size == current.size)

        return FloatArray(previous.size) { index ->
            maxOf(current[index].toFloat(), previous[index] * decay)
        }
    }

    fun buildFallbackHeights(): IntArray = IntArray(BAR_COLUMNS.size) { FALLBACK_BAR_HEIGHT }
}
