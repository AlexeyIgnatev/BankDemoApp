package com.esom.bank.screens.wallets

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.main.model.WalletModel
import com.esom.bank.screens.wallet.model.WalletsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WalletsUiStateViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(WalletsUiState())
    val uiState = mutableUiState.asStateFlow()

    fun setBalancesVisible(visible: Boolean) = mutableUiState.update { it.copy(balancesVisible = visible) }
    fun setWallets(wallets: List<WalletModel>, balanceInSom: (WalletModel) -> Double) =
        mutableUiState.update { it.copy(totalBalanceInSom = wallets.sumOf(balanceInSom)) }
}
