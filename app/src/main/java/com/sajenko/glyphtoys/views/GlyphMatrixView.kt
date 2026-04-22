package com.sajenko.glyphtoys.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.sajenko.glyphtoys.R
import com.sajenko.glyphtoys.models.MaskedPixelGrid
import com.sajenko.glyphtoys.toys.PixelGrid
import kotlin.math.min

class GlyphMatrixView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    var maskedGrid: MaskedPixelGrid? = null
        set(value) {
            field = value
            invalidate()
        }

    var pixelGrid: PixelGrid? = null
        set(value) {
            field = value
            invalidate()
        }

    var interactiveMode: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var onPixelToggled: ((x: Int, y: Int) -> Unit)? = null

    private val offPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pixel_off)
        style = Paint.Style.FILL
    }
    private val onPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pixel_on)
        style = Paint.Style.FILL
    }

    private val cellRect = RectF()

    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.black))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val requested = when {
            width > 0 && height > 0 -> min(width, height)
            width > 0 -> width
            height > 0 -> height
            else -> resources.getDimensionPixelSize(R.dimen.matrix_size_large)
        }
        setMeasuredDimension(requested, requested)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = maskedGrid?.size ?: pixelGrid?.size ?: MaskedPixelGrid.PixelGridSize
        val cellSize = min(width, height).toFloat() / size
        val gridWidth = cellSize * size
        val startX = (width - gridWidth) / 2f
        val startY = (height - gridWidth) / 2f
        val cellInset = cellSize / 12f

        for (y in 0 until size) {
            for (x in 0 until size) {
                val masked = maskedGrid
                if (!shouldDrawCell(masked, size, x, y)) {
                    continue
                }

                val left = startX + (x * cellSize)
                val top = startY + (y * cellSize)
                cellRect.set(
                    left + cellInset,
                    top + cellInset,
                    left + cellSize - cellInset,
                    top + cellSize - cellInset,
                )

                val lit = if (masked != null) {
                    masked.isLit(x, y)
                } else {
                    pixelGrid?.get(x, y) == true
                }

                canvas.drawRect(
                    cellRect,
                    if (lit) onPaint else offPaint,
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interactiveMode || maskedGrid == null) {
            return super.onTouchEvent(event)
        }
        if (event.action != MotionEvent.ACTION_DOWN) {
            return true
        }

        val point = eventToGridPoint(event.x, event.y) ?: return true
        maskedGrid?.toggle(point.first, point.second)
        onPixelToggled?.invoke(point.first, point.second)
        invalidate()
        performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun eventToGridPoint(touchX: Float, touchY: Float): Pair<Int, Int>? {
        val size = maskedGrid?.size ?: return null
        val cellSize = min(width, height).toFloat() / size
        val gridWidth = cellSize * size
        val startX = (width - gridWidth) / 2f
        val startY = (height - gridWidth) / 2f
        val x = ((touchX - startX) / cellSize).toInt()
        val y = ((touchY - startY) / cellSize).toInt()
        return if (x in 0 until size && y in 0 until size) x to y else null
    }

    private fun shouldDrawCell(masked: MaskedPixelGrid?, size: Int, x: Int, y: Int): Boolean {
        if (masked != null) {
            return masked.isEditable(x, y)
        }
        return size != MaskedPixelGrid.PixelGridSize || MaskedPixelGrid.isPhoneDisplayPixel(x, y)
    }
}
