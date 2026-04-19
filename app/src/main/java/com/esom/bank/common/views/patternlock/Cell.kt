package com.esom.bank.common.views.patternlock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View

internal class Cell(
    context: Context,
    var index: Int,
    private val regularCellBackground: Drawable?,
    private val regularDotColor: Int,
    private val regularDotRadiusRatio: Float,
    private val selectedCellBackground: Drawable?,
    private val selectedDotColor: Int,
    private val selectedDotRadiusRatio: Float,
    private val errorCellBackground: Drawable?,
    private val errorDotColor: Int,
    private val errorDotRadiusRatio: Float,
    private val lineStyle: Int,
    private val regularLineColor: Int,
    private val errorLineColor: Int,
    private val columnCount: Int,
    private val indicatorSizeRatio: Float
) : View(context) {

    private var currentState: State = State.REGULAR
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentDegree: Float = -1f
    private val indicatorPath: Path = Path()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val cellWidth = MeasureSpec.getSize(widthMeasureSpec) / columnCount
        setMeasuredDimension(cellWidth, cellWidth)
    }

    override fun onDraw(canvas: Canvas) {
        when (currentState) {
            State.REGULAR -> drawDot(canvas, regularCellBackground, regularDotColor, regularDotRadiusRatio)
            State.SELECTED -> drawDot(canvas, selectedCellBackground, selectedDotColor, selectedDotRadiusRatio)
            State.ERROR -> drawDot(canvas, errorCellBackground, errorDotColor, errorDotRadiusRatio)
        }
    }

    private fun drawDot(
        canvas: Canvas,
        background: Drawable?,
        dotColor: Int,
        radiusRatio: Float
    ) {
        val radius = getRadius()
        val centerX = width / 2
        val centerY = height / 2

        if (background is ColorDrawable) {
            paint.color = background.color
            paint.style = Paint.Style.FILL
            canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), radius.toFloat(), paint)
        } else {
            background?.setBounds(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom)
            background?.draw(canvas)
        }

        paint.color = dotColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), radius * radiusRatio, paint)

        if (lineStyle == PatternLockView.LINE_STYLE_INDICATOR &&
            (currentState == State.SELECTED || currentState == State.ERROR)
        ) {
            drawIndicator(canvas)
        }
    }

    private fun drawIndicator(canvas: Canvas) {
        if (currentDegree == -1f) return

        if (indicatorPath.isEmpty) {
            val radius = getRadius()
            val height = radius * indicatorSizeRatio
            indicatorPath.fillType = Path.FillType.WINDING
            indicatorPath.moveTo((width / 2).toFloat(), radius * (1 - selectedDotRadiusRatio - indicatorSizeRatio) / 2 + paddingTop)
            indicatorPath.lineTo(
                (width / 2).toFloat() - height,
                radius * (1 - selectedDotRadiusRatio - indicatorSizeRatio) / 2 + height + paddingTop
            )
            indicatorPath.lineTo(
                (width / 2).toFloat() + height,
                radius * (1 - selectedDotRadiusRatio - indicatorSizeRatio) / 2 + height + paddingTop
            )
            indicatorPath.close()
        }

        paint.color = if (currentState == State.SELECTED) regularLineColor else errorLineColor
        paint.style = Paint.Style.FILL

        canvas.save()
        canvas.rotate(currentDegree, (width / 2).toFloat(), (height / 2).toFloat())
        canvas.drawPath(indicatorPath, paint)
        canvas.restore()
    }

    fun getRadius(): Int {
        return (minOf(width, height) - (paddingLeft + paddingRight)) / 2
    }

    fun getCenter(): Point {
        return Point(
            left + (right - left) / 2,
            top + (bottom - top) / 2
        )
    }

    fun setState(state: State) {
        currentState = state
        invalidate()
    }

    fun setDegree(degree: Float) {
        currentDegree = degree
    }

    fun reset() {
        setState(State.REGULAR)
        currentDegree = -1f
    }
}
