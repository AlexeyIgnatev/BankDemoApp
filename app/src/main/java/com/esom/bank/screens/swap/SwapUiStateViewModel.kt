package com.esom.bank.screens.swap

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.swap.model.SwapTemplate
import com.esom.bank.screens.swap.model.SwapUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SwapUiStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SwapUiState())
    val uiState: StateFlow<SwapUiState> = _uiState.asStateFlow()

    fun initialize(from: CurrencyEnum, to: CurrencyEnum) {
        _uiState.update {
            it.copy(
                fromCurrency = from,
                toCurrency = to,
                fromPanelCurrencies = CurrencyEnum.supportedValues.filterNot { value -> value == from },
                toPanelCurrencies = CurrencyEnum.supportedValues.filterNot { value -> value == to }
            )
        }
    }

    fun toggleFromPanel() = _uiState.update { it.copy(fromPanelShown = !it.fromPanelShown) }
    fun toggleToPanel() = _uiState.update { it.copy(toPanelShown = !it.toPanelShown) }
    fun closePanels() = _uiState.update { it.copy(fromPanelShown = false, toPanelShown = false) }
    fun setUpdatingAmounts(updating: Boolean) = _uiState.update { it.copy(updatingAmounts = updating) }
    fun setPendingTemplate(template: SwapTemplate?) = _uiState.update { it.copy(pendingTemplate = template) }

    fun consumePendingTemplate(): SwapTemplate? {
        val template = _uiState.value.pendingTemplate
        _uiState.update { it.copy(pendingTemplate = null) }
        return template
    }

    fun selectFrom(currency: CurrencyEnum) {
        _uiState.update { state ->
            val previous = state.fromCurrency
            state.copy(
                fromCurrency = currency,
                fromPanelCurrencies = state.fromPanelCurrencies.map {
                    if (it == currency) previous else it
                }
            )
        }
    }

    fun selectTo(currency: CurrencyEnum) {
        _uiState.update { state ->
            val previous = state.toCurrency
            state.copy(
                toCurrency = currency,
                toPanelCurrencies = state.toPanelCurrencies.map {
                    if (it == currency) previous else it
                }
            )
        }
    }
}
