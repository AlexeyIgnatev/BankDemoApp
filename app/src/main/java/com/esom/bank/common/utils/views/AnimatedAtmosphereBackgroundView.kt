package com.esom.bank.common.utils.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.esom.bank.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class AnimatedAtmosphereBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {
    private val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sourceBitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.home_atmosphere_background)
    }
    private val meshVertices = FloatArray((MESH_WIDTH + 1) * (MESH_HEIGHT + 1) * 2)
    private var fittedBitmap: Bitmap? = null
    private var animationStartedAt = 0L

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (fittedBitmap == null && width > 0 && height > 0) fitBitmap(width, height)
        if (animationStartedAt == 0L) animationStartedAt = SystemClock.uptimeMillis()
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        fittedBitmap?.recycle()
        fittedBitmap = null
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE && isAttachedToWindow) postInvalidateOnAnimation()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width <= 0 || height <= 0) return
        fitBitmap(width, height)
    }

    private fun fitBitmap(width: Int, height: Int) {
        fittedBitmap?.recycle()
        fittedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { target ->
            val scale = max(width / sourceBitmap.width.toFloat(), height / sourceBitmap.height.toFloat())
            val matrix = Matrix().apply {
                setScale(scale, scale)
                postTranslate(
                    (width - sourceBitmap.width * scale) / 2f,
                    (height - sourceBitmap.height * scale) / 2f
                )
            }
            Canvas(target).drawBitmap(sourceBitmap, matrix, meshPaint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val bitmap = fittedBitmap ?: return
        val phase = phase()
        updateMesh(phase)
        canvas.drawBitmapMesh(
            bitmap,
            MESH_WIDTH,
            MESH_HEIGHT,
            meshVertices,
            0,
            null,
            0,
            meshPaint
        )
        drawMovingGlow(canvas, phase)
        if (isAttachedToWindow && visibility == View.VISIBLE) postInvalidateOnAnimation()
    }

    private fun updateMesh(phase: Float) {
        var offset = 0
        for (row in 0..MESH_HEIGHT) {
            val normalizedY = row / MESH_HEIGHT.toFloat()
            for (column in 0..MESH_WIDTH) {
                val normalizedX = column / MESH_WIDTH.toFloat()
                val edgeLock = sin(PI.toFloat() * normalizedX) * sin(PI.toFloat() * normalizedY)
                val diagonal = (normalizedX + normalizedY) * FULL_CIRCLE
                val horizontalWave = sin(normalizedY * FULL_CIRCLE * 2.15f - phase * 2f).toFloat()
                val diagonalWave = sin(diagonal * 1.2f + phase).toFloat()
                val verticalWave = cos(normalizedX * FULL_CIRCLE * 1.45f + phase * 2f).toFloat()
                val counterWave = sin((normalizedX - normalizedY) * FULL_CIRCLE * 1.7f - phase).toFloat()

                val displacementX = width * (horizontalWave * 0.029f + diagonalWave * 0.014f)
                val displacementY = height * (verticalWave * 0.017f + counterWave * 0.009f)
                meshVertices[offset++] = normalizedX * width + displacementX * edgeLock
                meshVertices[offset++] = normalizedY * height + displacementY * edgeLock
            }
        }
    }

    private fun drawMovingGlow(canvas: Canvas, phase: Float) {
        val glowX = width * (0.5f + sin(phase * 2f).toFloat() * 0.38f)
        val glowY = height * (0.29f + cos(phase).toFloat() * 0.17f)
        glowPaint.shader = RadialGradient(
            glowX,
            glowY,
            width * 0.66f,
            intArrayOf(Color.argb(64, 255, 130, 102), Color.argb(18, 255, 28, 38), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)

        val deepX = width * (0.5f + cos(phase * 2f).toFloat() * 0.42f)
        val deepY = height * (0.65f + sin(phase).toFloat() * 0.14f)
        glowPaint.shader = RadialGradient(
            deepX,
            deepY,
            width * 0.76f,
            intArrayOf(Color.argb(38, 78, 0, 28), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)
        glowPaint.shader = null
    }

    private fun phase(): Float {
        val elapsed = (SystemClock.uptimeMillis() - animationStartedAt) % LOOP_DURATION_MS
        return elapsed / LOOP_DURATION_MS.toFloat() * FULL_CIRCLE
    }

    private companion object {
        const val MESH_WIDTH = 18
        const val MESH_HEIGHT = 32
        const val LOOP_DURATION_MS = 10_000L
        const val FULL_CIRCLE = (PI * 2.0).toFloat()
    }
}
