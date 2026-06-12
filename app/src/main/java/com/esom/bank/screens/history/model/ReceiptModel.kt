package com.esom.bank.screens.history.model

import androidx.annotation.Keep
import com.esom.bank.screens.history.dto.ReceiptResponseDto
import com.esom.bank.screens.history.enums.ConversionSide

@Keep
data class ReceiptModel(
    val successful: Boolean,
    val amount: Double,
    val type: String,
    val currency: String,
    val createdAt: Long,
    val fee: Double,
    val accountDetails: String,
    val recipientFullName: String,
    val paidFromAccount: String,
    val conversionSide: ConversionSide?,
    val absAccount: String,
    val absFromAccount: String,
    val absToAccount: String,
    val receiptNumber: String,
    val targetCurrency: String = ""
)

fun ReceiptResponseDto.toModel(requestedConversionSide: ConversionSide? = null): ReceiptModel =
    ReceiptModel(
        successful = successful ?: false,
        amount = amount ?: 0.0,
        type = type.orEmpty(),
        currency = currency.orEmpty(),
        createdAt = createdAt ?: System.currentTimeMillis(),
        fee = fee ?: 0.0,
        accountDetails = accountDetails.orEmpty(),
        recipientFullName = recipientFullName.orEmpty(),
        paidFromAccount = paidFromAccount.orEmpty(),
        conversionSide = conversionSide ?: requestedConversionSide,
        absAccount = absAccount.orEmpty(),
        absFromAccount = absFromAccount.orEmpty(),
        absToAccount = absToAccount.orEmpty(),
        receiptNumber = receiptNumber.orEmpty()
    )
