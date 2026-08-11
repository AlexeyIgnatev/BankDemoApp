package com.esom.bank.screens.transfer.recipient

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.actions.model.PaymentContact
import com.esom.bank.screens.transfer.model.TransferRecipientUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TransferRecipientUiStateViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(TransferRecipientUiState())
    val uiState = mutableUiState.asStateFlow()
    fun setContacts(value: List<PaymentContact>) = mutableUiState.update { it.copy(contacts = value) }
}
