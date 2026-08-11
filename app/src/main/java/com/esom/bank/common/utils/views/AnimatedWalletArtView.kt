package com.esom.bank.common.utils.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.esom.bank.R
import com.esom.bank.screens.main.enums.CurrencyEnum
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AnimatedWalletArtView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val cardBounds = RectF()
    private val cardClip = Path()
    private val revealMask = Path()
    private val shapePath = Path()
    private val matrix = Matrix()
    private val meshVertices = FloatArray((MESH_WIDTH + 1) * (MESH_HEIGHT + 1) * 2)
    private val radius = 22f * resources.displayMetrics.density
    private var currency = CurrencyEnum.SOM
    private var texture: Bitmap? = null
    private var animationStartedAt = 0L
    private var elapsedMs = 0L

    fun setCurrency(value: CurrencyEnum) {
        currency = value
        texture = null
        restartAnimation(resetTime = true)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        restartAnimation(resetTime = animationStartedAt == 0L)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE && isAttachedToWindow) restartAnimation(resetTime = false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        elapsedMs = SystemClock.uptimeMillis() - animationStartedAt
        val bitmap = texture ?: loadTexture().also { texture = it }
        cardBounds.set(0f, 0f, width.toFloat(), height.toFloat())
        cardClip.reset()
        cardClip.addRoundRect(cardBounds, radius, radius, Path.Direction.CW)
        val phase = ((elapsedMs % LOOP_DURATION_MS) / LOOP_DURATION_MS.toFloat()) * FULL_CIRCLE

        canvas.save()
        canvas.clipPath(cardClip)
        drawDeepBase(canvas, phase)
        canvas.save()
        canvas.clipPath(buildRevealMask())
        updateMesh(phase)
        canvas.drawBitmapMesh(bitmap, MESH_WIDTH, MESH_HEIGHT, meshVertices, 0, null, 0, paint)
        drawLivingLight(canvas, phase)
        canvas.restore()
        drawGlassFinish(canvas, phase)
        canvas.restore()
        if (isAttachedToWindow && visibility == VISIBLE) postInvalidateOnAnimation()
    }

    private fun updateMesh(phase: Float) {
        var offset = 0
        for (row in 0..MESH_HEIGHT) {
            val ny = row / MESH_HEIGHT.toFloat()
            for (column in 0..MESH_WIDTH) {
                val nx = column / MESH_WIDTH.toFloat()
                val edgeLock = sin(PI.toFloat() * nx) * sin(PI.toFloat() * ny)
                val baseX = nx * width
                val baseY = ny * height
                val (dx, dy) = displacement(nx, ny, phase)
                meshVertices[offset++] = baseX + dx * edgeLock
                meshVertices[offset++] = baseY + dy * edgeLock
            }
        }
    }

    private fun displacement(nx: Float, ny: Float, phase: Float): Pair<Float, Float> = when (currency) {
        CurrencyEnum.SOM -> {
            val dx = width * (
                0.018f * sin(ny * FULL_CIRCLE * 1.55f + phase) +
                    0.008f * sin((nx + ny) * FULL_CIRCLE * 2.4f - phase * 2f)
                )
            val dy = height * (
                0.042f * sin(nx * FULL_CIRCLE * 1.18f - phase) +
                    0.016f * cos(ny * FULL_CIRCLE * 2.1f + phase * 2f)
                )
            dx to dy
        }
        CurrencyEnum.ESOM -> {
            val dx = width * (
                0.032f * sin(ny * FULL_CIRCLE * 1.24f - phase) +
                    0.012f * cos(nx * FULL_CIRCLE * 1.8f + phase * 2f)
                )
            val dy = height * (
                0.058f * sin(nx * FULL_CIRCLE * 0.94f + phase) +
                    0.021f * sin((nx - ny) * FULL_CIRCLE * 1.7f - phase * 2f)
                )
            dx to dy
        }
        CurrencyEnum.USDT_TRC20 -> {
            val dx = width * (
                0.016f * sin(ny * FULL_CIRCLE * 2.15f + phase * 2f) +
                    0.011f * cos((nx + ny) * FULL_CIRCLE * 1.6f - phase)
                )
            val dy = height * (
                0.034f * cos(nx * FULL_CIRCLE * 1.72f - phase * 2f) +
                    0.014f * sin(ny * FULL_CIRCLE * 2.8f + phase)
                )
            dx to dy
        }
    }

    private fun buildRevealMask(): Path {
        if (elapsedMs >= INTRO_DURATION_MS) return cardClip
        val progress = smoothStep((elapsedMs / INTRO_DURATION_MS.toFloat()).coerceIn(0f, 1f))
        revealMask.reset()
        buildMosaicReveal(progress)
        return revealMask
    }

    private fun buildMosaicReveal(progress: Float) {
        val columns = 8
        val rows = 5
        val cellWidth = width / columns.toFloat()
        val cellHeight = height / rows.toFloat()
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val index = row * columns + column
                val delayed = smoothStep((progress * 1.35f - (column + row) * 0.028f).coerceIn(0f, 1f))
                if (delayed <= 0f) continue
                val targetX = (column + 0.5f) * cellWidth
                val targetY = (row + 0.5f) * cellHeight
                val angle = index * GOLDEN_ANGLE
                val startRadius = min(width, height) * (0.12f + index % 7 * 0.055f)
                val startX = width / 2f + cos(angle) * startRadius
                val startY = height / 2f + sin(angle) * startRadius
                val x = lerp(startX, targetX, delayed)
                val y = lerp(startY, targetY, delayed)
                val scale = 0.08f + delayed * 0.96f
                createRoundedShape(
                    x,
                    y,
                    cellWidth * 1.04f * scale,
                    cellHeight * 1.04f * scale,
                    7f * resources.displayMetrics.density,
                    (1f - delayed) * (35f + index % 4 * 17f)
                )
                revealMask.addPath(shapePath)
            }
        }
    }

    private fun drawDeepBase(canvas: Canvas, phase: Float) {
        paint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            when (currency) {
                CurrencyEnum.SOM -> intArrayOf(Color.rgb(24, 0, 9), Color.rgb(100, 0, 26))
                CurrencyEnum.ESOM -> intArrayOf(Color.rgb(39, 8, 0), Color.rgb(160, 45, 0))
                CurrencyEnum.USDT_TRC20 -> intArrayOf(Color.rgb(0, 18, 23), Color.rgb(0, 78, 72))
            },
            null,
            Shader.TileMode.CLAMP
        )
        paint.alpha = 255
        canvas.drawRect(cardBounds, paint)
        paint.shader = RadialGradient(
            width * (0.56f + sin(phase).toFloat() * 0.16f),
            height * (0.38f + cos(phase).toFloat() * 0.12f),
            width * 0.72f,
            intArrayOf(Color.argb(58, 255, 255, 255), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(cardBounds, paint)
        paint.shader = null
    }

    private fun drawLivingLight(canvas: Canvas, phase: Float) {
        repeat(3) { index ->
            val harmonic = index + 1f
            val x = width * (0.5f + sin(phase * harmonic + index * 1.7f).toFloat() * (0.18f + index * 0.035f))
            val y = height * (0.5f + cos(phase * harmonic - index * 0.9f).toFloat() * (0.15f + index * 0.025f))
            paint.shader = RadialGradient(
                x,
                y,
                width * (0.28f + index * 0.08f),
                intArrayOf(Color.argb(84 - index * 14, 255, 255, 255), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
            paint.alpha = 255
            canvas.drawRect(cardBounds, paint)
        }
        paint.shader = null
    }

    private fun drawGlassFinish(canvas: Canvas, phase: Float) {
        val center = width * (0.5f + sin(phase).toFloat() * 0.36f)
        paint.shader = LinearGradient(
            center - width * 0.25f,
            height.toFloat(),
            center + width * 0.25f,
            0f,
            intArrayOf(Color.TRANSPARENT, Color.argb(72, 255, 255, 255), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
        paint.alpha = 255
        canvas.drawRect(cardBounds, paint)
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            intArrayOf(Color.argb(28, 255, 255, 255), Color.TRANSPARENT, Color.argb(22, 0, 0, 0)),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(cardBounds, paint)
        paint.shader = null
    }

    private fun loadTexture(): Bitmap = BitmapFactory.decodeResource(
        resources,
        when (currency) {
            CurrencyEnum.SOM -> R.drawable.wallet_motion_som
            CurrencyEnum.ESOM -> R.drawable.wallet_motion_salam
            CurrencyEnum.USDT_TRC20 -> R.drawable.wallet_motion_usdt
        }
    )

    private fun createRoundedShape(
        centerX: Float,
        centerY: Float,
        shapeWidth: Float,
        shapeHeight: Float,
        cornerRadius: Float,
        rotation: Float
    ) {
        shapePath.reset()
        shapePath.addRoundRect(
            -shapeWidth / 2f,
            -shapeHeight / 2f,
            shapeWidth / 2f,
            shapeHeight / 2f,
            cornerRadius,
            cornerRadius,
            Path.Direction.CW
        )
        matrix.reset()
        matrix.setRotate(rotation)
        matrix.postTranslate(centerX, centerY)
        shapePath.transform(matrix)
    }

    private fun restartAnimation(resetTime: Boolean) {
        if (!isAttachedToWindow || visibility != VISIBLE) return
        if (resetTime || animationStartedAt == 0L) animationStartedAt = SystemClock.uptimeMillis()
        elapsedMs = SystemClock.uptimeMillis() - animationStartedAt
        postInvalidateOnAnimation()
    }

    private fun lerp(start: Float, end: Float, amount: Float): Float = start + (end - start) * amount

    private fun smoothStep(value: Float): Float = value * value * (3f - 2f * value)

    companion object {
        private const val MESH_WIDTH = 24
        private const val MESH_HEIGHT = 14
        private const val LOOP_DURATION_MS = 10_000L
        private const val INTRO_DURATION_MS = 1_800L
        private const val FULL_CIRCLE = (PI * 2.0).toFloat()
        private const val GOLDEN_ANGLE = (PI * (3.0 - 2.236067977)).toFloat()
    }
}
