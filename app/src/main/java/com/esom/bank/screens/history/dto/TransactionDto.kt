package com.esom.bank.screens.history.dto

import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.gson.annotations.SerializedName

data class TransactionDto(
    @SerializedName("currency")
    val currencyEnum: CurrencyEnum?,
    @SerializedName("type")
    val type: TransactionEnum?,
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("successful")
    val successful: Boolean?,
    @SerializedName("created_at")
    val createdAt: Long?
)
