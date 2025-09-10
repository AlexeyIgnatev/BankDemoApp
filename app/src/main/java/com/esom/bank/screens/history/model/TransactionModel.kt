package com.esom.bank.screens.history.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.esom.bank.screens.history.dto.TransactionDto
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.main.enums.CurrencyEnum
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class TransactionModel(
    val currencyEnum: CurrencyEnum,
    val type: TransactionEnum,
    val amount: Double,
    val successful: Boolean,
    val createdAt: Long
): Parcelable

fun TransactionDto.toModel(): TransactionModel =
    TransactionModel(
        currencyEnum = currencyEnum,
        type = type,
        amount = amount,
        successful = successful,
        createdAt = createdAt
    )

fun List<TransactionDto>.toModel(): List<TransactionModel> {
    return this.map { it.toModel() }
}
