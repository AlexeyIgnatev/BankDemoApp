package com.esom.bank.screens.history.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.esom.bank.screens.history.HistoryFragment
import com.esom.bank.screens.main.model.WalletModel

@Keep
@Parcelize
data class HistoryUiState(
    val wallets: List<WalletModel> = emptyList(),
    val periodTransactions: List<TransactionModel> = emptyList(),
    val adapterState: HistoryAdapterUiState = HistoryAdapterUiState()
) : Parcelable

@Keep
@Parcelize
data class FinancialAnalysisUiState(
    val mode: String = HistoryFragment.MODE_EXPENSES,
    val transactions: List<TransactionModel> = emptyList(),
    val wallets: List<WalletModel> = emptyList()
) : Parcelable

@Keep
@Parcelize
data class HistoryAdapterUiState(
    val withoutTransfers: Boolean = false,
    val typeFilter: HistoryTypeFilter = HistoryTypeFilter.ALL,
    val minimumAmount: Double? = null,
    val maximumAmount: Double? = null,
    val searchQuery: String = ""
) : Parcelable

@Keep
@Parcelize
data class HistoryGroup(val date: String, val list: List<TransactionModel>) : Parcelable

enum class HistoryTypeFilter { ALL, TRANSFERS, CONVERSIONS, INCOME, EXPENSES }
