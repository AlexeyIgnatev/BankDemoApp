package com.esom.bank.screens.main.dto

import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.gson.annotations.SerializedName

data class WalletDto (
    @SerializedName("currency")
    val currency: CurrencyEnum,
    @SerializedName("address")
    val address: String,
    @SerializedName("balance")
    val balance: Double,
    @SerializedName("buy_rate")
    val buyRate: Double,
    @SerializedName("sell_rate")
    val sellRate: Double
)