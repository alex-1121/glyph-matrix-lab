package com.sajenko.glyphtoys.toys

interface CustomGlyphProvider {
    fun idleImageGrid(): PixelGrid?
}

object EmptyCustomGlyphProvider : CustomGlyphProvider {
    override fun idleImageGrid(): PixelGrid? = null
}
