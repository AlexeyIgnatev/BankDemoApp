package com.esom.bank.screens.main.dto

import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class WalletDto (
    @SerializedName("currency")
    val currency: CurrencyEnum,
    @SerializedName("address")
    val address: String,
    @SerializedName("balance")
    val balance: BigDecimal,
    @SerializedName("buy_rate")
    val buyRate: BigDecimal,
    @SerializedName("sell_rate")
    val sellRate: BigDecimal
)
