package org.mpdx.android.features.tasks.editor.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat

class CutoutDrawable(private val updateState: (CutoutDrawable.() -> Unit)? = null) : Drawable() {
    private val cutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.WHITE
    }
    private val cutoutBounds = RectF()

    override fun draw(canvas: Canvas) {
        updateState?.invoke(this)
        // Draw mask for the cutout.
        canvas.drawRect(cutoutBounds, cutoutPaint)
    }

    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit
    override fun getOpacity(): Int = PixelFormat.OPAQUE

    fun setCutout(start: Float, top: Float, end: Float, bottom: Float) {
        val (left, right) = when (DrawableCompat.getLayoutDirection(this)) {
            ViewCompat.LAYOUT_DIRECTION_RTL -> {
                val width = bounds.width()
                Pair(width - end, width - start)
            }
            else -> Pair(start, end)
        }

        // Avoid expensive redraws by only calling invalidateSelf if one of the cutout's dimensions has changed.
        if (
            left != cutoutBounds.left ||
            top != cutoutBounds.top ||
            right != cutoutBounds.right ||
            bottom != cutoutBounds.bottom
        ) {
            cutoutBounds.set(left, top, right, bottom)
            invalidateSelf()
        }
    }

    override fun onBoundsChange(bounds: Rect?) {
        super.onBoundsChange(bounds)
        updateState?.invoke(this)
    }
}
