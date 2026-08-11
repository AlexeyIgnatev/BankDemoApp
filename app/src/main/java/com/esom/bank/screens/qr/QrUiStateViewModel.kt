package com.esom.bank.screens.qr

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.qr.model.QrUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class QrUiStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(QrUiState())
    val uiState: StateFlow<QrUiState> = _uiState.asStateFlow()

    fun initializePrimaryCurrency(currency: CurrencyEnum) =
        _uiState.update { it.copy(primaryCurrency = currency) }

    fun updateAddresses(phone: String, salam: String, usdt: String) =
        _uiState.update { it.copy(phone = phone, salamAddress = salam, usdtAddress = usdt) }

    fun selectPrimaryCurrency(currency: CurrencyEnum) =
        _uiState.update { it.copy(primaryCurrency = currency) }

    fun toggleTorch(): Boolean {
        val enabled = !_uiState.value.torchEnabled
        _uiState.update { it.copy(torchEnabled = enabled) }
        return enabled
    }

    fun setScanHandled(handled: Boolean) = _uiState.update { it.copy(scanHandled = handled) }
    fun setCameraRequestInFlight(inFlight: Boolean) =
        _uiState.update { it.copy(cameraRequestInFlight = inFlight) }

    fun nextRenderGeneration(): Int {
        val generation = _uiState.value.renderGeneration + 1
        _uiState.update { it.copy(renderGeneration = generation) }
        return generation
    }
}
