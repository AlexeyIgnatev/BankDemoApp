package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class FeeDto(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("esom_per_usd")
    val esomPerUsd: Double
)
