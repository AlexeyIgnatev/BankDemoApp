package com.esom.bank.screens.transfer.model

import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.history.enums.ConversionSide

data class SuccessOperationModel(
    val amount: Double,
    val currency: CurrencyEnum,
    val operationTitle: String,
    val paidFromAccount: String,
    val recipient: String,
    val receiptNumber: String,
    val transactionId: Long? = null,
    val conversionSide: ConversionSide? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val loadReceiptAutomatically: Boolean = true
)
