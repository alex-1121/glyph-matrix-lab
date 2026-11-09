package com.sajenko.glyphtoys.toys

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixNTypeUtils
import com.nothinglondon.text.Font

/**
 * Debug-only spike: exercises the undocumented NType scrolling-text API from the unified
 * Glyph SDK on the 13x13 matrix. Logs support checks, frame shape/timing, and ASCII frames.
 * Not part of release builds; delete once a decision is made.
 */
class NTypeTextSpikeToyService : GlyphToyBase(TAG) {
    private var gmm: GlyphMatrixManager? = null
    private var animation: Thread? = null
    private var frameCount = 0
    private var startedAtMs = 0L

    override fun onServiceConnected(context: Context, gmm: GlyphMatrixManager) {
        this.gmm = gmm
        logSupport()
        start()
    }

    override fun onAod() {
        if (animation?.isAlive != true) start()
    }

    override fun onServiceDisconnected(context: Context) {
        animation?.interrupt()
        animation = null
        gmm = null
    }

    private fun logSupport() {
        runCatching {
            Log.i(TAG, "fontNames=${Font.getFontNames()}")
            SAMPLES.forEach { Log.i(TAG, "isNTypeSupport(\"$it\")=${GlyphMatrixNTypeUtils.isNTypeSupport(it)}") }
            Log.i(TAG, "measureAnimateTextOnce=${GlyphMatrixNTypeUtils.measureAnimateTextOnce(TEXT)}ms")
            val bmp = GlyphMatrixNTypeUtils.createBitmapFromText("12", 16f, 0f, false, GlyphMatrixNTypeUtils.TEXT_POSITION_CENTER)
            Log.i(TAG, "createBitmapFromText(\"12\") -> ${bmp?.width}x${bmp?.height}")
        }.onFailure { Log.e(TAG, "support probe failed", it) }
    }

    private fun start() {
        frameCount = 0
        startedAtMs = SystemClock.elapsedRealtime()
        animation = runCatching {
            GlyphMatrixNTypeUtils.animateText(TEXT, object : GlyphMatrixNTypeUtils.AnimationDataListener {
                override fun onAnimationData(data: IntArray) {
                    val n = frameCount++
                    runCatching { gmm?.setMatrixFrame(data) }
                        .onFailure { Log.e(TAG, "setMatrixFrame failed", it) }
                    if (n == 0) Log.i(TAG, "frame size=${data.size} min=${data.minOrNull()} max=${data.maxOrNull()}")
                    if (n % FRAME_LOG_EVERY == 0) Log.i(TAG, "frame #$n t=${SystemClock.elapsedRealtime() - startedAtMs}ms\n${ascii(data)}")
                }

                override fun onAnimationEnd() {
                    Log.i(TAG, "animation end after $frameCount frames, ${SystemClock.elapsedRealtime() - startedAtMs}ms")
                }
            })
        }.onFailure { Log.e(TAG, "animateText failed", it) }.getOrNull()
    }

    private fun ascii(data: IntArray): String {
        val side = kotlin.math.sqrt(data.size.toDouble()).toInt()
        if (side * side != data.size) return "(non-square: ${data.size})"
        return (0 until side).joinToString("\n") { y ->
            (0 until side).joinToString("") { x ->
                val v = data[y * side + x]
                when { v <= 0 -> "."; v < 128 -> "+"; else -> "#" }
            }
        }
    }

    private companion object {
        const val TAG = "NTypeSpike"
        const val TEXT = "MATRIX LAB 0123 ÄÖ"
        const val FRAME_LOG_EVERY = 6
        val SAMPLES = listOf("MATRIX LAB", "0123456789", "äöüßé", "Привет", "♪♫")
    }
}
