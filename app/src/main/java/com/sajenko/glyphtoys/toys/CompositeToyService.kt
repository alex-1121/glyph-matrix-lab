package com.sajenko.glyphtoys.toys

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.nothing.ketchum.GlyphMatrixManager

open class CompositeToyService : GlyphToyBase("CompositeToy") {

    private val handler = Handler(Looper.getMainLooper())
    private var audioManager: AudioManager? = null
    private var visualizer: Visualizer? = null
    private var isPlaybackActive = false
    private var isCallActive = false
    private var modeChangedListener: AudioManager.OnModeChangedListener? = null
    private var equalizerAvailable = false
    private var equalizerRetryAttempts = 0
    private var equalizerRetryScheduled = false
    private var equalizerRetryLimitLogged = false
    private var controller: CompositeToyController? = null
    private var callAnimationStep = 0

    @Volatile
    private var latestRawHeights: IntArray = IntArray(EqualizerProcessor.BAR_COLUMNS.size)

    @Volatile
    private var lastWaveformTimestampMs: Long = 0L
    private var lastRestartAttemptMs: Long = 0L
    private var startupHealthCheckGeneration = 0L
    private var startupHealthCheckRunnable: Runnable? = null

    private var renderTickScheduled = false

    private val minuteTickRunnable = object : Runnable {
        override fun run() {
            renderCurrentState(force = true)
            scheduleNextMinuteTick()
        }
    }

    private val callAnimationRunnable = object : Runnable {
        override fun run() {
            if (!isCallActive) {
                return
            }
            callAnimationStep = (callAnimationStep + 1) % CALL_FRAME_SEQUENCE.size
            renderCurrentState(force = false)
            scheduleCallAnimation()
        }
    }

    private val equalizerRetryRunnable = Runnable {
        equalizerRetryScheduled = false
        if (!isPlaybackActive || equalizerAvailable || isCallActive) {
            return@Runnable
        }
        Log.i(
            TAG,
            "Retrying equalizer startup ($equalizerRetryAttempts/$MAX_EQUALIZER_RETRIES) while playback remains active",
        )
        startEqualizer()
    }

