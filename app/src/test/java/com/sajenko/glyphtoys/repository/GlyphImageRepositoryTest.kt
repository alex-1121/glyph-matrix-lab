package com.sajenko.glyphtoys.repository

import android.content.SharedPreferences
import com.sajenko.glyphtoys.models.ActiveGlyphSelection
import com.sajenko.glyphtoys.models.CustomGlyphImage
import com.sajenko.glyphtoys.models.DisplayPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlyphImageRepositoryTest {
    private val prefs = FakeSharedPreferences()
    private val repository = GlyphImageRepository(prefs)

    @Test
    fun `composite selection persists without image id`() {
        repository.setActiveSelection(
            ActiveGlyphSelection(
                imageId = null,
                mode = DisplayPriority.COMPOSITE,
                updatedAt = 123L,
            ),
        )

        assertEquals(
            ActiveGlyphSelection(
                imageId = null,
                mode = DisplayPriority.COMPOSITE,
                updatedAt = 123L,
            ),
            repository.getActiveSelection(),
        )
    }

    @Test
    fun `image backed selection persists when image exists`() {
        repository.saveImage(testImage("image-1"))

        repository.setActiveSelection(
            ActiveGlyphSelection(
                imageId = "image-1",
                mode = DisplayPriority.IDLE_ONLY,
                updatedAt = 456L,
            ),
        )

        assertEquals(
            ActiveGlyphSelection(
                imageId = "image-1",
                mode = DisplayPriority.IDLE_ONLY,
                updatedAt = 456L,
            ),
            repository.getActiveSelection(),
        )
    }

    @Test
    fun `image backed selection is cleared when image is missing`() {
        repository.setActiveSelection(
            ActiveGlyphSelection(
                imageId = "missing",
                mode = DisplayPriority.ALWAYS_ON,
                updatedAt = 789L,
            ),
        )

        assertNull(repository.getActiveSelection())
    }

    private fun testImage(id: String): CustomGlyphImage {
        return CustomGlyphImage(
            id = id,
            name = "Test",
            pixels = "0".repeat(169),
            createdAt = 1L,
            updatedAt = 1L,
        )
    }
}

private class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? {
        return values[key] as? String ?: defValue
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        @Suppress("UNCHECKED_CAST")
        return (values[key] as? Set<String>)?.toMutableSet() ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return values[key] as? Int ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return values[key] as? Long ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return values[key] as? Float ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return values[key] as? Boolean ?: defValue
    }

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        listener?.let(listeners::add)
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        listener?.let(listeners::remove)
    }

    private inner class Editor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var shouldClear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?,
        ): SharedPreferences.Editor {
            key?.let { pending[it] = values?.toSet() }
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            key?.let(removals::add)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            shouldClear = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            val changedKeys = mutableSetOf<String>()
            if (shouldClear) {
                changedKeys.addAll(values.keys)
                values.clear()
            }
            removals.forEach { key ->
                if (values.containsKey(key)) {
                    changedKeys.add(key)
                }
                values.remove(key)
            }
            pending.forEach { (key, value) ->
                if (value == null) {
                    if (values.containsKey(key)) {
                        changedKeys.add(key)
                    }
                    values.remove(key)
                } else {
                    if (values[key] != value) {
                        changedKeys.add(key)
                    }
                    values[key] = value
                }
            }
            changedKeys.forEach { key ->
                listeners.forEach { listener ->
                    listener.onSharedPreferenceChanged(this@FakeSharedPreferences, key)
                }
            }
        }
    }
}
