package com.esom.bank.screens.transfer.dialog.success

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.transfer.model.SuccessOperationModel
import com.esom.bank.screens.transfer.model.SuccessTransferUiState
import com.esom.bank.screens.transfer.model.ReceiptAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SuccessTransferUiStateViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(SuccessTransferUiState())
    val uiState = mutableUiState.asStateFlow()
    fun setOperation(value: SuccessOperationModel?) = mutableUiState.update { it.copy(operation = value) }
    fun requestReceiptAction(action: ReceiptAction) =
        mutableUiState.update { it.copy(pendingReceiptAction = action) }

    fun consumeReceiptAction(): ReceiptAction {
        val action = mutableUiState.value.pendingReceiptAction
        mutableUiState.update { it.copy(pendingReceiptAction = ReceiptAction.NONE) }
        return action
    }
}
