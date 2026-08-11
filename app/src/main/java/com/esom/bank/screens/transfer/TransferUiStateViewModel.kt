package com.esom.bank.screens.transfer

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.transfer.model.TransferTemplate
import com.esom.bank.screens.transfer.model.TransferUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TransferUiStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    fun initialize(currency: CurrencyEnum) {
        updateCurrency(currency, currency != CurrencyEnum.USDT_TRC20)
    }

    fun updateCurrency(currency: CurrencyEnum, toPhoneNumber: Boolean) {
        _uiState.update {
            it.copy(
                fromCurrency = currency,
                toPhoneNumber = toPhoneNumber,
                currencyPanelOptions = CurrencyEnum.supportedValues.filterNot { value -> value == currency }
            )
        }
    }

    fun setRecipientMode(toPhoneNumber: Boolean) =
        _uiState.update { it.copy(toPhoneNumber = toPhoneNumber) }

    fun toggleRecipientMode() =
        _uiState.update { it.copy(toPhoneNumber = !it.toPhoneNumber) }

    fun toggleCurrencyPanel() =
        _uiState.update { it.copy(currencyPanelShown = !it.currencyPanelShown) }

    fun setPendingTemplate(template: TransferTemplate?) =
        _uiState.update { it.copy(pendingTemplate = template) }

    fun consumePendingTemplate(): TransferTemplate? {
        val template = _uiState.value.pendingTemplate
        _uiState.update { it.copy(pendingTemplate = null) }
        return template
    }
}
