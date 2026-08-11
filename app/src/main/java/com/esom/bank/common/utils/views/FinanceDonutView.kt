package com.esom.bank.common.utils.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.esom.bank.R

class FinanceDonutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 42f * resources.displayMetrics.scaledDensity
        color = ContextCompat.getColor(context, R.color.subtitle)
    }
    private var values = listOf(1.0)
    private var centerLabel = "Месяц"

    fun setData(newValues: List<Double>, label: String) {
        values = newValues.filter { it > 0.0 }.ifEmpty { listOf(1.0) }
        centerLabel = label
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = width.coerceAtMost(height) * 0.115f
        paint.strokeWidth = stroke
        val inset = stroke / 2f + 8f
        val rect = RectF(inset, inset, width - inset, height - inset)
        val colors = intArrayOf(
            ContextCompat.getColor(context, R.color.red),
            ContextCompat.getColor(context, R.color.finance_orange),
            ContextCompat.getColor(context, R.color.finance_cyan),
            ContextCompat.getColor(context, R.color.finance_purple)
        )
        val total = values.sum().coerceAtLeast(1.0)
        var start = -90f
        values.forEachIndexed { index, value ->
            val sweep = (value / total * 360f).toFloat()
            paint.color = colors[index % colors.size]
            canvas.drawArc(rect, start, sweep, false, paint)
            start += sweep
        }
        textPaint.textSize = 18f * resources.displayMetrics.scaledDensity
        val y = height / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(centerLabel, width / 2f, y, textPaint)
    }
}
