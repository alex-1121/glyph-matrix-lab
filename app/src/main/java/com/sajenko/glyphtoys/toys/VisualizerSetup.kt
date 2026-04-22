package com.sajenko.glyphtoys.toys

import android.media.audiofx.Visualizer

internal fun interface WaveformCaptureListener {
    fun onWaveformDataCapture(waveform: ByteArray, samplingRate: Int)
}

internal interface VisualizerSetupAdapter {
    fun setEnabled(enabled: Boolean)

    fun setCaptureSize(captureSize: Int)

    fun setScalingMode(scalingMode: Int)

    fun setDataCaptureListener(
        listener: WaveformCaptureListener,
        captureRate: Int,
        waveform: Boolean,
        fft: Boolean,
    )
}

internal object VisualizerSetup {
    fun configure(
        visualizer: VisualizerSetupAdapter,
        captureSize: Int,
        scalingMode: Int,
        captureRate: Int,
        waveformListener: WaveformCaptureListener,
        onCaptureSizeFailure: ((IllegalStateException) -> Unit)? = null,
    ) {
        // setCaptureSize() throws IllegalStateException on some devices if called while enabled
        visualizer.setEnabled(false)
        try {
            visualizer.setCaptureSize(captureSize)
        } catch (exception: IllegalStateException) {
            onCaptureSizeFailure?.invoke(exception)
        }
        visualizer.setScalingMode(scalingMode)
        visualizer.setDataCaptureListener(
            listener = waveformListener,
            captureRate = captureRate,
            waveform = true,
            fft = false,
        )
        visualizer.setEnabled(true)
    }

    fun createConfiguredVisualizer(
        sessionId: Int,
        captureSize: Int,
        scalingMode: Int,
        captureRate: Int,
        waveformListener: WaveformCaptureListener,
        onCaptureSizeFailure: ((IllegalStateException) -> Unit)? = null,
    ): Visualizer {
        val visualizer = Visualizer(sessionId)
        try {
            configure(
                visualizer = AndroidVisualizerSetupAdapter(visualizer),
                captureSize = captureSize,
                scalingMode = scalingMode,
                captureRate = captureRate,
                waveformListener = waveformListener,
                onCaptureSizeFailure = onCaptureSizeFailure,
            )
        } catch (exception: RuntimeException) {
            visualizer.release()
            throw exception
        }
        return visualizer
    }
}

private class AndroidVisualizerSetupAdapter(
    private val visualizer: Visualizer,
) : VisualizerSetupAdapter {
    override fun setEnabled(enabled: Boolean) {
        visualizer.enabled = enabled
    }

    override fun setCaptureSize(captureSize: Int) {
        visualizer.captureSize = captureSize
    }

    override fun setScalingMode(scalingMode: Int) {
        visualizer.scalingMode = scalingMode
    }

    override fun setDataCaptureListener(
        listener: WaveformCaptureListener,
        captureRate: Int,
        waveform: Boolean,
        fft: Boolean,
    ) {
        visualizer.setDataCaptureListener(
            object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    visualizer: Visualizer,
                    waveform: ByteArray,
                    samplingRate: Int,
                ) {
                    listener.onWaveformDataCapture(waveform, samplingRate)
                }

                override fun onFftDataCapture(
                    visualizer: Visualizer,
                    fft: ByteArray,
                    samplingRate: Int,
                ) = Unit
            },
            captureRate,
            waveform,
            fft,
        )
    }
}
