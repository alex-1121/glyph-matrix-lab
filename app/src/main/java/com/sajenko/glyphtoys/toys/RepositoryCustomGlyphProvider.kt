package com.sajenko.glyphtoys.toys

import com.sajenko.glyphtoys.models.DisplayPriority
import com.sajenko.glyphtoys.repository.GlyphImageRepository
import com.sajenko.glyphtoys.serialization.GlyphImageSerializer

class RepositoryCustomGlyphProvider(
    private val repository: GlyphImageRepository,
) : CustomGlyphProvider {
    private var cachedGrid: PixelGrid? = null

    fun refresh() {
        cachedGrid = loadActiveIdleGrid()
    }

    override fun idleImageGrid(): PixelGrid? = cachedGrid

    private fun loadActiveIdleGrid(): PixelGrid? {
        val selection = repository.getActiveSelection() ?: return null
        if (selection.mode != DisplayPriority.IDLE_ONLY) {
            return null
        }
        val imageId = selection.imageId ?: return null
        val image = repository.getImage(imageId) ?: return null
        return GlyphImageSerializer.binaryToPixelGrid(image.pixels)
    }
}
