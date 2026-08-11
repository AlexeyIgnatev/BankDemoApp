package com.esom.bank.common.utils

import android.content.Context
import androidx.annotation.DrawableRes
import com.esom.bank.R
import com.esom.bank.screens.main.enums.CurrencyEnum

@DrawableRes
fun CurrencyEnum.iconRes(): Int = when (this) {
    CurrencyEnum.SOM -> R.drawable.som_icon
    CurrencyEnum.ESOM -> R.drawable.salam_icon
    CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
}

fun CurrencyEnum.displayName(context: Context): String = when (this) {
    CurrencyEnum.SOM -> context.getString(R.string.som)
    CurrencyEnum.ESOM -> context.getString(R.string.digital)
    CurrencyEnum.USDT_TRC20 -> context.getString(R.string.usdt)
}
