package com.esom.bank.screens.chat

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.chat.model.ChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatUiStateViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(ChatUiState())
    val uiState = mutableUiState.asStateFlow()

    fun setMessageCount(value: Int) = mutableUiState.update { it.copy(lastMessageCount = value) }
    fun setInitialTopPadding(value: Int) = mutableUiState.update { it.copy(initialTopPadding = value) }
}
