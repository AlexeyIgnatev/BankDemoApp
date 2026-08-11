package com.esom.bank.common.utils.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.esom.bank.R
import com.esom.bank.common.model.CurrencyIconPalette
import com.esom.bank.screens.main.enums.CurrencyEnum
import kotlin.math.min

class VolumetricCurrencyIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val bounds = RectF()
    private var currency = CurrencyEnum.SOM

    fun setCurrency(value: CurrencyEnum) {
        currency = value
        contentDescription = when (value) {
            CurrencyEnum.SOM -> context.getString(R.string.som)
            CurrencyEnum.ESOM -> context.getString(R.string.digital)
            CurrencyEnum.USDT_TRC20 -> "USDT"
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val diameter = min(width, height) * 0.88f
        val radius = diameter / 2f
        val centerX = width / 2f
        val centerY = height / 2f - diameter * 0.015f
        val palette = palette()

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            centerX,
            centerY + radius * 0.72f,
            radius * 1.08f,
            intArrayOf(Color.argb(105, 0, 0, 0), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        bounds.set(
            centerX - radius * 0.94f,
            centerY + radius * 0.59f,
            centerX + radius * 0.94f,
            centerY + radius * 1.02f
        )
        canvas.drawOval(bounds, paint)

        canvas.save()
        canvas.scale(1f, 0.9f, centerX, centerY)
        paint.shader = RadialGradient(
            centerX - radius * 0.31f,
            centerY - radius * 0.4f,
            radius * 1.42f,
            intArrayOf(palette.highlight, palette.base, palette.shadow),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, radius, paint)

        paint.shader = null
        val iconInset = radius * 0.24f
        val icon = AppCompatResources.getDrawable(context, iconResource())?.mutate()
        icon?.setBounds(
            (centerX - radius + iconInset).toInt(),
            (centerY - radius + iconInset).toInt(),
            (centerX + radius - iconInset).toInt(),
            (centerY + radius - iconInset).toInt()
        )
        icon?.draw(canvas)

        paint.shader = LinearGradient(
            centerX,
            centerY - radius,
            centerX,
            centerY + radius,
            intArrayOf(Color.argb(92, 255, 255, 255), Color.TRANSPARENT, Color.argb(78, 0, 0, 0)),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, radius * 0.97f, paint)

        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = radius * 0.075f
        paint.color = Color.argb(125, 255, 255, 255)
        canvas.drawCircle(centerX, centerY, radius * 0.94f, paint)
        paint.strokeWidth = radius * 0.035f
        paint.color = Color.argb(115, 0, 0, 0)
        canvas.drawArc(
            centerX - radius * 0.9f,
            centerY - radius * 0.9f,
            centerX + radius * 0.9f,
            centerY + radius * 0.9f,
            18f,
            144f,
            false,
            paint
        )

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            centerX - radius * 0.38f,
            centerY - radius * 0.43f,
            radius * 0.34f,
            intArrayOf(Color.argb(190, 255, 255, 255), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX - radius * 0.31f, centerY - radius * 0.36f, radius * 0.3f, paint)
        canvas.restore()
        paint.shader = null
    }

    private fun iconResource(): Int = when (currency) {
        CurrencyEnum.SOM -> R.drawable.som_icon
        CurrencyEnum.ESOM -> R.drawable.salam_icon
        CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
    }

    private fun palette(): CurrencyIconPalette = when (currency) {
        CurrencyEnum.SOM -> CurrencyIconPalette(Color.rgb(255, 104, 107), Color.rgb(226, 35, 36), Color.rgb(112, 0, 24))
        CurrencyEnum.ESOM -> CurrencyIconPalette(Color.rgb(255, 143, 91), Color.rgb(236, 58, 38), Color.rgb(124, 18, 8))
        CurrencyEnum.USDT_TRC20 -> CurrencyIconPalette(Color.rgb(91, 240, 208), Color.rgb(24, 172, 142), Color.rgb(0, 73, 70))
    }
}
