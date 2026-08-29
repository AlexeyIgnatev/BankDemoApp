package com.esom.bank.screens.history.dto

import com.esom.bank.screens.history.enums.ConversionSide
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class ReceiptResponseDto(
    @SerializedName("successful")
    val successful: Boolean?,
    @SerializedName("amount")
    val amount: BigDecimal?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("currency")
    val currency: String?,
    @SerializedName("created_at")
    val createdAt: Long?,
    @SerializedName("fee")
    val fee: BigDecimal?,
    @SerializedName("fee_currency")
    val feeCurrency: String? = null,
    @SerializedName("credited_amount")
    val creditedAmount: BigDecimal? = null,
    @SerializedName("credited_currency")
    val creditedCurrency: String? = null,
    @SerializedName("debited_currency")
    val debitedCurrency: String? = null,
    @SerializedName("account_details")
    val accountDetails: String?,
    @SerializedName("recipient_full_name")
    val recipientFullName: String?,
    @SerializedName("paid_from_account")
    val paidFromAccount: String?,
    @SerializedName("conversion_side")
    val conversionSide: ConversionSide?,
    @SerializedName("abs_account")
    val absAccount: String?,
    @SerializedName("abs_from_account")
    val absFromAccount: String?,
    @SerializedName("abs_to_account")
    val absToAccount: String?,
    @SerializedName("receipt_number")
    val receiptNumber: String?,
    @SerializedName("total_debited_amount")
    val totalDebitedAmount: BigDecimal? = null
)
