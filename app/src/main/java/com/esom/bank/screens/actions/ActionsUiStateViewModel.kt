package com.esom.bank.screens.actions

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.actions.model.ActionsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ActionsUiStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ActionsUiState())
    val uiState: StateFlow<ActionsUiState> = _uiState.asStateFlow()

    fun selectCurrency(currency: CurrencyEnum) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }

    fun toggleTemplates() = _uiState.update { it.copy(templatesExpanded = !it.templatesExpanded) }
    fun toggleContacts() = _uiState.update { it.copy(contactsExpanded = !it.contactsExpanded) }
}
