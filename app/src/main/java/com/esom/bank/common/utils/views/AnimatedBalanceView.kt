package com.esom.bank.common.utils.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class AnimatedBalanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private var targetMasked = false
    private var transitionStartedAt = 0L
    private var transitioning = false
    private var initialized = false

    fun setBalance(value: String, visible: Boolean) {
        text = value
        val masked = !visible
        if (!initialized) {
            initialized = true
            targetMasked = masked
            invalidate()
            return
        }
        if (targetMasked == masked) return
        targetMasked = masked
        transitionStartedAt = SystemClock.uptimeMillis()
        transitioning = true
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        val progress = if (transitioning) {
            ((now - transitionStartedAt) / TRANSITION_DURATION_MS.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }
        val eased = progress * progress * (3f - 2f * progress)
        val particleAmount = when {
            !transitioning && targetMasked -> 1f
            !transitioning -> 0f
            targetMasked -> eased
            else -> 1f - eased
        }

        val originalAlpha = paint.alpha
        // The value changes immediately; only the particle cloud animates.
        paint.alpha = if (targetMasked) 0 else 255
        if (paint.alpha > 0) super.onDraw(canvas)
        paint.alpha = originalAlpha

        if (particleAmount > 0f) drawParticles(canvas, now, particleAmount)
        if (transitioning && progress >= 1f) transitioning = false
        if (transitioning || targetMasked) postInvalidateOnAnimation()
    }

    private fun drawParticles(canvas: Canvas, now: Long, amount: Float) {
        val contentLeft = paddingLeft.toFloat()
        val contentWidth = (width - paddingLeft - paddingRight).coerceAtLeast(1).toFloat()
        val centerX = contentLeft + contentWidth / 2f
        val centerY = height / 2f
        val spread = (amount * 1.45f).coerceAtMost(1f)
        val seconds = now / 1000f
        particlePaint.color = currentTextColor

        repeat(PARTICLE_COUNT) { index ->
            val column = index % PARTICLE_COLUMNS
            val row = index / PARTICLE_COLUMNS
            val phase = index * GOLDEN_ANGLE
            val targetX = contentLeft + contentWidth *
                ((column + 0.45f + sin(phase).toFloat() * 0.18f) / PARTICLE_COLUMNS)
            val targetY = centerY + (row - 1f) * 3.6f * density +
                cos(phase).toFloat() * 1.4f * density
            val driftX = sin(seconds * (1.15f + index % 4 * 0.16f) + phase).toFloat() * 2.2f * density
            val driftY = cos(seconds * (0.92f + index % 5 * 0.11f) - phase).toFloat() * 1.7f * density
            val x = centerX + (targetX + driftX - centerX) * spread
            val y = centerY + (targetY + driftY - centerY) * spread
            val pulse = 0.62f + 0.38f * sin(seconds * 2.1f + phase).toFloat()
            particlePaint.alpha = (amount * (110f + pulse * 125f)).toInt().coerceIn(0, 255)
            val radius = (0.75f + index % 3 * 0.3f) * density
            canvas.drawCircle(x, y, radius, particlePaint)
        }
    }

    private companion object {
        const val PARTICLE_COLUMNS = 12
        const val PARTICLE_COUNT = 36
        const val TRANSITION_DURATION_MS = 520L
        const val GOLDEN_ANGLE = (PI * (3.0 - 2.236067977)).toFloat()
    }
}
