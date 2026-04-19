package com.esom.bank.screens.history.dto

import com.esom.bank.screens.history.enums.ConversionSide
import com.google.gson.annotations.SerializedName

data class ReceiptRequestDto(
    @SerializedName("transaction_id")
    val transactionId: Long,
    @SerializedName("conversion_side")
    val conversionSide: ConversionSide? = null
)
