package com.esom.bank.screens.wallet

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.main.model.WalletModel
import com.esom.bank.screens.wallet.model.WalletUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WalletUiStateViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(WalletUiState())
    val uiState = mutableUiState.asStateFlow()

    fun restoreHistoryExpanded(value: Boolean) = mutableUiState.update { it.copy(historyExpanded = value) }
    fun toggleHistory() = mutableUiState.update { it.copy(historyExpanded = !it.historyExpanded) }
    fun setBalancesVisible(value: Boolean) = mutableUiState.update { it.copy(balancesVisible = value) }
    fun setWallets(value: List<WalletModel>) = mutableUiState.update { it.copy(wallets = value) }
}
