package com.esom.bank.screens.chat.dto

import com.google.gson.annotations.SerializedName

data class SendMessageDto (
    @SerializedName("text")
    val text: String
)