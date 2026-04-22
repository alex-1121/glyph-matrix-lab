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

    fun guidanceFor(mode: DisplayPriority, actualSource: LiveGlyphSource?): AodToySetupGuidance? {
        val expectedSource = expectedSource(mode)
        if (actualSource == expectedSource) {
            return null
        }
        return AodToySetupGuidance(
            expectedSource = expectedSource,
            actualSource = actualSource,
        )
    }
}
