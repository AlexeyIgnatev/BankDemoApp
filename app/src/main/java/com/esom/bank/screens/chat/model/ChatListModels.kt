package com.esom.bank.screens.chat.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Message(
    val message: String,
    val time: String,
    val name: String = ""
) : Parcelable

@Keep
sealed class MessageItem {
    data class SenderMessage(val message: Message) : MessageItem()

    data class ReceiverMessage(val message: Message) : MessageItem()

    data class Date(val date: String) : MessageItem()
}
