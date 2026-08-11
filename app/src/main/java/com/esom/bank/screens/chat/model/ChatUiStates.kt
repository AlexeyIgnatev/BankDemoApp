package com.esom.bank.screens.chat.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class ChatUiState(
    val lastMessageCount: Int = 0,
    val initialTopPadding: Int = 0
) : Parcelable

@Keep
@Parcelize
data class SupportMessagesUiState(
    val pendingMessages: List<SupportModel> = emptyList(),
    val cachedMessages: List<SupportModel> = emptyList(),
    val nextPendingMessageId: Int = -1
) : Parcelable
