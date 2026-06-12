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
            loadReceiptAutomatically = true
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
            receiptNumber = operation.receiptNumber
        )

    private fun TransactionModel.title(context: Context, currency: CurrencyEnum): String {
        val titleRes = when (type) {
            TransactionEnum.CONVERSION -> null
            TransactionEnum.INCOME -> when (currency) {
                CurrencyEnum.SOM -> R.string.income_som
                CurrencyEnum.ESOM -> R.string.income_digital
                CurrencyEnum.ETH -> R.string.income_eth
                CurrencyEnum.BTC -> R.string.income_bitcoin
                CurrencyEnum.USDT_TRC20 -> R.string.income_usdt
            }
            TransactionEnum.EXPENSE -> when (currency) {
                CurrencyEnum.SOM -> R.string.expense_som
                CurrencyEnum.ESOM -> R.string.expense_digital
                CurrencyEnum.ETH -> R.string.expense_eth
                CurrencyEnum.BTC -> R.string.expense_bitcoin
                CurrencyEnum.USDT_TRC20 -> R.string.expense_usdt
            }
            TransactionEnum.INFLOW -> when (currency) {
                CurrencyEnum.SOM -> R.string.inflow_som
                CurrencyEnum.ESOM -> R.string.inflow_digital
                CurrencyEnum.ETH -> R.string.inflow_eth
                CurrencyEnum.BTC -> R.string.inflow_bitcoin
                CurrencyEnum.USDT_TRC20 -> R.string.inflow_usdt
            }
            TransactionEnum.TRANSFER -> when (currency) {
                CurrencyEnum.SOM -> R.string.transfer_som
                CurrencyEnum.ESOM -> R.string.transfer_digital
                CurrencyEnum.ETH -> R.string.transfer_eth
                CurrencyEnum.BTC -> R.string.transfer_bitcoin
                CurrencyEnum.USDT_TRC20 -> R.string.transfer_usdt
            }
            null -> R.string.transfer
        }

        return if (titleRes != null) {
            context.getString(titleRes)
        } else {
            context.getString(R.string.convertation)
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
            TransactionEnum.TRANSFER,
            null -> ""
        }

    private fun UserModel?.accountFor(currency: CurrencyEnum): String {
        if (this == null) return ""
        if (currency == CurrencyEnum.SOM && phone.isNotBlank()) return phone
        return wallets.firstOrNull { it.currency == currency }?.address
            ?.takeIf { it.isNotBlank() }
            ?: phone
    }
}
