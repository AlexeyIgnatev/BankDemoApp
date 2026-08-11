package com.esom.bank.screens.actions

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.actions.model.ActionsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActionsUiStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ActionsUiState())
    val uiState: StateFlow<ActionsUiState> = _uiState.asStateFlow()

    fun selectCurrency(currency: CurrencyEnum) {
        _uiState.value = ActionsUiState(currency)
    }
}
