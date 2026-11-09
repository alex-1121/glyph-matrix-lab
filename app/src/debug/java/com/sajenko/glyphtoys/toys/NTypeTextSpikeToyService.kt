package com.sajenko.glyphtoys.toys

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixNTypeUtils
import com.nothinglondon.text.Font

/**
 * Debug-only reference toy for the undocumented NType text API in the unified Glyph SDK.
 *
 * Alternates two passes over the same text, forever:
 * 1. PUSH: `GlyphMatrixNTypeUtils.animateTextOnce` renders on its own thread and pushes
 *    169-value frames, which go straight to `setMatrixFrame(int[])`.
 * 2. PULL: a main-thread [Handler] tick (like `CompositeToyService`) calls
 *    [NTypeTextScroller.nextFrame] every [ScrollSpeed.tickDelayMs] and sends the
 *    [PixelGrid] (with anti-aliased brightness) through [GlyphDisplayAdapter].
 *
 * After each pull pass it logs how many pull frames exactly match (including brightness
 * levels) a frame from the previous push pass, frames per cycle and per-frame render time, under the `NTypeSpike` logcat tag.
 * Not part of release builds.
 */
class NTypeTextSpikeToyService : GlyphToyBase(TAG) {
    private val handler = Handler(Looper.getMainLooper())
    private var gmm: GlyphMatrixManager? = null
    private var sink: FrameSink? = null
    private var pushThread: Thread? = null
    private var scroller: TextFrameScroller? = null
    private var running = false

    private val pushFrames = mutableSetOf<String>()
    private var pushCount = 0
    private var pullCount = 0
    private var pullMatches = 0
    private var pullRenderTotalNs = 0L
    private var pullRenderMaxNs = 0L
    private var passStartedAtMs = 0L

    private val pullTick = object : Runnable {
        override fun run() {
            val s = scroller ?: return
            val t0 = System.nanoTime()
            val grid = s.nextFrame()
            val dt = System.nanoTime() - t0
            pullRenderTotalNs += dt
            pullRenderMaxNs = maxOf(pullRenderMaxNs, dt)
            if (key(grid) in pushFrames) pullMatches++
            if (pullCount == 0) Log.i(TAG, "pull first frame (expect blank)\n${ascii(grid)}")
            if (pullCount % FRAME_LOG_EVERY == 0) Log.d(TAG, "pull #$pullCount\n${ascii(grid)}")
            pullCount++
            runCatching { sink?.display(grid) }.onFailure { Log.e(TAG, "display failed", it) }

            if (s.hasFinishedCycle) {
                logPullSummary()
                handler.post { startPushPass() }
            } else if (running) {
                handler.postDelayed(this, ScrollSpeed.tickDelayMs(PULL_PX_PER_SEC))
            }
        }
    }

    override fun onServiceConnected(context: Context, gmm: GlyphMatrixManager) {
        this.gmm = gmm
        sink = GlyphDisplayAdapter(context, gmm)
        running = true
        logSupport()
        startPushPass()
    }

    override fun onAod() = Unit

    override fun onServiceDisconnected(context: Context) {
        running = false
        handler.removeCallbacksAndMessages(null)
        pushThread?.interrupt()
        pushThread = null
        scroller = null
        sink = null
        gmm = null
    }

    private fun logSupport() {
        runCatching {
            Log.i(TAG, "fontNames=${Font.getFontNames()}")
            SAMPLES.forEach { Log.i(TAG, "isNTypeSupport(\"$it\")=${GlyphMatrixNTypeUtils.isNTypeSupport(it)}") }
            Log.i(TAG, "measureAnimateTextOnce=${GlyphMatrixNTypeUtils.measureAnimateTextOnce(TEXT)}ms")
        }.onFailure { Log.e(TAG, "support probe failed", it) }
    }

    private fun startPushPass() {
        if (!running) return
        pushFrames.clear()
        pushCount = 0
        passStartedAtMs = SystemClock.elapsedRealtime()
        Log.i(TAG, "PUSH pass start: animateTextOnce(\"$TEXT\")")
        pushThread = runCatching {
            GlyphMatrixNTypeUtils.animateTextOnce(TEXT, object : GlyphMatrixNTypeUtils.AnimationDataListener {
                override fun onAnimationData(data: IntArray) {
                    val grid = NTypeTextScroller.gridFromViewPortData(data)
                    synchronized(pushFrames) { pushFrames.add(key(grid)) }
                    pushCount++
                    runCatching { gmm?.setMatrixFrame(data) }.onFailure { Log.e(TAG, "setMatrixFrame failed", it) }
                }

                override fun onAnimationEnd() {
                    val ms = SystemClock.elapsedRealtime() - passStartedAtMs
                    Log.i(TAG, "PUSH pass end: frames=$pushCount distinct=${pushFrames.size} ${ms}ms")
                    handler.post { startPullPass() }
                }
            })
        }.onFailure { Log.e(TAG, "animateTextOnce failed", it) }.getOrNull()
    }

    private fun startPullPass() {
        if (!running) return
        scroller = NTypeTextScroller.createOrNull(TEXT)
        if (scroller == null) {
            Log.e(TAG, "PULL pass skipped: NTypeTextScroller.createOrNull returned null")
            return
        }
        pullCount = 0
        pullMatches = 0
        pullRenderTotalNs = 0L
        pullRenderMaxNs = 0L
        passStartedAtMs = SystemClock.elapsedRealtime()
        Log.i(TAG, "PULL pass start: NTypeTextScroller at ${PULL_PX_PER_SEC}px/s")
        handler.post(pullTick)
    }

    private fun logPullSummary() {
        val ms = SystemClock.elapsedRealtime() - passStartedAtMs
        val avgUs = if (pullCount > 0) pullRenderTotalNs / pullCount / 1000 else 0
        Log.i(
            TAG,
            "PULL pass end: pull frames matching a push frame: $pullMatches/$pullCount, " +
                "hasFinishedCycle=${scroller?.hasFinishedCycle}, render avg=${avgUs}us " +
                "max=${pullRenderMaxNs / 1000}us, ${ms}ms",
        )
    }

    /** Exact frame identity including brightness levels. */
    private fun key(grid: PixelGrid): String =
        (0 until grid.size * grid.size).joinToString(",") { grid.brightness(it % grid.size, it / grid.size).toString() }

    private fun ascii(grid: PixelGrid): String = (0 until grid.size).joinToString("\n") { y ->
        (0 until grid.size).joinToString("") { x ->
            val b = grid.brightness(x, y)
            when { b == 0 -> "."; b < PixelGrid.MAX_BRIGHTNESS -> "+"; else -> "#" }
        }
    }

    private companion object {
        const val TAG = "NTypeSpike"
        // Compact's character set only, so both passes render identical strings.
        const val TEXT = "MATRIX LAB 0123"
        // Same speed as the SDK's own 4a Pro animation, for a side-by-side comparison.
        const val PULL_PX_PER_SEC = 24
        const val FRAME_LOG_EVERY = 6
        val SAMPLES = listOf("MATRIX LAB", "0123456789", "äöüßé", "Привет", "♪♫")
    }
}
