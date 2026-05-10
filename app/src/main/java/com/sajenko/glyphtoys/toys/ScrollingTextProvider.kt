package com.sajenko.glyphtoys.toys

/**
 * Provides scrolling text state to [CompositeToyController].
 *
 * Returns whether scrolling text mode is active, and produces successive
 * scroll frames on demand via [nextScrollFrame].
 */
interface ScrollingTextProvider {
    fun isScrollingTextActive(): Boolean
    fun currentFrame(): PixelGrid
    fun nextScrollFrame(): PixelGrid
    fun hasFinishedCycle(): Boolean
    fun getScrollDelayMs(): Long
    fun reset()
}

/** Default no-op implementation used when no text is configured. */
object EmptyScrollingTextProvider : ScrollingTextProvider {
    override fun isScrollingTextActive(): Boolean = false
    override fun currentFrame(): PixelGrid = PixelGrid()
    override fun nextScrollFrame(): PixelGrid = PixelGrid()
    override fun hasFinishedCycle(): Boolean = false
    override fun getScrollDelayMs(): Long = 80L
    override fun reset() = Unit
}

/**
 * Live implementation backed by a [TextScroller].
 * Call [update] whenever the user changes the text or toggles the feature.
 */
class RepositoryScrollingTextProvider(
    private val repository: com.sajenko.glyphtoys.repository.GlyphImageRepository,
) : ScrollingTextProvider {

    @Volatile private var enabled: Boolean = false
    @Volatile private var scroller: TextScroller? = null
    @Volatile private var scrollDelayMs: Long = 80L

    /** Reload text and enabled state from the repository. */
    fun refresh() {
        val text = repository.getScrollingText()?.trim().orEmpty()
        enabled = repository.isScrollingTextEnabled() && text.isNotEmpty()
        scroller = if (enabled && text.isNotEmpty()) TextScroller(text) else null
        
        // Map user-facing speed scale 1-10 to scroll delay.
        // Speed 1 = slowest (longest delay), Speed 10 = fastest (shortest delay).
        val speed = repository.getScrollingTextSpeed().coerceIn(1, 10)
        scrollDelayMs = (150L / speed).coerceIn(15L, 150L)
    }

    override fun isScrollingTextActive(): Boolean = enabled && scroller != null

    override fun currentFrame(): PixelGrid = scroller?.currentFrame() ?: PixelGrid()

    override fun nextScrollFrame(): PixelGrid = scroller?.nextFrame() ?: PixelGrid()

    override fun hasFinishedCycle(): Boolean = scroller?.hasFinishedCycle ?: false

    override fun getScrollDelayMs(): Long = scrollDelayMs

    override fun reset() {
        scroller?.reset()
    }
}
