package com.esom.bank.screens.swap.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Keep
@Parcelize
data class SwapTemplate(
    val amount: BigDecimal,
    val fromCurrency: String,
    val toCurrency: String,
    val name: String? = null
) : Parcelable
