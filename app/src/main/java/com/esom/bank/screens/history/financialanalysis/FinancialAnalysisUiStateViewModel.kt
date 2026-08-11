package com.esom.bank.screens.history.financialanalysis

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.history.model.FinancialAnalysisUiState
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.main.model.WalletModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FinancialAnalysisUiStateViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(FinancialAnalysisUiState())
    val uiState = mutableUiState.asStateFlow()

    fun setMode(value: String) = mutableUiState.update { it.copy(mode = value) }
    fun setTransactions(value: List<TransactionModel>) =
        mutableUiState.update { it.copy(transactions = value) }
    fun setWallets(value: List<WalletModel>) =
        mutableUiState.update { it.copy(wallets = value) }
}