    private val renderTickRunnable = object : Runnable {
        override fun run() {
            renderTickScheduled = false
            if (!isPlaybackActive || !equalizerAvailable) {
                return
            }
            val now = SystemClock.uptimeMillis()
            val ageMs = now - lastWaveformTimestampMs
            val inputHeights = currentInputHeights(ageMs)
            renderEqualizer(inputHeights, smooth = true)

            if (ageMs >= INPUT_GIVE_UP_MS) {
                val am = audioManager
                val stillActive = am == null || detectPlaybackActive(am)
                if (!stillActive) {
                    Log.i(
                        TAG,
                        "Waveform stale ${ageMs}ms: playback inactive on re-poll, dropping to CLOCK",
                    )
                    isPlaybackActive = false
                    stopEqualizer()
                    renderCurrentState(force = true)
                    return
                }
            }
            if (ageMs >= WAVEFORM_RESTART_MS && (now - lastRestartAttemptMs) >= WAVEFORM_RESTART_MS) {
                lastRestartAttemptMs = now
                Log.i(TAG, "Waveform stale ${ageMs}ms: restarting Visualizer")
                startEqualizer()
                return
            }
            scheduleRenderTick()
        }
    }

    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
            val nextPlaybackState = configs.isNotEmpty() //DO NOT UNCOMMENT, THIS WORKS || audioManager?.isMusicActive == true
            Log.i(
                TAG,
                "Playback config changed: nextPlaybackState=$nextPlaybackState configCount=${configs.size}",
            )
            if (nextPlaybackState == isPlaybackActive) {
                if (nextPlaybackState && !equalizerAvailable && !isCallActive) {
                    Log.i(TAG, "Playback is active but meter is unavailable; retrying waveform meter startup")
                    startEqualizer()
                }
                return
            }
            isPlaybackActive = nextPlaybackState
            if (nextPlaybackState && !isCallActive) {
                startEqualizer()
            } else {
                stopEqualizer()
            }
            renderCurrentState(force = true)
        }
    }

    override fun onServiceConnected(context: Context, gmm: GlyphMatrixManager) {
        val stateProvider = object : SystemStateProvider {
            override val isCallActive: Boolean
                get() = this@CompositeToyService.isCallActive

            override val isMediaPlaying: Boolean
                get() = this@CompositeToyService.isPlaybackActive
        }
        controller = CompositeToyController(
            frameSink = GlyphDisplayAdapter(context, gmm),
            stateProvider = stateProvider,
        )
        val nextAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager = nextAudioManager
        isCallActive = nextAudioManager.isCallLikeMode()
        val listener = AudioManager.OnModeChangedListener { newMode ->
            val nextIsCallActive = newMode == AudioManager.MODE_IN_CALL ||
                newMode == AudioManager.MODE_IN_COMMUNICATION
            if (nextIsCallActive != isCallActive) {
                isCallActive = nextIsCallActive
                if (nextIsCallActive) {
                    stopEqualizer()
                    startCallAnimation()
                } else if (isPlaybackActive) {
                    stopCallAnimation()
                    startEqualizer()
                    renderCurrentState(force = true)
                } else {
                    stopCallAnimation()
                    renderCurrentState(force = true)
                }
            }
        }
        modeChangedListener = listener
        nextAudioManager.addOnModeChangedListener(
            ContextCompat.getMainExecutor(context),
            listener,
        )
        nextAudioManager.registerAudioPlaybackCallback(playbackCallback, handler)
        isPlaybackActive = detectPlaybackActive(nextAudioManager)
        Log.i(TAG, "Service connected: playbackActive=$isPlaybackActive callActive=$isCallActive")
        if (isPlaybackActive && !isCallActive) {
            startEqualizer()
        } else {
            stopEqualizer()
        }
        if (isCallActive) {
            startCallAnimation()
        } else {
            stopCallAnimation()
            renderCurrentState(force = true)
        }
        scheduleNextMinuteTick()
    }

    override fun onServiceDisconnected(context: Context) {
        handler.removeCallbacks(minuteTickRunnable)
        cancelRenderTick()
        stopCallAnimation()
        modeChangedListener?.let { audioManager?.removeOnModeChangedListener(it) }
        modeChangedListener = null
        audioManager?.unregisterAudioPlaybackCallback(playbackCallback)
        stopEqualizer()
        audioManager = null
        isPlaybackActive = false
        isCallActive = false
        controller = null
    }

    override fun onAod() {
        val mode = currentDisplayMode()
        Log.i(TAG, "onAod mode=$mode smoothedHeights=${controller?.formatSmoothedHeights() ?: "[]"}")
        renderCurrentState(force = true)
    }

    private fun renderCurrentState(force: Boolean = false) {
        val currentController = controller ?: return
        currentController.equalizerAvailable = equalizerAvailable
        when (currentController.render(force, callFrameIndex = CALL_FRAME_SEQUENCE[callAnimationStep])) {
            RenderResult.Rendered -> {
                if (currentController.currentMode() == DisplayMode.EQUALIZER && !equalizerAvailable) {
                    ensureEqualizerRecoveryScheduled("fallback render")
                }
            }
            RenderResult.Skipped -> Unit
            RenderResult.NeedsEqualizerRender -> {
                currentController.renderEqualizer(currentInputHeights(), smooth = true)
            }
        }
    }

    private fun scheduleNextMinuteTick() {
        handler.removeCallbacks(minuteTickRunnable)
        val now = System.currentTimeMillis()
        val delay = 60_000L - (now % 60_000L)
        handler.postDelayed(minuteTickRunnable, if (delay == 0L) 60_000L else delay)
    }

    private fun startCallAnimation() {
        controller?.resetCallAnimation()
        callAnimationStep = 0
        renderCurrentState(force = true)
        scheduleCallAnimation()
    }

    private fun stopCallAnimation() {
        handler.removeCallbacks(callAnimationRunnable)
        callAnimationStep = 0
        controller?.resetCallAnimation()
    }

    private fun scheduleCallAnimation() {
        handler.removeCallbacks(callAnimationRunnable)
        if (!isCallActive) {
            return
        }
        handler.postDelayed(callAnimationRunnable, CALL_FRAME_INTERVAL_MS)
    }

    private fun detectPlaybackActive(audioManager: AudioManager): Boolean {
        return audioManager.getActivePlaybackConfigurations().isNotEmpty() || audioManager.isMusicActive
    }

    private fun AudioManager.isCallLikeMode(): Boolean {
        return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
    }

    private fun currentDisplayMode(): DisplayMode = controller?.currentMode() ?: when {
        isCallActive -> DisplayMode.CALL
        isPlaybackActive -> DisplayMode.EQUALIZER
        else -> DisplayMode.CLOCK
    }

    private fun startEqualizer() {
        cancelStartupHealthCheck()
        releaseVisualizer(visualizer)
        resetMeterState()
        if (!isPlaybackActive || isCallActive) {
            return
        }
        val captureSizeRange = Visualizer.getCaptureSizeRange()
        val captureSize = CAPTURE_SIZE.coerceIn(captureSizeRange[0], captureSizeRange[1])
        val permissionGranted = hasRecordAudioPermission()
        Log.i(
            TAG,
            "Starting waveform meter with permissionGranted=$permissionGranted captureSize=$captureSize captureRange=${captureSizeRange.contentToString()} maxCaptureRate=${Visualizer.getMaxCaptureRate()} retryAttempt=$equalizerRetryAttempts/$MAX_EQUALIZER_RETRIES",
        )
        if (!permissionGranted) {
            onEqualizerStartFailure("RECORD_AUDIO permission missing")
            return
        }
        var nextVisualizer: Visualizer? = null
        try {
            nextVisualizer = VisualizerSetup.createConfiguredVisualizer(
                sessionId = 0,
                captureSize = captureSize,
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED,
                captureRate = ((1000L / CAPTURE_RATE_MS).toInt() * 1000)
                    .coerceAtMost(Visualizer.getMaxCaptureRate()),
                waveformListener = WaveformCaptureListener { waveform, _ ->
                    latestRawHeights = EqualizerProcessor.computeWaveformHeights(waveform)
                    lastWaveformTimestampMs = SystemClock.uptimeMillis()
                },
                onCaptureSizeFailure = { exception ->
                    Log.w(
                        TAG,
                        "Visualizer rejected captureSize=$captureSize during startup; continuing with default capture size (${exception.javaClass.simpleName}: ${exception.message})",
                        exception,
                    )
                },
            )
            visualizer = nextVisualizer
            latestRawHeights = EqualizerProcessor.ZERO_HEIGHTS
            controller?.resetSmoothing()
            equalizerAvailable = true
            val startedAt = SystemClock.uptimeMillis()
            lastWaveformTimestampMs = startedAt
            lastRestartAttemptMs = startedAt
            cancelPendingEqualizerRetry(resetAttempts = true)
            scheduleStartupHealthCheck(startedAt)
            Log.i(TAG, "Waveform meter initialized successfully")
        } catch (exception: SecurityException) {
            releaseVisualizer(nextVisualizer)
            onEqualizerStartFailure("Visualizer rejected access", exception)
            return
        } catch (exception: RuntimeException) {
            releaseVisualizer(nextVisualizer)
            onEqualizerStartFailure("Visualizer startup runtime failure", exception)
            return
        }
        renderEqualizer(EqualizerProcessor.ZERO_HEIGHTS, smooth = false)
        scheduleRenderTick()
    }

    private fun stopEqualizer() {
        cancelStartupHealthCheck()
        cancelRenderTick()
        cancelPendingEqualizerRetry(resetAttempts = true)
        releaseVisualizer(visualizer)
        resetMeterState()
    }

    private fun scheduleRenderTick() {
        if (renderTickScheduled) {
            return
        }
        renderTickScheduled = true
        handler.postDelayed(renderTickRunnable, RENDER_TICK_MS)
    }

    private fun cancelRenderTick() {
        handler.removeCallbacks(renderTickRunnable)
        renderTickScheduled = false
    }

    private fun scheduleStartupHealthCheck(startedAt: Long) {
        cancelStartupHealthCheck()
        val generation = ++startupHealthCheckGeneration
        val runnable = Runnable {
            startupHealthCheckRunnable = null
            if (generation != startupHealthCheckGeneration) {
                return@Runnable
            }
            val audioManager = audioManager
            val playbackStillActive = isPlaybackActive &&
                (audioManager == null || detectPlaybackActive(audioManager))
            if (!playbackStillActive || isCallActive) {
                return@Runnable
            }
            if (!equalizerAvailable || visualizer == null || lastWaveformTimestampMs != startedAt) {
                return@Runnable
            }
            Log.w(
                TAG,
                "No waveform received within ${STARTUP_HEALTH_CHECK_DELAY_MS}ms of Visualizer startup; restarting meter",
            )
            startEqualizer()
        }
        startupHealthCheckRunnable = runnable
        handler.postDelayed(runnable, STARTUP_HEALTH_CHECK_DELAY_MS)
    }

    private fun cancelStartupHealthCheck() {
        startupHealthCheckGeneration++
        startupHealthCheckRunnable?.let(handler::removeCallbacks)
        startupHealthCheckRunnable = null
    }

    private fun renderEqualizer(rawHeights: IntArray, smooth: Boolean = true) {
        controller?.renderEqualizer(rawHeights, smooth)
    }

    private fun renderFallbackEqualizer() {
        controller?.renderFallbackEqualizer()
    }

    private fun currentInputHeights(ageMs: Long = SystemClock.uptimeMillis() - lastWaveformTimestampMs): IntArray {
        return if (ageMs < INPUT_SOFT_STALE_MS) latestRawHeights else EqualizerProcessor.ZERO_HEIGHTS
    }

    private fun ensureEqualizerRecoveryScheduled(reason: String) {
        if (!isPlaybackActive || equalizerAvailable || visualizer != null || isCallActive) {
            return
        }
        scheduleEqualizerRetry(reason)
    }

    private fun scheduleEqualizerRetry(reason: String) {
        if (equalizerRetryScheduled) {
            return
        }
        if (equalizerRetryAttempts >= MAX_EQUALIZER_RETRIES) {
            if (!equalizerRetryLimitLogged) {
                Log.w(
                    TAG,
                    "Equalizer retry budget exhausted after $MAX_EQUALIZER_RETRIES attempts; keeping fallback bars while playback stays active",
                )
                equalizerRetryLimitLogged = true
            }
            return
        }
        equalizerRetryScheduled = true
        equalizerRetryAttempts++
        Log.i(
            TAG,
            "Scheduling equalizer retry $equalizerRetryAttempts/$MAX_EQUALIZER_RETRIES in ${EQUALIZER_RETRY_DELAY_MS}ms because $reason",
        )
        handler.postDelayed(equalizerRetryRunnable, EQUALIZER_RETRY_DELAY_MS)
    }

    private fun cancelPendingEqualizerRetry(resetAttempts: Boolean) {
        handler.removeCallbacks(equalizerRetryRunnable)
        equalizerRetryScheduled = false
        if (resetAttempts) {
            equalizerRetryAttempts = 0
            equalizerRetryLimitLogged = false
        }
    }

    private fun releaseVisualizer(target: Visualizer?) {
        val currentVisualizer = target ?: return
        try {
            currentVisualizer.enabled = false
        } catch (exception: RuntimeException) {
            Log.w(
                TAG,
                "Failed to disable Visualizer cleanly before release: ${exception.javaClass.simpleName}: ${exception.message}",
                exception,
            )
        }
        try {
            currentVisualizer.release()
        } catch (exception: RuntimeException) {
            Log.w(
                TAG,
                "Failed to release Visualizer cleanly: ${exception.javaClass.simpleName}: ${exception.message}",
                exception,
            )
        }
    }

    private fun resetMeterState() {
        visualizer = null
        equalizerAvailable = false
        latestRawHeights = EqualizerProcessor.ZERO_HEIGHTS
        lastWaveformTimestampMs = 0L
        lastRestartAttemptMs = 0L
        controller?.resetSmoothing()
    }

    private fun onEqualizerStartFailure(reason: String, exception: Exception? = null) {
        resetMeterState()
        if (exception == null) {
            Log.w(TAG, "Waveform meter unavailable: $reason")
        } else {
            Log.w(
                TAG,
                "Waveform meter unavailable: $reason (${exception.javaClass.simpleName}: ${exception.message})",
                exception,
            )
        }
        renderFallbackEqualizer()
        ensureEqualizerRecoveryScheduled(reason)
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val TAG = "CompositeToyService"
        const val CAPTURE_RATE_MS = 50L
        const val CAPTURE_SIZE = 256
        const val CALL_FRAME_INTERVAL_MS = 500L
        const val EQUALIZER_RETRY_DELAY_MS = 1_500L
        val CALL_FRAME_SEQUENCE = intArrayOf(0, 1, 2)
        const val MAX_EQUALIZER_RETRIES = 10
        const val RENDER_TICK_MS = 50L
        const val STARTUP_HEALTH_CHECK_DELAY_MS = 1_000L
        const val INPUT_SOFT_STALE_MS = 250L
        const val WAVEFORM_RESTART_MS = 2_000L
        const val INPUT_GIVE_UP_MS = 5_000L
    }
}
