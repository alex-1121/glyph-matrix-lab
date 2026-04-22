package com.sajenko.glyphtoys

import android.content.Context
import com.sajenko.glyphtoys.models.CustomGlyphImage
import com.sajenko.glyphtoys.repository.GlyphImageRepository
import com.sajenko.glyphtoys.serialization.GlyphImageSerializer
import com.sajenko.glyphtoys.toys.FrameBuilders
import com.sajenko.glyphtoys.toys.PixelGrid

object ShowcaseGlyphImages {
    private const val SeedVersion = 1
    private const val SeedVersionKey = "showcase_images_seed_version"
    private const val StableTimestamp = 0L

    fun seedIfNeeded(context: Context, repository: GlyphImageRepository) {
        repository.seedImagesOnce(
            seedVersionKey = SeedVersionKey,
            version = SeedVersion,
            images = buildImages(context),
        )
    }

    private fun buildImages(context: Context): List<CustomGlyphImage> {
        return listOf(
            image(
                id = "showcase_app_logo",
                name = context.getString(R.string.showcase_matrix_lab_logo_name),
                grid = FrameBuilders.buildLabIconGrid(),
            ),
            image(
                id = "showcase_cross_example",
                name = context.getString(R.string.showcase_cross_example_name),
                grid = FrameBuilders.buildAppLogoGrid(),
            ),
            image(
                id = "showcase_dollar_example",
                name = context.getString(R.string.showcase_dollar_icon_name),
                grid = FrameBuilders.buildDollarIconGrid(),
            ),
        )
    }

    private fun image(id: String, name: String, grid: PixelGrid): CustomGlyphImage {
        return CustomGlyphImage(
            id = id,
            name = name,
            pixels = GlyphImageSerializer.pixelGridToBinary(grid),
            createdAt = StableTimestamp,
            updatedAt = StableTimestamp,
        )
    }
}
