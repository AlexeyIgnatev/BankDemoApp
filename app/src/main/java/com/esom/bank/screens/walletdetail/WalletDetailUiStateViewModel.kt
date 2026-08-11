package com.esom.bank.screens.walletdetail

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.main.model.WalletModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.wallet.model.WalletDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WalletDetailUiStateViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(WalletDetailUiState())
    val uiState = mutableUiState.asStateFlow()
    fun setCurrency(value: CurrencyEnum) = mutableUiState.update { it.copy(currency = value) }
    fun setWallet(value: WalletModel?) = mutableUiState.update { it.copy(wallet = value) }
    fun setBalanceVisible(value: Boolean) = mutableUiState.update { it.copy(balanceVisible = value) }
}
