package com.esom.bank.screens.history.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.esom.bank.screens.history.dto.ReceiptResponseDto
import com.esom.bank.screens.history.enums.ConversionSide

@Keep
@Parcelize
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
    val creditedAmount: Double? = null,
    val absAccount: String,
    val absFromAccount: String,
    val absToAccount: String,
    val receiptNumber: String,
    val targetCurrency: String = "",
    val totalDebitedAmount: Double? = null,
    val senderFullName: String = ""
) : Parcelable

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
        receiptNumber = receiptNumber.orEmpty(),
        totalDebitedAmount = totalDebitedAmount
    )
