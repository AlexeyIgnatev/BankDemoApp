package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class PaymentFeeDto(
    @SerializedName("operation")
    val operation: String,
    @SerializedName("percent_fee")
    val percentFee: String,
    @SerializedName("fixed_fee")
    val fixedFee: String
)
