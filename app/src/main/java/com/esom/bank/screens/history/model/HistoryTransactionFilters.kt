package com.esom.bank.screens.history.model

import com.esom.bank.screens.history.enums.TransactionEnum
import java.util.Locale

fun List<TransactionModel>.filterHistoryTransactions(
    state: HistoryAdapterUiState
): List<TransactionModel> {
    val searchQuery = state.searchQuery.trim().lowercase(Locale.getDefault())

    return asSequence()
        .filter { it.matchesHistoryType(state.typeFilter) }
        .filter { transaction ->
            val amount = transaction.amount ?: 0.0
            (state.minimumAmount == null || amount >= state.minimumAmount) &&
                (state.maximumAmount == null || amount <= state.maximumAmount)
        }
        .filter { transaction ->
            searchQuery.isBlank() || transaction.searchableText().contains(searchQuery)
        }
        .toList()
}

fun TransactionModel.matchesHistoryType(filter: HistoryTypeFilter): Boolean = when (filter) {
    HistoryTypeFilter.ALL -> true
    HistoryTypeFilter.TRANSFERS -> isHistoryTransferTransaction()
    HistoryTypeFilter.CONVERSIONS -> type == TransactionEnum.CONVERSION
    HistoryTypeFilter.INCOME -> isHistoryIncomeTransaction()
    HistoryTypeFilter.EXPENSES -> isHistoryExpenseTransaction()
}

fun TransactionModel.isHistoryTransferTransaction(): Boolean =
    isUserTransfer()

fun TransactionModel.isHistoryConversionTransaction(): Boolean =
    type == TransactionEnum.CONVERSION

fun TransactionModel.isHistoryIncomeTransaction(): Boolean =
    type == TransactionEnum.INCOME ||
        type == TransactionEnum.INFLOW ||
        (!senderFullName.isNullOrBlank() && recipientFullName.isNullOrBlank())

fun TransactionModel.isHistoryExpenseTransaction(): Boolean =
    type == TransactionEnum.EXPENSE ||
        (type != TransactionEnum.CONVERSION &&
            !isHistoryIncomeTransaction() &&
            isUserTransfer())

fun TransactionModel.searchableText(): String = buildString {
    append(type?.name.orEmpty())
    append(' ')
    append(currencyEnum?.name.orEmpty())
    append(' ')
    append(amount?.toString().orEmpty())
    append(' ')
    append(recipientFullName.orEmpty())
    append(' ')
    append(senderFullName.orEmpty())
}.lowercase(Locale.getDefault())
