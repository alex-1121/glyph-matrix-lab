package com.sajenko.glyphtoys.toys

import com.sajenko.glyphtoys.models.DisplayPriority

data class AodToySetupGuidance(
    val expectedSource: LiveGlyphSource,
    val actualSource: LiveGlyphSource?,
)

object AodToySelectionAdvisor {
    fun expectedSource(mode: DisplayPriority): LiveGlyphSource {
        return when (mode) {
            DisplayPriority.COMPOSITE -> LiveGlyphSource.COMPOSITE_TOY
            DisplayPriority.IDLE_ONLY -> LiveGlyphSource.COMPOSITE_TOY
            DisplayPriority.ALWAYS_ON -> LiveGlyphSource.STATIC_IMAGE_TOY
        }
    }

    /**
     * Returns setup guidance when the running toy doesn't match what's configured, or null.
     * Scrolling text only renders in the Composite toy, so it expects Composite whatever
     * image is selected.
     */
    fun guidanceFor(
        mode: DisplayPriority?,
        actualSource: LiveGlyphSource?,
        scrollingTextEnabled: Boolean = false,
    ): AodToySetupGuidance? {
        val expectedSource = when {
            scrollingTextEnabled -> LiveGlyphSource.COMPOSITE_TOY
            mode != null -> expectedSource(mode)
            else -> return null
        }
        if (actualSource == expectedSource) {
            return null
        }
        return AodToySetupGuidance(
            expectedSource = expectedSource,
            actualSource = actualSource,
        )
    }
}
