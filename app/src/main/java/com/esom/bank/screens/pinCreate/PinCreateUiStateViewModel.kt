package com.esom.bank.screens.pinCreate

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.pinCreate.data.LockType
import com.esom.bank.screens.pinCreate.model.PinCreateUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PinCreateUiStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PinCreateUiState())
    val uiState: StateFlow<PinCreateUiState> = _uiState.asStateFlow()

    fun selectMode(mode: LockType) {
        _uiState.value = PinCreateUiState(mode = mode)
    }

    fun appendDigit(digit: String, maxLength: Int) {
        _uiState.update { state ->
            if (state.mode != LockType.PIN || state.currentPin.length >= maxLength) state
            else state.copy(currentPin = state.currentPin + digit)
        }
    }

    fun removeLastDigit() {
        _uiState.update { it.copy(currentPin = it.currentPin.dropLast(1)) }
    }

    fun startPinConfirmation() {
        _uiState.update { it.copy(firstPin = it.currentPin, currentPin = "") }
    }

    fun startPatternConfirmation(pattern: List<Int>) {
        _uiState.update { it.copy(firstPattern = pattern) }
    }

    fun clearCurrentPin() {
        _uiState.update { it.copy(currentPin = "") }
    }

    fun resetCurrentMode() {
        _uiState.update {
            if (it.mode == LockType.PIN) it.copy(currentPin = "", firstPin = null)
            else it.copy(firstPattern = null)
        }
    }
}
