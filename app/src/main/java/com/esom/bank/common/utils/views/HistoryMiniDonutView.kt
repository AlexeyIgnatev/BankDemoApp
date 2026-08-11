package com.esom.bank.common.utils.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.esom.bank.R

class HistoryMiniDonutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = width.coerceAtMost(height) * 0.22f
        paint.strokeWidth = stroke
        val rect = RectF(stroke, stroke, width - stroke, height - stroke)
        val colors = intArrayOf(
            ContextCompat.getColor(context, R.color.red),
            ContextCompat.getColor(context, R.color.green),
            ContextCompat.getColor(context, R.color.subtitle)
        )
        val sweeps = floatArrayOf(150f, 105f, 81f)
        var start = -90f
        sweeps.forEachIndexed { index, sweep ->
            paint.color = colors[index]
            canvas.drawArc(rect, start, sweep, false, paint)
            start += sweep + 8f
        }
    }
}
