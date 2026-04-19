package com.esom.bank.screens.history.dto

import com.google.gson.annotations.SerializedName

data class ReceiptResponseDto(
    @SerializedName("successful")
    val successful: Boolean?,
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("currency")
    val currency: String?,
    @SerializedName("created_at")
    val createdAt: Long?,
    @SerializedName("fee")
    val fee: Double?,
    @SerializedName(value = "account_details", alternate = ["account", "to_account", "requisites"])
    val accountDetails: String?,
    @SerializedName(
        value = "recipient_full_name",
        alternate = ["recipient_name", "recipient", "recipientFullName"]
    )
    val recipientFullName: String?,
    @SerializedName(value = "paid_from_account", alternate = ["from_account", "source_account"])
    val paidFromAccount: String?,
    @SerializedName(value = "receipt_number", alternate = ["receipt_id", "receiptId", "id"])
    val receiptNumber: String?
)
