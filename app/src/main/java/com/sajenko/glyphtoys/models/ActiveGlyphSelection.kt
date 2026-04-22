package com.sajenko.glyphtoys.models

data class ActiveGlyphSelection(
    val imageId: String,
    val mode: DisplayPriority,
    val updatedAt: Long,
)

enum class DisplayPriority {
    IDLE_ONLY,
    ALWAYS_ON,
}
