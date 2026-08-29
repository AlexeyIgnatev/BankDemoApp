package com.esom.bank.screens.main.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.esom.bank.screens.main.dto.WalletDto
import com.esom.bank.screens.main.enums.CurrencyEnum
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Keep
@Parcelize
data class WalletModel (
    val currency: CurrencyEnum,
    val address: String,
    val balance: BigDecimal,
    val buyRate: BigDecimal,
    val sellRate: BigDecimal
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
