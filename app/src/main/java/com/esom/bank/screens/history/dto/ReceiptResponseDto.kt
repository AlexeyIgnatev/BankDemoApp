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
    @SerializedName("account_details")
    val accountDetails: String?,
    @SerializedName("recipient_full_name")
    val recipientFullName: String?,
    @SerializedName("paid_from_account")
    val paidFromAccount: String?,
    @SerializedName("receipt_number")
    val receiptNumber: String?
)
