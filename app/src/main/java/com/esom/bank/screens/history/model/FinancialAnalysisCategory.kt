package com.esom.bank.screens.history.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class FinancialAnalysisCategory(
    val title: String,
    val amount: java.math.BigDecimal,
    val count: Int
) : Parcelable
