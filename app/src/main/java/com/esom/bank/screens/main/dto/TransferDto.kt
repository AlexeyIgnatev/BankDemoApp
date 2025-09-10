package com.esom.bank.screens.main.dto

import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.gson.annotations.SerializedName

data class TransferDto(
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("currency")
    val currencyEnum: CurrencyEnum
)
