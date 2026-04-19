package com.esom.bank.common.views.patternlock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.GridLayout
import androidx.core.content.ContextCompat
import com.esom.bank.R

class PatternLockView : GridLayout {

    companion object {
        const val DEFAULT_RADIUS_RATIO = 0.3f
        const val DEFAULT_LINE_WIDTH = 2f
        const val DEFAULT_SPACING = 24f
        const val DEFAULT_ROW_COUNT = 3
        const val DEFAULT_COLUMN_COUNT = 3
        const val DEFAULT_ERROR_DURATION = 400
        const val DEFAULT_HIT_AREA_PADDING_RATIO = 0.2f
        const val DEFAULT_INDICATOR_SIZE_RATIO = 0.2f

        const val LINE_STYLE_COMMON = 1
        const val LINE_STYLE_INDICATOR = 2
    }

    private var regularCellBackground = null as android.graphics.drawable.Drawable?
    private var regularDotColor: Int = 0
    private var regularDotRadiusRatio: Float = 0f
    private var selectedCellBackground = null as android.graphics.drawable.Drawable?
    private var selectedDotColor: Int = 0
    private var selectedDotRadiusRatio: Float = 0f
    private var errorCellBackground = null as android.graphics.drawable.Drawable?
    private var errorDotColor: Int = 0
    private var errorDotRadiusRatio: Float = 0f
    private var lineStyle: Int = 0
    private var lineWidth: Int = 0
    private var regularLineColor: Int = 0
    private var errorLineColor: Int = 0
    private var spacing: Int = 0
    private var plvRowCount: Int = 0
    private var plvColumnCount: Int = 0
    private var errorDuration: Int = 0
    private var hitAreaPaddingRatio: Float = 0f
    private var indicatorSizeRatio: Float = 0f

