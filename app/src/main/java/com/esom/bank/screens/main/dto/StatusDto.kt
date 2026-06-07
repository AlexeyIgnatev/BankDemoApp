package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class StatusDto(
    @SerializedName("status")
    val status: String,
    @SerializedName(value = "transaction_id", alternate = ["transactionId", "id"])
    val transactionId: Long? = null,
    @SerializedName(value = "receipt_number", alternate = ["receiptNumber", "receipt_id", "receiptId"])
    val receiptNumber: String? = null
)
