package com.esom.bank.common.utils.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.appcompat.widget.AppCompatTextView
import com.esom.bank.R
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
    private var balanceValue = ""

    fun setBalance(value: String, visible: Boolean) {
        balanceValue = value
        val masked = !visible
        // Do not keep the real amount in TextView.text while hidden. Drawing-only
        // masking leaves the value available to TalkBack and accessibility dumps.
        super.setText(if (masked) MASKED_TEXT else balanceValue)
        contentDescription = if (masked) context.getString(R.string.balance_hidden_accessibility) else null
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

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (targetMasked) {
            val hiddenLabel = context.getString(R.string.balance_hidden_accessibility)
            info.text = hiddenLabel
            info.contentDescription = hiddenLabel
        }
    }

    override fun onPopulateAccessibilityEvent(event: AccessibilityEvent) {
        super.onPopulateAccessibilityEvent(event)
        if (targetMasked) {
            event.text.clear()
            event.text.add(context.getString(R.string.balance_hidden_accessibility))
        }
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        val animationFrame = transitioning
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

        // Freeze the masked particle cloud after the transition. A perpetual
        // postInvalidateOnAnimation loop keeps the main looper permanently busy,
        // wasting CPU and preventing UI automation from ever reaching idle.
        val particleTime = if (animationFrame) now else transitionStartedAt + TRANSITION_DURATION_MS
        if (particleAmount > 0f) drawParticles(canvas, particleTime, particleAmount)
        if (transitioning && progress >= 1f) transitioning = false
        if (transitioning) postInvalidateOnAnimation()
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
        const val MASKED_TEXT = "••••••"
        const val PARTICLE_COLUMNS = 12
        const val PARTICLE_COUNT = 36
        const val TRANSITION_DURATION_MS = 520L
        const val GOLDEN_ANGLE = (PI * (3.0 - 2.236067977)).toFloat()
    }
}
