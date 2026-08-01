package com.esom.bank.screens.history.dto

import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.gson.annotations.SerializedName

data class TransactionDto(
    @SerializedName("transaction_id")
    val transactionId: Long?,
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("currency")
    val currencyEnum: CurrencyEnum?,
    @SerializedName("type")
    val type: TransactionEnum?,
    @SerializedName("conversion_side")
    val conversionSide: ConversionSide?,
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("successful")
    val successful: Boolean?,
    @SerializedName("created_at")
    val createdAt: Long?,
    @SerializedName(
        value = "recipient_full_name",
        alternate = ["recipient_name", "recipient", "to_user_name"]
    )
    val recipientFullName: String? = null,
    @SerializedName(
        value = "sender_full_name",
        alternate = ["sender_name", "sender", "from_user_name"]
    )
    val senderFullName: String? = null,
    @SerializedName(
        value = "account_details",
        alternate = ["recipient_account", "to_account", "address", "phone_number"]
    )
    val accountDetails: String? = null
)
