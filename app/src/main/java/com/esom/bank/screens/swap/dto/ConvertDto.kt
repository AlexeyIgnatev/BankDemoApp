package com.esom.bank.screens.swap.dto

import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.gson.annotations.SerializedName

data class ConvertDto(
    @SerializedName("asset_from")
    val assetFrom: CurrencyEnum,
    @SerializedName("asset_to")
    val assetTo: CurrencyEnum,
    @SerializedName("amount_from")
    val amountFrom: Double
)
