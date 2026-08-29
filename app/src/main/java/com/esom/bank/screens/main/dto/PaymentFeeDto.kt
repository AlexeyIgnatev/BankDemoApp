package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class PaymentFeeDto(
    @SerializedName("operation")
    val operation: String? = null,
    @SerializedName("percent_fee")
    val percentFee: String? = null,
    @SerializedName("fixed_fee")
    val fixedFee: String? = null
)
