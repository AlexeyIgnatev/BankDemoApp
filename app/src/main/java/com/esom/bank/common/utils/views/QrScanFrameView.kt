package com.esom.bank.common.utils.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class QrScanFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        val inset = 8f * density
        val arm = 54f * density
        val radius = 26f * density
        val right = width - inset
        val bottom = height - inset

        drawCorner(canvas, inset, inset, arm, radius, false, false)
        drawCorner(canvas, right, inset, arm, radius, true, false)
        drawCorner(canvas, inset, bottom, arm, radius, false, true)
        drawCorner(canvas, right, bottom, arm, radius, true, true)
    }

    private fun drawCorner(
        canvas: Canvas,
        x: Float,
        y: Float,
        arm: Float,
        radius: Float,
        right: Boolean,
        bottom: Boolean
    ) {
        val sx = if (right) -1f else 1f
        val sy = if (bottom) -1f else 1f
        val path = Path().apply {
            moveTo(x + sx * arm, y)
            lineTo(x + sx * radius, y)
            quadTo(x, y, x, y + sy * radius)
            lineTo(x, y + sy * arm)
        }
        canvas.drawPath(path, paint)
    }
}
