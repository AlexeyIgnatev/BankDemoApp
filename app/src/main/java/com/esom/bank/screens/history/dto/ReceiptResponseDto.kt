package com.esom.bank.screens.history.dto

import com.esom.bank.screens.history.enums.ConversionSide
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
    @SerializedName(value = "conversion_side", alternate = ["side"])
    val conversionSide: ConversionSide?,
    @SerializedName(value = "abs_account", alternate = ["absAccount", "account_abs"])
    val absAccount: String?,
    @SerializedName(value = "abs_from_account", alternate = ["absFromAccount", "from_abs_account"])
    val absFromAccount: String?,
    @SerializedName(value = "abs_to_account", alternate = ["absToAccount", "to_abs_account"])
    val absToAccount: String?,
    @SerializedName(value = "receipt_number", alternate = ["receipt_id", "receiptId", "id"])
    val receiptNumber: String?
)
