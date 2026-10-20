package com.esom.bank.screens.transfer.model

import com.esom.bank.screens.history.model.ReceiptModel

internal fun ReceiptModel.withConversionSummary(
    operation: SuccessOperationModel?
): ReceiptModel {
    operation ?: return this
    if (operation.conversionSide == null && operation.targetCurrency == null) return this

    val hasIntermediateHop = debitedCurrency.isNotBlank() &&
        debitedCurrency != operation.currency.name
    val sourceFeeCurrency = (operation.feeCurrency ?: operation.currency).name

    return copy(
        amount = operation.amount,
        currency = operation.currency.name,
        fee = if (hasIntermediateHop) operation.fee else fee,
        feeCurrency = if (hasIntermediateHop) sourceFeeCurrency else feeCurrency,
        debitedCurrency = operation.currency.name,
        paidFromAccount = if (hasIntermediateHop) {
            operation.paidFromAccount.ifBlank { paidFromAccount }
        } else {
            paidFromAccount
        },
        accountDetails = if (hasIntermediateHop) {
            operation.recipient.ifBlank { accountDetails }
        } else {
            accountDetails
        },
        creditedAmount = creditedAmount ?: operation.creditedAmount,
        creditedCurrency = creditedCurrency.ifBlank {
            operation.creditedCurrency?.name ?: operation.targetCurrency?.name.orEmpty()
        },
        targetCurrency = operation.targetCurrency?.name ?: targetCurrency,
        totalDebitedAmount = operation.totalDebitedAmount ?: operation.amount
    )
}
