package com.esom.bank.screens.transfer.dialog.success

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.transfer.model.SuccessOperationModel
import com.esom.bank.screens.transfer.model.SuccessTransferUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SuccessTransferUiStateViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(SuccessTransferUiState())
    val uiState = mutableUiState.asStateFlow()
    fun setOperation(value: SuccessOperationModel?) = mutableUiState.update { it.copy(operation = value) }
    fun requestShareAfterLoad() = mutableUiState.update { it.copy(shareAfterReceiptLoaded = true) }
    fun consumeShareAfterLoad() = mutableUiState.update { it.copy(shareAfterReceiptLoaded = false) }
}
