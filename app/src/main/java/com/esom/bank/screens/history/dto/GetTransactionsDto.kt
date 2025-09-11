package com.esom.bank.screens.history.dto

import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.gson.annotations.SerializedName

data class GetTransactionsDto(
    @SerializedName("currency")
    val currency: List<CurrencyEnum>? = null,
    @SerializedName("from_time")
    val fromTime: Long,
    @SerializedName("to_time")
    val toTime: Long,
    @SerializedName("take")
    val take: Int,
    @SerializedName("skip")
    val skip: Int
)
