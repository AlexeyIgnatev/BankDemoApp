package com.esom.bank.screens.history.model

import android.content.Context
import com.esom.bank.R
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.UserModel
import com.esom.bank.screens.transfer.model.SuccessOperationModel

object TransactionSuccessMapper {

    fun toSuccessOperation(
        context: Context,
        transaction: TransactionModel,
        user: UserModel?
    ): SuccessOperationModel {
        val currency = transaction.currencyEnum ?: CurrencyEnum.SOM
        val receiptNumber = transaction.receiptNumber()
        val account = user.accountFor(currency)
        val paidFromAccount = transaction.paidFromAccount(account)
        val recipient = transaction.recipientAccount(account)
        val recipientName = when (transaction.type) {
            TransactionEnum.INCOME, TransactionEnum.INFLOW -> transaction.senderFullName
            else -> transaction.recipientFullName
        }.orEmpty()

        return SuccessOperationModel(
            amount = transaction.amount ?: 0.0,
            currency = currency,
            operationTitle = transaction.title(context, currency),
            paidFromAccount = paidFromAccount,
            recipient = recipient,
            receiptNumber = receiptNumber,
            fee = 0.0,
            transactionId = transaction.transactionId,
            conversionSide = transaction.conversionSide,
            createdAt = transaction.createdAt ?: System.currentTimeMillis(),
            loadReceiptAutomatically = true,
            recipientName = recipientName,
            openedFromHistory = true,
            amountIsIncoming = transaction.isDisplayedAsIncome()
        )
    }

    fun toReceipt(
        transaction: TransactionModel,
        operation: SuccessOperationModel
    ): ReceiptModel =
        ReceiptModel(
            successful = transaction.successful ?: true,
            amount = operation.amount,
            type = transaction.type?.name.orEmpty(),
            currency = operation.currency.name,
            createdAt = operation.createdAt,
            fee = operation.fee,
            accountDetails = operation.recipient,
            recipientFullName = operation.recipient,
            paidFromAccount = operation.paidFromAccount,
            conversionSide = transaction.conversionSide,
            absAccount = "",
            absFromAccount = "",
            absToAccount = "",
            receiptNumber = operation.receiptNumber,
            totalDebitedAmount = operation.totalDebitedAmount
        )

    private fun TransactionModel.title(context: Context, currency: CurrencyEnum): String {
        if (conversionSide != null) {
            return "Конвертация собственных средств"
        }
        if (!recipientFullName.isNullOrBlank()) {
            return context.getString(R.string.transfer)
        }

        val titleRes = when (type) {
            TransactionEnum.CONVERSION -> null
            TransactionEnum.INCOME -> when (currency) {
                CurrencyEnum.SOM -> R.string.income_som
                CurrencyEnum.ESOM -> R.string.income_digital
                CurrencyEnum.USDT_TRC20 -> R.string.income_usdt
            }
            TransactionEnum.EXPENSE -> when (currency) {
                CurrencyEnum.SOM -> R.string.expense_som
                CurrencyEnum.ESOM -> R.string.expense_digital
                CurrencyEnum.USDT_TRC20 -> R.string.expense_usdt
            }
            TransactionEnum.INFLOW -> when (currency) {
                CurrencyEnum.SOM -> R.string.inflow_som
                CurrencyEnum.ESOM -> R.string.inflow_digital
                CurrencyEnum.USDT_TRC20 -> R.string.inflow_usdt
            }
            TransactionEnum.TRANSFER -> when (currency) {
                CurrencyEnum.SOM -> R.string.transfer_som
                CurrencyEnum.ESOM -> R.string.transfer_digital
                CurrencyEnum.USDT_TRC20 -> R.string.transfer_usdt
            }
            null -> R.string.transfer
        }

        return if (titleRes != null) {
            context.getString(titleRes)
        } else {
            "Конвертация собственных средств"
        }
    }

    private fun TransactionModel.receiptNumber(): String =
        ""

    private fun TransactionModel.paidFromAccount(account: String): String =
        when (type) {
            TransactionEnum.EXPENSE,
            TransactionEnum.TRANSFER,
            TransactionEnum.CONVERSION -> account
            TransactionEnum.INCOME,
            TransactionEnum.INFLOW,
            null -> ""
        }

    private fun TransactionModel.recipientAccount(account: String): String =
        when (type) {
            TransactionEnum.INCOME,
            TransactionEnum.INFLOW,
            TransactionEnum.CONVERSION -> account
            TransactionEnum.EXPENSE,
            TransactionEnum.TRANSFER -> accountDetails.orEmpty()
            null -> accountDetails.orEmpty()
        }

    private fun UserModel?.accountFor(currency: CurrencyEnum): String {
        if (this == null) return ""
        if (currency == CurrencyEnum.SOM && phone.isNotBlank()) return phone
        return wallets.firstOrNull { it.currency == currency }?.address
            ?.takeIf { it.isNotBlank() }
            ?: phone
    }
}
