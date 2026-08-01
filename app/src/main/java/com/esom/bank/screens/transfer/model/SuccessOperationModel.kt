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
    val fee: Double = 0.0,
    val creditedAmount: Double? = null,
    val transactionId: Long? = null,
    val conversionSide: ConversionSide? = null,
    val targetCurrency: CurrencyEnum? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val loadReceiptAutomatically: Boolean = true,
    val amountIsNet: Boolean = false,
    val recipientName: String = "",
    val openedFromHistory: Boolean = false,
    val totalDebitedAmount: Double? = null
)
