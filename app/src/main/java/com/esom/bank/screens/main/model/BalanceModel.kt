package com.esom.bank.screens.main.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.esom.bank.screens.main.dto.BalanceDto
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class BalanceModel(
    val somBalance: Double,
    val esomBalance: Double
) : Parcelable

fun BalanceDto.toModel() = BalanceModel(
    somBalance = somBalance,
    esomBalance = esomBalance
)