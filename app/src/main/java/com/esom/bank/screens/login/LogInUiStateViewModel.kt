package com.esom.bank.screens.login

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.pinCreate.data.LockType
import com.esom.bank.screens.login.model.LogInUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LogInUiStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LogInUiState())
    val uiState: StateFlow<LogInUiState> = _uiState.asStateFlow()

    fun appendDigit(digit: String, maxLength: Int) {
        _uiState.update { state ->
            if (state.mode != LockType.PIN || state.pinCode.length >= maxLength) state
            else state.copy(pinCode = state.pinCode + digit)
        }
    }

    fun removeLastDigit() {
        _uiState.update { state ->
            if (state.mode != LockType.PIN) state else state.copy(pinCode = state.pinCode.dropLast(1))
        }
    }

    fun selectMode(mode: LockType) {
        _uiState.update { it.copy(mode = mode, pinCode = "") }
    }

    fun clearPin() {
        _uiState.update { it.copy(pinCode = "") }
    }

    fun resetBiometricPrompt() {
        _uiState.update { it.copy(biometricPromptShown = false) }
    }

    fun markBiometricPromptShown() {
        _uiState.update { it.copy(biometricPromptShown = true) }
    }
}
