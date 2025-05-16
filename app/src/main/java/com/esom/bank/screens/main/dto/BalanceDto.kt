package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class BalanceDto(
    @SerializedName("SOM")
    val somBalance: Double,
    @SerializedName("ESOM")
    val esomBalance: Double
)
