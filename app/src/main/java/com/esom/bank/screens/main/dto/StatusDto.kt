package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class StatusDto(
    @SerializedName("status")
    val status: String,
    @SerializedName("transaction_id")
    val transactionId: Long? = null,
    @SerializedName("receipt_number")
    val receiptNumber: String? = null
)
