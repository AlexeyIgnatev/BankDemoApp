package com.esom.bank.screens.history.dto

import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class TransactionDto(
    @SerializedName("transaction_id")
    val transactionId: Long?,
    @SerializedName("currency")
    val currencyEnum: CurrencyEnum?,
    @SerializedName("type")
    val type: TransactionEnum?,
    @SerializedName("conversion_side")
    val conversionSide: ConversionSide?,
    @SerializedName("amount")
    val amount: BigDecimal?,
    @SerializedName("successful")
    val successful: Boolean?,
    @SerializedName("created_at")
    val createdAt: Long?,
    @SerializedName("recipient_full_name")
    val recipientFullName: String? = null,
    @SerializedName("sender_full_name")
    val senderFullName: String? = null,
    @SerializedName("account_details")
    val accountDetails: String? = null
)
