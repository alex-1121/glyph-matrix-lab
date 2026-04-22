package com.sajenko.glyphtoys.toys

import com.sajenko.glyphtoys.models.DisplayPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AodToySelectionAdvisorTest {
    @Test
    fun `idle only expects composite toy source`() {
        assertEquals(
            LiveGlyphSource.COMPOSITE_TOY,
            AodToySelectionAdvisor.expectedSource(DisplayPriority.IDLE_ONLY),
        )
    }

    @Test
    fun `always on expects static image toy source`() {
        assertEquals(
            LiveGlyphSource.STATIC_IMAGE_TOY,
            AodToySelectionAdvisor.expectedSource(DisplayPriority.ALWAYS_ON),
        )
    }

    @Test
    fun `matching live source has no guidance`() {
        assertNull(
            AodToySelectionAdvisor.guidanceFor(
                DisplayPriority.IDLE_ONLY,
                LiveGlyphSource.COMPOSITE_TOY,
            ),
        )
    }

    @Test
    fun `wrong live source reports expected setup`() {
        val guidance = AodToySelectionAdvisor.guidanceFor(
            DisplayPriority.IDLE_ONLY,
            LiveGlyphSource.STATIC_IMAGE_TOY,
        )

        assertNotNull(guidance)
        assertEquals(LiveGlyphSource.COMPOSITE_TOY, guidance!!.expectedSource)
        assertEquals(LiveGlyphSource.STATIC_IMAGE_TOY, guidance.actualSource)
    }

    @Test
    fun `missing live source reports expected setup`() {
        val guidance = AodToySelectionAdvisor.guidanceFor(DisplayPriority.IDLE_ONLY, null)

        assertNotNull(guidance)
        assertEquals(LiveGlyphSource.COMPOSITE_TOY, guidance!!.expectedSource)
        assertNull(guidance.actualSource)
    }
}
