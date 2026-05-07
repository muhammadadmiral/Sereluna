package com.android.capstone.sereluna.ui.diary

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper

/**
 * Simple three-dot typing indicator drawable for chat bubbles.
 */
class TypingIndicatorDrawable(
    private val dotColor: Int = Color.GRAY,
    private val radius: Float = 6f,
    private val spacing: Float = 16f
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dotColor
        style = Paint.Style.FILL
    }
    private val handler = Handler(Looper.getMainLooper())
    private var phase = 0
    private val runnable = object : Runnable {
        override fun run() {
            phase = (phase + 1) % 3
            invalidateSelf()
            handler.postDelayed(this, 350)
        }
    }

    override fun draw(canvas: Canvas) {
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        for (i in 0..2) {
            val alpha = if (i == phase) 255 else 120
            paint.alpha = alpha
            val dx = (i - 1) * spacing
            canvas.drawCircle(cx + dx, cy, radius, paint)
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        handler.removeCallbacks(runnable)
        handler.post(runnable)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { paint.colorFilter = colorFilter }
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = (spacing * 4).toInt()
    override fun getIntrinsicHeight(): Int = (radius * 4).toInt()

    fun stop() {
        handler.removeCallbacks(runnable)
    }
}
