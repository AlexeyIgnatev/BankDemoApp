package com.esom.bank.screens.main

import androidx.lifecycle.ViewModel
import com.esom.bank.R
import com.esom.bank.screens.main.model.MainContainerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainContainerUiStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainContainerUiState())
    val uiState: StateFlow<MainContainerUiState> = _uiState.asStateFlow()

    fun setDestination(destinationId: Int) =
        _uiState.update { it.copy(destinationId = destinationId) }

    fun setKeyboardVisible(visible: Boolean) =
        _uiState.update { it.copy(keyboardVisible = visible) }
}
