package com.sajenko.glyphtoys.toys

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

enum class LiveGlyphSource {
    COMPOSITE_TOY,
    STATIC_IMAGE_TOY,
}

enum class LiveGlyphMode {
    CALL,
    CLOCK,
    CUSTOM_IDLE,
    EQUALIZER,
    STATIC_IMAGE,
}

data class LiveGlyphFrame(
    val grid: PixelGrid,
    val source: LiveGlyphSource,
    val mode: LiveGlyphMode,
    val updatedAtElapsedMs: Long,
) {
    fun copyFrame(): LiveGlyphFrame {
        return copy(grid = grid.copy())
    }
}

object LiveGlyphPreview {
    fun interface Listener {
        fun onLiveGlyphFrame(frame: LiveGlyphFrame?)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = linkedSetOf<Listener>()
    private var latestFrame: LiveGlyphFrame? = null

    fun addListener(listener: Listener) {
        runOnMain {
            listeners.add(listener)
            listener.onLiveGlyphFrame(latestFrame?.copyFrame())
        }
    }

    fun removeListener(listener: Listener) {
        runOnMain {
            listeners.remove(listener)
        }
    }

    fun latestFrame(): LiveGlyphFrame? {
        return latestFrame?.copyFrame()
    }

    fun publish(grid: PixelGrid, source: LiveGlyphSource, mode: LiveGlyphMode) {
        val frame = LiveGlyphFrame(
            grid = grid.copy(),
            source = source,
            mode = mode,
            updatedAtElapsedMs = SystemClock.uptimeMillis(),
        )
        runOnMain {
            latestFrame = frame
            notifyListeners(frame)
        }
    }

    fun clear(source: LiveGlyphSource) {
        runOnMain {
            if (latestFrame?.source != source) {
                return@runOnMain
            }
            latestFrame = null
            notifyListeners(null)
        }
    }

    private fun notifyListeners(frame: LiveGlyphFrame?) {
        listeners.toList().forEach { listener ->
            listener.onLiveGlyphFrame(frame?.copyFrame())
        }
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}

fun DisplayMode.toLiveGlyphMode(): LiveGlyphMode {
    return when (this) {
        DisplayMode.CALL -> LiveGlyphMode.CALL
        DisplayMode.CLOCK -> LiveGlyphMode.CLOCK
        DisplayMode.CUSTOM_IDLE -> LiveGlyphMode.CUSTOM_IDLE
        DisplayMode.EQUALIZER -> LiveGlyphMode.EQUALIZER
    }
}
