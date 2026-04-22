package com.sajenko.glyphtoys.toys

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class VisualizerSetupTest {
    @Test
    fun `configure applies safe startup order`() {
        val fakeVisualizer = FakeVisualizerSetupAdapter()
        val listener = WaveformCaptureListener { _, _ -> }

        VisualizerSetup.configure(
            visualizer = fakeVisualizer,
            captureSize = 256,
            scalingMode = 1,
            captureRate = 20_000,
            waveformListener = listener,
        )

        assertEquals(
            listOf(
                "enabled:false",
                "captureSize:256",
                "scalingMode:1",
                "listener:20000:true:false",
                "enabled:true",
            ),
            fakeVisualizer.operations,
        )
        assertSame(listener, fakeVisualizer.listener)
    }

    @Test
    fun `capture size failure does not abort startup`() {
        val fakeVisualizer = FakeVisualizerSetupAdapter(failCaptureSize = true)
        val listener = WaveformCaptureListener { _, _ -> }
        var captureSizeFailure: IllegalStateException? = null

        VisualizerSetup.configure(
            visualizer = fakeVisualizer,
            captureSize = 256,
            scalingMode = 1,
            captureRate = 20_000,
            waveformListener = listener,
            onCaptureSizeFailure = { captureSizeFailure = it },
        )

        assertNotNull(captureSizeFailure)
        assertEquals(
            listOf(
                "enabled:false",
                "captureSize:256",
                "scalingMode:1",
                "listener:20000:true:false",
                "enabled:true",
            ),
            fakeVisualizer.operations,
        )
        assertSame(listener, fakeVisualizer.listener)
    }

    private class FakeVisualizerSetupAdapter(
        private val failCaptureSize: Boolean = false,
    ) : VisualizerSetupAdapter {
        val operations = mutableListOf<String>()
        var listener: WaveformCaptureListener? = null

        override fun setEnabled(enabled: Boolean) {
            operations += "enabled:$enabled"
        }

        override fun setCaptureSize(captureSize: Int) {
            operations += "captureSize:$captureSize"
            if (failCaptureSize) {
                throw IllegalStateException("setCaptureSize() called in wrong state: 2")
            }
        }

        override fun setScalingMode(scalingMode: Int) {
            operations += "scalingMode:$scalingMode"
        }

        override fun setDataCaptureListener(
            listener: WaveformCaptureListener,
            captureRate: Int,
            waveform: Boolean,
            fft: Boolean,
        ) {
            operations += "listener:$captureRate:$waveform:$fft"
            this.listener = listener
        }
    }
}
