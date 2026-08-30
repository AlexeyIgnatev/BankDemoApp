package com.esom.bank.common.utils.views

import android.graphics.Color
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.core.view.WindowInsetsCompat
import com.esom.bank.R
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.screens.main.enums.CurrencyEnum
import java.math.BigDecimal

fun View.applyPaymentWindowInsets() {
    doOnApplyWindowInsets { target, insets, initialPadding ->
        val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        target.updatePadding(
            top = initialPadding.top + systemBars.top,
            bottom = initialPadding.bottom + if (imeBottom == 0) systemBars.bottom else imeBottom
        )
        insets
    }
}

fun ViewGroup.addRecentTemplateView(
    label: String,
    onClick: () -> Unit
) {
    val density = resources.displayMetrics.density
    fun dp(value: Int) = (value * density).toInt()

    addView(TextView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = dp(8)
        }
        minWidth = dp(132)
        maxWidth = dp(190)
        setPadding(dp(12), dp(9), dp(12), dp(9))
        setBackgroundResource(R.drawable.recent_template_background)
        text = label
        textSize = 12f
        setTextColor(Color.parseColor("#1D1D1B"))
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setOnClickListener { onClick() }
    })
}

fun formatTemplateAmount(amount: BigDecimal): String = amount.formatBalanceNew()

fun paymentName(currency: CurrencyEnum): String = when (currency) {
    CurrencyEnum.SOM -> "Сом"
    CurrencyEnum.ESOM -> "Салам"
    CurrencyEnum.USDT_TRC20 -> "USDT"
}

fun View.setQuickAmountClick(
    input: EditText,
    amount: BigDecimal,
    appendToCurrent: Boolean = false,
    onChanged: (String) -> Unit
) {
    setOnClickListener {
        val current = if (appendToCurrent) {
            input.text?.toString()
                ?.trim()
                ?.replace(',', '.')
                ?.toBigDecimalOrNull()
                ?: BigDecimal.ZERO
        } else {
            BigDecimal.ZERO
        }
        val value = if (appendToCurrent) current.add(amount) else amount
        val text = value.stripTrailingZeros().toPlainString()
        input.setText(text)
        input.setSelection(text.length)
        onChanged(text)
    }
}
