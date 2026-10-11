package com.esom.bank.screens.swap

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.swap.model.SwapTemplate
import com.esom.bank.screens.swap.model.SwapUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class SwapUiStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SwapUiState())
    val uiState: StateFlow<SwapUiState> = _uiState.asStateFlow()

    fun initialize(from: CurrencyEnum, to: CurrencyEnum) {
        val safeTo = if (from == to) {
            CurrencyEnum.supportedValues.first { it != from }
        } else {
            to
        }
        _uiState.update {
            it.copy(
                fromCurrency = from,
                toCurrency = safeTo,
                fromPanelCurrencies = CurrencyEnum.supportedValues.filterNot { value -> value == from },
                toPanelCurrencies = CurrencyEnum.supportedValues.filterNot { value -> value == safeTo }
            )
        }
    }

    fun toggleFromPanel() = _uiState.update { it.copy(fromPanelShown = !it.fromPanelShown) }
    fun toggleToPanel() = _uiState.update { it.copy(toPanelShown = !it.toPanelShown) }
    fun closePanels() = _uiState.update { it.copy(fromPanelShown = false, toPanelShown = false) }
    fun setUpdatingAmounts(updating: Boolean) = _uiState.update { it.copy(updatingAmounts = updating) }
    fun setPendingTemplate(template: SwapTemplate?) = _uiState.update { it.copy(pendingTemplate = template) }

    fun getOrCreateConversionIdempotencyKey(fingerprint: String): String {
        val current = _uiState.value
        val existingKey = current.conversionIdempotencyKey
        if (current.conversionFingerprint == fingerprint && !existingKey.isNullOrBlank()) {
            return existingKey
        }
        val key = UUID.randomUUID().toString()
        _uiState.update {
            it.copy(
                conversionFingerprint = fingerprint,
                conversionIdempotencyKey = key,
            )
        }
        return key
    }

    fun clearConversionIdempotencyKey() = _uiState.update {
        it.copy(conversionFingerprint = null, conversionIdempotencyKey = null)
    }

    fun consumePendingTemplate(): SwapTemplate? {
        val template = _uiState.value.pendingTemplate
        _uiState.update { it.copy(pendingTemplate = null) }
        return template
    }

    fun startAutomaticRepeat(): Boolean {
        if (_uiState.value.automaticRepeatStarted) return false
        _uiState.update { it.copy(automaticRepeatStarted = true) }
        return true
    }

    fun selectFrom(currency: CurrencyEnum) {
        _uiState.update { state ->
            val previous = state.fromCurrency
            val newTo = if (currency == state.toCurrency) previous else state.toCurrency
            state.copy(
                fromCurrency = currency,
                toCurrency = newTo,
                fromPanelCurrencies = CurrencyEnum.supportedValues.filterNot { it == currency },
                toPanelCurrencies = CurrencyEnum.supportedValues.filterNot { it == newTo },
            )
        }
    }

    fun selectTo(currency: CurrencyEnum) {
        _uiState.update { state ->
            val previous = state.toCurrency
            val newFrom = if (currency == state.fromCurrency) previous else state.fromCurrency
            state.copy(
                toCurrency = currency,
                fromCurrency = newFrom,
                fromPanelCurrencies = CurrencyEnum.supportedValues.filterNot { it == newFrom },
                toPanelCurrencies = CurrencyEnum.supportedValues.filterNot { it == currency },
            )
        }
    }
}
