package com.esom.bank.screens.swap.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class SwapTemplate(
    val amount: Double,
    val fromCurrency: String,
    val toCurrency: String,
    val name: String? = null
) : Parcelable
