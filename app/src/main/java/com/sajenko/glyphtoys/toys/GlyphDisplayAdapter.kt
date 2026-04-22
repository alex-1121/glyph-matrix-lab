package com.sajenko.glyphtoys.toys

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject

class GlyphDisplayAdapter(
    private val context: Context,
    private val glyphMatrixManager: GlyphMatrixManager,
) : FrameSink {
    override fun display(grid: PixelGrid) {
        val bitmap = Bitmap.createBitmap(grid.size, grid.size, Bitmap.Config.ARGB_8888)
        for (y in 0 until grid.size) {
            for (x in 0 until grid.size) {
                if (grid.get(x, y)) {
                    bitmap.setPixel(x, y, Color.WHITE)
                }
            }
        }

        val frame = GlyphMatrixFrame.Builder()
            .addTop(
                GlyphMatrixObject.Builder()
                    .setImageSource(bitmap)
                    .setBrightness(255)
                    .build(),
            )
            .build(context)
        glyphMatrixManager.setMatrixFrame(frame)
    }
}
