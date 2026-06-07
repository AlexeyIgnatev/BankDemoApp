package com.esom.bank.screens.transfer.model

import com.esom.bank.screens.main.enums.CurrencyEnum

data class SuccessOperationModel(
    val amount: Double,
    val currency: CurrencyEnum,
    val operationTitle: String,
    val paidFromAccount: String,
    val recipient: String,
    val receiptNumber: String,
    val createdAt: Long = System.currentTimeMillis()
)
