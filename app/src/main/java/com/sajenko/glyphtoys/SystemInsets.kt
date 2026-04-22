package com.sajenko.glyphtoys

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.applySystemBarsPadding() {
    val originalLeft = paddingLeft
    val originalTop = paddingTop
    val originalRight = paddingRight
    val originalBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            originalLeft + bars.left,
            originalTop + bars.top,
            originalRight + bars.right,
            originalBottom + bars.bottom,
        )
        insets
    }

    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    } else {
        addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    view.removeOnAttachStateChangeListener(this)
                    ViewCompat.requestApplyInsets(view)
                }

                override fun onViewDetachedFromWindow(view: View) = Unit
            },
        )
    }
}