    private val cells = ArrayList<Cell>()
    private val selectedCells = ArrayList<Cell>()
    private val linePaint: Paint = Paint()
    private val linePath: Path = Path()

    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var isSecureMode = false
    private var onPatternListener: OnPatternListener? = null
    private var isFreeze = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.PatternLockView)
        regularCellBackground = ta.getDrawable(R.styleable.PatternLockView_plv_regularCellBackground)
        regularDotColor = ta.getColor(
            R.styleable.PatternLockView_plv_regularDotColor,
            ContextCompat.getColor(context, R.color.lock_pattern_regular)
        )
        regularDotRadiusRatio = ta.getFloat(
            R.styleable.PatternLockView_plv_regularDotRadiusRatio,
            DEFAULT_RADIUS_RATIO
        )

        selectedCellBackground = ta.getDrawable(R.styleable.PatternLockView_plv_selectedCellBackground)
        selectedDotColor = ta.getColor(
            R.styleable.PatternLockView_plv_selectedDotColor,
            ContextCompat.getColor(context, R.color.lock_pattern_selected)
        )
        selectedDotRadiusRatio = ta.getFloat(
            R.styleable.PatternLockView_plv_selectedDotRadiusRatio,
            DEFAULT_RADIUS_RATIO
        )

        errorCellBackground = ta.getDrawable(R.styleable.PatternLockView_plv_errorCellBackground)
        errorDotColor = ta.getColor(
            R.styleable.PatternLockView_plv_errorDotColor,
            ContextCompat.getColor(context, R.color.lock_pattern_error)
        )
        errorDotRadiusRatio = ta.getFloat(
            R.styleable.PatternLockView_plv_errorDotRadiusRatio,
            DEFAULT_RADIUS_RATIO
        )

        lineStyle = ta.getInt(R.styleable.PatternLockView_plv_lineStyle, LINE_STYLE_COMMON)
        lineWidth = ta.getDimensionPixelSize(
            R.styleable.PatternLockView_plv_lineWidth,
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_LINE_WIDTH,
                context.resources.displayMetrics
            ).toInt()
        )
        regularLineColor = ta.getColor(
            R.styleable.PatternLockView_plv_regularLineColor,
            ContextCompat.getColor(context, R.color.lock_pattern_selected)
        )
        errorLineColor = ta.getColor(
            R.styleable.PatternLockView_plv_errorLineColor,
            ContextCompat.getColor(context, R.color.lock_pattern_error)
        )

        spacing = ta.getDimensionPixelSize(
            R.styleable.PatternLockView_plv_spacing,
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_SPACING,
                context.resources.displayMetrics
            ).toInt()
        )
        plvRowCount = ta.getInteger(R.styleable.PatternLockView_plv_rowCount, DEFAULT_ROW_COUNT)
        plvColumnCount = ta.getInteger(R.styleable.PatternLockView_plv_columnCount, DEFAULT_COLUMN_COUNT)
        errorDuration = ta.getInteger(R.styleable.PatternLockView_plv_errorDuration, DEFAULT_ERROR_DURATION)
        hitAreaPaddingRatio = ta.getFloat(
            R.styleable.PatternLockView_plv_hitAreaPaddingRatio,
            DEFAULT_HIT_AREA_PADDING_RATIO
        )
        indicatorSizeRatio = ta.getFloat(
            R.styleable.PatternLockView_plv_indicatorSizeRatio,
            DEFAULT_INDICATOR_SIZE_RATIO
        )
        ta.recycle()

        rowCount = plvRowCount
        columnCount = plvColumnCount

        setupCells()
        initPathPaint()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isFreeze) return false

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                val hitCell = getHitCell(event.x.toInt(), event.y.toInt()) ?: return false
                onPatternListener?.onStarted()
                notifyCellSelected(hitCell)
            }

            MotionEvent.ACTION_MOVE -> handleActionMove(event)
            MotionEvent.ACTION_UP -> onFinish()
            MotionEvent.ACTION_CANCEL -> reset()
            else -> return false
        }
        return true
    }

    private fun handleActionMove(event: MotionEvent) {
        val hitCell = getHitCell(event.x.toInt(), event.y.toInt())
        if (hitCell != null && !selectedCells.contains(hitCell)) {
            notifyCellSelected(hitCell)
        }

        lastX = event.x
        lastY = event.y
        invalidate()
    }

    private fun notifyCellSelected(cell: Cell) {
        if (selectedCells.size >= 1) {
            val currentIndex = cell.index
            val prevIndex = selectedCells.last().index
            if ((prevIndex == 0 && currentIndex == 2 || prevIndex == 2 && currentIndex == 0) && selectedCells.none { it.index == 1 }) {
                notifyCellSelected(cells.first { it.index == 1 })
            }
            if ((prevIndex == 0 && currentIndex == 6 || prevIndex == 6 && currentIndex == 0) && selectedCells.none { it.index == 3 }) {
                notifyCellSelected(cells.first { it.index == 3 })
            }
            if ((prevIndex == 0 && currentIndex == 8 || prevIndex == 8 && currentIndex == 0) && selectedCells.none { it.index == 4 }) {
                notifyCellSelected(cells.first { it.index == 4 })
            }
            if ((prevIndex == 1 && currentIndex == 7 || prevIndex == 7 && currentIndex == 1) && selectedCells.none { it.index == 4 }) {
                notifyCellSelected(cells.first { it.index == 4 })
            }
            if ((prevIndex == 2 && currentIndex == 6 || prevIndex == 6 && currentIndex == 2) && selectedCells.none { it.index == 4 }) {
                notifyCellSelected(cells.first { it.index == 4 })
            }
            if ((prevIndex == 2 && currentIndex == 8 || prevIndex == 8 && currentIndex == 2) && selectedCells.none { it.index == 5 }) {
                notifyCellSelected(cells.first { it.index == 5 })
            }
            if ((prevIndex == 5 && currentIndex == 3 || prevIndex == 3 && currentIndex == 5) && selectedCells.none { it.index == 4 }) {
                notifyCellSelected(cells.first { it.index == 4 })
            }
            if ((prevIndex == 6 && currentIndex == 8 || prevIndex == 8 && currentIndex == 6) && selectedCells.none { it.index == 7 }) {
                notifyCellSelected(cells.first { it.index == 7 })
            }
        }

        selectedCells.add(cell)
        onPatternListener?.onProgress(generateSelectedIds())

        if (isSecureMode) return

        cell.setState(State.SELECTED)
        val center = cell.getCenter()

        if (selectedCells.size == 1) {
            if (lineStyle == LINE_STYLE_COMMON) {
                linePath.moveTo(center.x.toFloat(), center.y.toFloat())
            }
        } else {
            if (lineStyle == LINE_STYLE_COMMON) {
                linePath.lineTo(center.x.toFloat(), center.y.toFloat())
            } else if (lineStyle == LINE_STYLE_INDICATOR) {
                val previousCell = selectedCells[selectedCells.size - 2]
                val previousCenter = previousCell.getCenter()
                val diffX = center.x - previousCenter.x
                val diffY = center.y - previousCenter.y
                val radius = cell.getRadius()
                val length = kotlin.math.sqrt((diffX * diffX + diffY * diffY).toDouble())

                linePath.moveTo(
                    (previousCenter.x + radius * diffX / length).toFloat(),
                    (previousCenter.y + radius * diffY / length).toFloat()
                )
                linePath.lineTo(
                    (center.x - radius * diffX / length).toFloat(),
                    (center.y - radius * diffY / length).toFloat()
                )

                val degree = Math.toDegrees(Math.atan2(diffY.toDouble(), diffX.toDouble())) + 90
                previousCell.setDegree(degree.toFloat())
                previousCell.invalidate()
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (isSecureMode) return

        canvas.drawPath(linePath, linePaint)
        if (selectedCells.isEmpty() || lastX <= 0 || lastY <= 0) return

        if (lineStyle == LINE_STYLE_COMMON) {
            val center = selectedCells.last().getCenter()
            canvas.drawLine(center.x.toFloat(), center.y.toFloat(), lastX, lastY, linePaint)
        } else if (lineStyle == LINE_STYLE_INDICATOR) {
            val lastCell = selectedCells.last()
            val lastCenter = lastCell.getCenter()
            val radius = lastCell.getRadius()
            if (!(lastX >= lastCenter.x - radius &&
                        lastX <= lastCenter.x + radius &&
                        lastY >= lastCenter.y - radius &&
                        lastY <= lastCenter.y + radius)
            ) {
                val diffX = lastX - lastCenter.x
                val diffY = lastY - lastCenter.y
                val length = kotlin.math.sqrt((diffX * diffX + diffY * diffY).toDouble())
                canvas.drawLine(
                    (lastCenter.x + radius * diffX / length).toFloat(),
                    (lastCenter.y + radius * diffY / length).toFloat(),
                    lastX,
                    lastY,
                    linePaint
                )
            }
        }
    }

    private fun setupCells() {
        for (i in 0 until plvRowCount) {
            for (j in 0 until plvColumnCount) {
                val cell = Cell(
                    context,
                    i * plvColumnCount + j,
                    regularCellBackground,
                    regularDotColor,
                    regularDotRadiusRatio,
                    selectedCellBackground,
                    selectedDotColor,
                    selectedDotRadiusRatio,
                    errorCellBackground,
                    errorDotColor,
                    errorDotRadiusRatio,
                    lineStyle,
                    regularLineColor,
                    errorLineColor,
                    plvColumnCount,
                    indicatorSizeRatio
                )
                val padding = spacing / 2
                cell.setPadding(padding, padding, padding, padding)
                addView(cell)
                cells.add(cell)
            }
        }
    }

    private fun initPathPaint() {
        linePaint.isAntiAlias = true
        linePaint.isDither = true
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeJoin = Paint.Join.ROUND
        linePaint.strokeCap = Paint.Cap.ROUND
        linePaint.strokeWidth = lineWidth.toFloat()
        linePaint.color = regularLineColor
    }

    private fun reset() {
        selectedCells.forEach { it.reset() }
        selectedCells.clear()
        linePaint.color = regularLineColor
        linePath.reset()
        lastX = 0f
        lastY = 0f
        invalidate()
    }

    fun enableSecureMode() {
        isSecureMode = true
    }

    fun disableSecureMode() {
        isSecureMode = false
    }

    private fun getHitCell(x: Int, y: Int): Cell? {
        return cells.firstOrNull { isSelected(it, x, y) }
    }

    private fun isSelected(view: Cell, x: Int, y: Int): Boolean {
        val innerPadding = view.width * hitAreaPaddingRatio
        return x >= view.left + innerPadding &&
                x <= view.right - innerPadding &&
                y >= view.top + innerPadding &&
                y <= view.bottom - innerPadding
    }

    private fun onFinish() {
        lastX = 0f
        lastY = 0f

        val isCorrect = onPatternListener?.onComplete(generateSelectedIds())
        if (isCorrect == true) {
            if (isFreeze) {
                selectedCells.forEach { it.setState(State.SELECTED) }
                linePaint.color = regularLineColor
                invalidate()
            } else {
                reset()
            }
        } else {
            onError()
        }
    }

    private fun generateSelectedIds(): ArrayList<Int> {
        return ArrayList<Int>().apply {
            selectedCells.forEach { add(it.index) }
        }
    }

    private fun onError() {
        if (isSecureMode) {
            reset()
            return
        }
        selectedCells.forEach { it.setState(State.ERROR) }
        linePaint.color = errorLineColor
        invalidate()

        postDelayed({ reset() }, errorDuration.toLong())
    }

    fun freeze() {
        isFreeze = true
    }

    fun unFreeze() {
        isFreeze = false
        reset()
        invalidate()
    }

    fun setOnPatternListener(listener: OnPatternListener) {
        onPatternListener = listener
    }

    interface OnPatternListener {
        fun onStarted() {}
        fun onProgress(ids: ArrayList<Int>) {}
        fun onComplete(ids: ArrayList<Int>): Boolean
    }
}
