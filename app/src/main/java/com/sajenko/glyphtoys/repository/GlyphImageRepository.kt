package com.sajenko.glyphtoys.repository

import android.content.SharedPreferences
import com.sajenko.glyphtoys.models.ActiveGlyphSelection
import com.sajenko.glyphtoys.models.CustomGlyphImage
import com.sajenko.glyphtoys.models.DisplayPriority
import com.sajenko.glyphtoys.serialization.GlyphImageSerializer

class GlyphImageRepository(private val prefs: SharedPreferences) {
    fun getAllImages(): List<CustomGlyphImage> {
        return imageIds().mapNotNull(::getImage)
    }

    fun getImage(id: String): CustomGlyphImage? {
        val name = prefs.getString(imageNameKey(id), null) ?: return null
        val pixels = prefs.getString(imagePixelsKey(id), null) ?: return null
        val createdAt = prefs.getLong(imageCreatedKey(id), MissingTimestamp)
        val updatedAt = prefs.getLong(imageUpdatedKey(id), MissingTimestamp)
        if (
            createdAt == MissingTimestamp ||
            updatedAt == MissingTimestamp ||
            GlyphImageSerializer.binaryToPixelGrid(pixels) == null
        ) {
            return null
        }
        return CustomGlyphImage(
            id = id,
            name = name,
            pixels = pixels,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    fun saveImage(image: CustomGlyphImage) {
        require(GlyphImageSerializer.binaryToPixelGrid(image.pixels) != null) {
            "Custom glyph image pixel data must be a 169 character binary string"
        }

        val nextIds = imageIds().toMutableList()
        if (!nextIds.contains(image.id)) {
            nextIds.add(image.id)
        }

        prefs.edit()
            .putString(KeyImageList, nextIds.joinToString(","))
            .putString(imageNameKey(image.id), image.name)
            .putString(imagePixelsKey(image.id), image.pixels)
            .putLong(imageCreatedKey(image.id), image.createdAt)
            .putLong(imageUpdatedKey(image.id), image.updatedAt)
            .commit()
    }

    fun deleteImage(id: String) {
        val nextIds = imageIds().filterNot { it == id }
        val editor = prefs.edit()
            .putString(KeyImageList, nextIds.joinToString(","))
            .remove(imageNameKey(id))
            .remove(imagePixelsKey(id))
            .remove(imageCreatedKey(id))
            .remove(imageUpdatedKey(id))

        if (prefs.getString(KeyActiveSelectionId, null) == id) {
            editor
                .remove(KeyActiveSelectionId)
                .remove(KeyActiveSelectionMode)
                .remove(KeyActiveSelectionUpdated)
        }

        editor.commit()
    }

    fun setActiveSelection(selection: ActiveGlyphSelection) {
        if (getImage(selection.imageId) == null) {
            clearActiveSelection()
            return
        }

        prefs.edit()
            .putString(KeyActiveSelectionId, selection.imageId)
            .putString(KeyActiveSelectionMode, selection.mode.name)
            .putLong(KeyActiveSelectionUpdated, selection.updatedAt)
            .commit()
    }

    fun getActiveSelection(): ActiveGlyphSelection? {
        val imageId = prefs.getString(KeyActiveSelectionId, null) ?: return null
        val modeName = prefs.getString(KeyActiveSelectionMode, null) ?: return null
        val updatedAt = prefs.getLong(KeyActiveSelectionUpdated, MissingTimestamp)
        val mode = runCatching { DisplayPriority.valueOf(modeName) }.getOrNull() ?: return null
        if (updatedAt == MissingTimestamp || getImage(imageId) == null) {
            return null
        }
        return ActiveGlyphSelection(
            imageId = imageId,
            mode = mode,
            updatedAt = updatedAt,
        )
    }

    fun clearActiveSelection() {
        prefs.edit()
            .remove(KeyActiveSelectionId)
            .remove(KeyActiveSelectionMode)
            .remove(KeyActiveSelectionUpdated)
            .commit()
    }

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun imageIds(): List<String> {
        val rawIds = prefs.getString(KeyImageList, null).orEmpty()
        if (rawIds.isBlank()) {
            return emptyList()
        }
        return rawIds.split(",").filter { it.isNotBlank() }
    }

    companion object {
        const val PreferencesName = "glyph_images"
        const val KeyImageList = "image_list"
        const val KeyActiveSelectionId = "active_selection_id"
        const val KeyActiveSelectionMode = "active_selection_mode"
        const val KeyActiveSelectionUpdated = "active_selection_updated"
        private const val MissingTimestamp = -1L

        fun imageNameKey(id: String): String = "image_${id}_name"
        fun imagePixelsKey(id: String): String = "image_${id}_pixels"
        fun imageCreatedKey(id: String): String = "image_${id}_created"
        fun imageUpdatedKey(id: String): String = "image_${id}_updated"
    }
}
