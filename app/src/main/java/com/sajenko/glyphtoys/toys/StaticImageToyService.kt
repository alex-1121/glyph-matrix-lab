package com.sajenko.glyphtoys.toys

import android.content.Context
import android.content.SharedPreferences
import com.nothing.ketchum.GlyphMatrixManager
import com.sajenko.glyphtoys.models.DisplayPriority
import com.sajenko.glyphtoys.repository.GlyphImageRepository
import com.sajenko.glyphtoys.serialization.GlyphImageSerializer

class StaticImageToyService : GlyphToyBase("StaticImageToy") {
    private lateinit var repository: GlyphImageRepository
    private var frameSink: GlyphDisplayAdapter? = null

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (shouldRefreshForKey(key)) {
            renderCurrentImage()
        }
    }

    override fun onServiceConnected(context: Context, gmm: GlyphMatrixManager) {
        repository = GlyphImageRepository(
            context.getSharedPreferences(GlyphImageRepository.PreferencesName, Context.MODE_PRIVATE),
        )
        frameSink = GlyphDisplayAdapter(context, gmm)
        repository.registerChangeListener(prefListener)
        renderCurrentImage()
    }

    override fun onAod() {
        renderCurrentImage()
    }

    override fun onServiceDisconnected(context: Context) {
        if (::repository.isInitialized) {
            repository.unregisterChangeListener(prefListener)
        }
        frameSink = null
        LiveGlyphPreview.clear(LiveGlyphSource.STATIC_IMAGE_TOY)
    }

    private fun shouldRefreshForKey(key: String?): Boolean {
        if (key == GlyphImageRepository.KeyActiveSelectionId ||
            key == GlyphImageRepository.KeyActiveSelectionMode ||
            key == GlyphImageRepository.KeyImageList
        ) {
            return true
        }
        val activeId = if (::repository.isInitialized) {
            repository.getActiveSelection()?.imageId
        } else {
            null
        }
        return activeId != null && key?.startsWith("image_${activeId}_") == true
    }

    private fun renderCurrentImage() {
        val sink = frameSink ?: return
        val selection = repository.getActiveSelection()
        if (selection == null || selection.mode != DisplayPriority.ALWAYS_ON) {
            displayStaticFrame(sink, PixelGrid())
            return
        }

        val imageId = selection.imageId
        val image = imageId?.let(repository::getImage)
        val grid = image?.pixels?.let(GlyphImageSerializer::binaryToPixelGrid)
        displayStaticFrame(sink, grid ?: PixelGrid())
    }

    private fun displayStaticFrame(sink: FrameSink, grid: PixelGrid) {
        sink.display(grid)
        LiveGlyphPreview.publish(
            grid = grid,
            source = LiveGlyphSource.STATIC_IMAGE_TOY,
            mode = LiveGlyphMode.STATIC_IMAGE,
        )
    }
}
