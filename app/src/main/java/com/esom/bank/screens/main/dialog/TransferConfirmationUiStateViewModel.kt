package com.esom.bank.screens.main.dialog

import android.os.Bundle
import androidx.lifecycle.ViewModel
import com.esom.bank.screens.main.model.TransferConfirmationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransferConfirmationUiStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TransferConfirmationUiState())
    val uiState: StateFlow<TransferConfirmationUiState> = _uiState.asStateFlow()

    fun setData(data: Bundle) {
        _uiState.value = TransferConfirmationUiState(Bundle(data))
    }
}
