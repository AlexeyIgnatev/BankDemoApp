package com.esom.bank.screens.main.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.esom.bank.screens.main.dto.WalletDto
import com.esom.bank.screens.main.enums.CurrencyEnum
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class WalletModel (
    val currency: CurrencyEnum,
    val address: String,
    val balance: Double,
    val buyRate: Double,
    val sellRate: Double
): Parcelable

fun WalletDto.toModel(): WalletModel =
    WalletModel(
        currency = currency,
        address = address,
        balance = balance,
        buyRate = buyRate,
        sellRate = sellRate
    )

fun List<WalletDto>.toModel(): List<WalletModel> {
    return this.map { it.toModel() }
}